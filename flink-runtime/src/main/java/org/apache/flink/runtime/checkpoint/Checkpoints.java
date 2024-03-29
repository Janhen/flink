/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.checkpoint;

import org.apache.flink.api.common.JobID;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.OperatorIDPair;
import org.apache.flink.runtime.checkpoint.metadata.CheckpointMetadata;
import org.apache.flink.runtime.checkpoint.metadata.MetadataSerializer;
import org.apache.flink.runtime.checkpoint.metadata.MetadataSerializers;
import org.apache.flink.runtime.checkpoint.metadata.MetadataV3Serializer;
import org.apache.flink.runtime.executiongraph.ExecutionJobVertex;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.state.CheckpointStorage;
import org.apache.flink.runtime.state.CheckpointStorageLoader;
import org.apache.flink.runtime.state.CompletedCheckpointStorageLocation;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.runtime.state.StateBackendLoader;
import org.apache.flink.runtime.state.StreamStateHandle;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.runtime.state.storage.JobManagerCheckpointStorage;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.FlinkException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * 一个实用类，它具有写加载处理检查点和保存点元数据的方法。
 *
 * <p>存储的检查点元数据文件的格式如下:
 *
 * [MagicNumber (int) | Format Version (int) | Checkpoint Metadata (variable)]
 *
 * <p>实际的保存点序列化是通过{@link MetadataSerializer}特定于版本的。
 *
 * A utility class with the methods to write/load/dispose the checkpoint and savepoint metadata.
 *
 * <p>Stored checkpoint metadata files have the following format:
 *
 * <pre>[MagicNumber (int) | Format Version (int) | Checkpoint Metadata (variable)]</pre>
 *
 * <p>The actual savepoint serialization is version-specific via the {@link MetadataSerializer}.
 */
public class Checkpoints {

    private static final Logger LOG = LoggerFactory.getLogger(Checkpoints.class);

    /** Magic number at the beginning of every checkpoint metadata file, for sanity checks. */
    // 在每个检查点元数据文件的开始处设置一个神奇的数字，用于进行完整性检查
    public static final int HEADER_MAGIC_NUMBER = 0x4960672d;

    // ------------------------------------------------------------------------
    //  Writing out checkpoint metadata
    // ------------------------------------------------------------------------

    // J: 写入检查点元数据
    public static void storeCheckpointMetadata(
            CheckpointMetadata checkpointMetadata, OutputStream out) throws IOException {

        DataOutputStream dos = new DataOutputStream(out);
        storeCheckpointMetadata(checkpointMetadata, dos);
    }

    // J: 存储检查点元数据
    public static void storeCheckpointMetadata(
            CheckpointMetadata checkpointMetadata, DataOutputStream out) throws IOException {

        // write generic header
        out.writeInt(HEADER_MAGIC_NUMBER);

        out.writeInt(MetadataV3Serializer.VERSION);
        MetadataV3Serializer.serialize(checkpointMetadata, out);
    }

    // ------------------------------------------------------------------------
    //  Reading and validating checkpoint metadata
    // ------------------------------------------------------------------------
    // 读取和验证检查点元数据

    public static CheckpointMetadata loadCheckpointMetadata(
            DataInputStream in, ClassLoader classLoader, String externalPointer)
            throws IOException {
        checkNotNull(in, "input stream");
        checkNotNull(classLoader, "classLoader");

        // J: 读取到检查点的号?
        // /<path>/<job-id>/chk-254770
        final int magicNumber = in.readInt();

        if (magicNumber == HEADER_MAGIC_NUMBER) {
            final int version = in.readInt();
            final MetadataSerializer serializer = MetadataSerializers.getSerializer(version);
            return serializer.deserialize(in, classLoader, externalPointer);
        } else {
            throw new IOException(
                    "Unexpected magic number. This can have multiple reasons: "
                            + "(1) You are trying to load a Flink 1.0 savepoint, which is not supported by this "
                            + "version of Flink. (2) The file you were pointing to is not a savepoint at all. "
                            + "(3) The savepoint file has been corrupted.");
        }
    }

    public static CompletedCheckpoint loadAndValidateCheckpoint(
            JobID jobId,
            Map<JobVertexID, ExecutionJobVertex> tasks,
            CompletedCheckpointStorageLocation location,
            ClassLoader classLoader,
            boolean allowNonRestoredState)
            throws IOException {

        checkNotNull(jobId, "jobId");
        checkNotNull(tasks, "tasks");
        checkNotNull(location, "location");
        checkNotNull(classLoader, "classLoader");

        final StreamStateHandle metadataHandle = location.getMetadataHandle();
        final String checkpointPointer = location.getExternalPointer();

        // (1) load the savepoint
        final CheckpointMetadata checkpointMetadata;
        try (InputStream in = metadataHandle.openInputStream()) {
            DataInputStream dis = new DataInputStream(in);
            checkpointMetadata = loadCheckpointMetadata(dis, classLoader, checkpointPointer);
        }

        // generate mapping from operator to task
        Map<OperatorID, ExecutionJobVertex> operatorToJobVertexMapping = new HashMap<>();
        for (ExecutionJobVertex task : tasks.values()) {
            for (OperatorIDPair operatorIDPair : task.getOperatorIDs()) {
                operatorToJobVertexMapping.put(operatorIDPair.getGeneratedOperatorID(), task);
                operatorIDPair
                        .getUserDefinedOperatorID()
                        .ifPresent(id -> operatorToJobVertexMapping.put(id, task));
            }
        }

        // (2) validate it (parallelism, etc)
        HashMap<OperatorID, OperatorState> operatorStates =
                new HashMap<>(checkpointMetadata.getOperatorStates().size());
        for (OperatorState operatorState : checkpointMetadata.getOperatorStates()) {

            ExecutionJobVertex executionJobVertex =
                    operatorToJobVertexMapping.get(operatorState.getOperatorID());

            if (executionJobVertex != null) {

                if (executionJobVertex.getMaxParallelism() == operatorState.getMaxParallelism()
                        || executionJobVertex.canRescaleMaxParallelism(
                                operatorState.getMaxParallelism())) {
                    operatorStates.put(operatorState.getOperatorID(), operatorState);
                } else {
                    String msg =
                            String.format(
                                    "Failed to rollback to checkpoint/savepoint %s. "
                                            + "Max parallelism mismatch between checkpoint/savepoint state and new program. "
                                            + "Cannot map operator %s with max parallelism %d to new program with "
                                            + "max parallelism %d. This indicates that the program has been changed "
                                            + "in a non-compatible way after the checkpoint/savepoint.",
                                    checkpointMetadata,
                                    operatorState.getOperatorID(),
                                    operatorState.getMaxParallelism(),
                                    executionJobVertex.getMaxParallelism());

                    throw new IllegalStateException(msg);
                }
            } else if (allowNonRestoredState) {
                LOG.info(
                        "Skipping savepoint state for operator {}.", operatorState.getOperatorID());
            } else {
                if (operatorState.getCoordinatorState() != null) {
                    throwNonRestoredStateException(
                            checkpointPointer, operatorState.getOperatorID());
                }

                for (OperatorSubtaskState operatorSubtaskState : operatorState.getStates()) {
                    if (operatorSubtaskState.hasState()) {
                        throwNonRestoredStateException(
                                checkpointPointer, operatorState.getOperatorID());
                    }
                }

                LOG.info(
                        "Skipping empty savepoint state for operator {}.",
                        operatorState.getOperatorID());
            }
        }

        // (3) convert to checkpoint so the system can fall back to it
        CheckpointProperties props = CheckpointProperties.forSavepoint(false);

        return new CompletedCheckpoint(
                jobId,
                checkpointMetadata.getCheckpointId(),
                0L,
                0L,
                operatorStates,
                checkpointMetadata.getMasterStates(),
                props,
                location);
    }

    private static void throwNonRestoredStateException(
            String checkpointPointer, OperatorID operatorId) {
        String msg =
                String.format(
                        "Failed to rollback to checkpoint/savepoint %s. "
                                + "Cannot map checkpoint/savepoint state for operator %s to the new program, "
                                + "because the operator is not available in the new program. If "
                                + "you want to allow to skip this, you can set the --allowNonRestoredState "
                                + "option on the CLI.",
                        checkpointPointer, operatorId);

        throw new IllegalStateException(msg);
    }

    // ------------------------------------------------------------------------
    //  Savepoint Disposal Hooks
    // ------------------------------------------------------------------------
    // 保存点处理钩子

    public static void disposeSavepoint(
            String pointer, CheckpointStorage checkpointStorage, ClassLoader classLoader)
            throws IOException, FlinkException {

        checkNotNull(pointer, "location");
        checkNotNull(checkpointStorage, "stateBackend");
        checkNotNull(classLoader, "classLoader");

        // J:
        final CompletedCheckpointStorageLocation checkpointLocation =
                checkpointStorage.resolveCheckpoint(pointer);

        final StreamStateHandle metadataHandle = checkpointLocation.getMetadataHandle();

        // 加载savepoint对象(元数据)以拥有需要释放所有状态的所有状态句柄
        // load the savepoint object (the metadata) to have all the state handles that we need
        // to dispose of all state
        final CheckpointMetadata metadata;
        try (InputStream in = metadataHandle.openInputStream();
                DataInputStream dis = new DataInputStream(in)) {

            metadata = loadCheckpointMetadata(dis, classLoader, pointer);
        }

        Exception exception = null;

        // 首先，销毁保存点元数据，这样，即使下面的销毁失败，保存点也不再可寻址
        // first dispose the savepoint metadata, so that the savepoint is not
        // addressable any more even if the following disposal fails
        try {
            metadataHandle.discardState();
        } catch (Exception e) {
            exception = e;
        }

        // 现在处理保存点数据
        // now dispose the savepoint data
        try {
            metadata.dispose();
        } catch (Exception e) {
            exception = ExceptionUtils.firstOrSuppressed(e, exception);
        }

        // now dispose the location (directory, table, whatever)
        // 现在处理位置(目录、表等)
        try {
            checkpointLocation.disposeStorageLocation();
        } catch (Exception e) {
            exception = ExceptionUtils.firstOrSuppressed(e, exception);
        }

        // forward exceptions caught in the process
        if (exception != null) {
            ExceptionUtils.rethrowIOException(exception);
        }
    }

    public static void disposeSavepoint(
            String pointer,
            Configuration configuration,
            ClassLoader classLoader,
            @Nullable Logger logger)
            throws IOException, FlinkException {

        checkNotNull(pointer, "location");
        checkNotNull(configuration, "configuration");
        checkNotNull(classLoader, "classLoader");

        CheckpointStorage storage = loadCheckpointStorage(configuration, classLoader, logger);

        disposeSavepoint(pointer, storage, classLoader);
    }

    @Nonnull
    public static StateBackend loadStateBackend(
            Configuration configuration, ClassLoader classLoader, @Nullable Logger logger) {
        if (logger != null) {
            logger.info("Attempting to load configured state backend for savepoint disposal");
        }

        StateBackend backend = null;
        try {
            backend =
                    StateBackendLoader.loadStateBackendFromConfig(configuration, classLoader, null);

            if (backend == null && logger != null) {
                logger.debug(
                        "No state backend configured, attempting to dispose savepoint "
                                + "with configured checkpoint storage");
            }
        } catch (Throwable t) {
            // catches exceptions and errors (like linking errors)
            if (logger != null) {
                logger.info("Could not load configured state backend.");
                logger.debug("Detailed exception:", t);
            }
        }

        if (backend == null) {
            // We use the hashmap state backend by default. This will
            // force the checkpoint storage loader to load
            // the configured storage backend.
            backend = new HashMapStateBackend();
        }
        return backend;
    }

    @Nonnull
    public static CheckpointStorage loadCheckpointStorage(
            Configuration configuration, ClassLoader classLoader, @Nullable Logger logger) {
        StateBackend backend = loadStateBackend(configuration, classLoader, logger);

        if (logger != null) {
            logger.info("Attempting to load configured checkpoint storage for savepoint disposal");
        }

        CheckpointStorage checkpointStorage = null;
        try {
            checkpointStorage =
                    CheckpointStorageLoader.load(
                            null, null, backend, configuration, classLoader, null);
        } catch (Throwable t) {
            // catches exceptions and errors (like linking errors)
            if (logger != null) {
                logger.info("Could not load configured state backend.");
                logger.debug("Detailed exception:", t);
            }
        }

        if (checkpointStorage == null) {
            // We use the jobmanager checkpoint storage by default.
            // The JobManagerCheckpointStorage is actually
            // FileSystem-based for metadata
            return new JobManagerCheckpointStorage();
        }
        return checkpointStorage;
    }

    // ------------------------------------------------------------------------

    /** This class contains only static utility methods and is not meant to be instantiated. */
    private Checkpoints() {}
}
