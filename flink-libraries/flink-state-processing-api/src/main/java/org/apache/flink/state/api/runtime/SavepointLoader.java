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

package org.apache.flink.state.api.runtime;

import org.apache.flink.annotation.Internal;
import org.apache.flink.runtime.checkpoint.Checkpoints;
import org.apache.flink.runtime.checkpoint.metadata.CheckpointMetadata;
import org.apache.flink.runtime.state.CompletedCheckpointStorageLocation;
import org.apache.flink.runtime.state.filesystem.AbstractFsCheckpointStorageAccess;

import java.io.DataInputStream;
import java.io.IOException;

/** Utility class for loading {@link CheckpointMetadata} metadata. */
@Internal
public final class SavepointLoader {
    private SavepointLoader() {}

    /**
     * 接受给定的字符串(表示指向检查点的指针)，并将其解析为检查点元数据文件的文件状态。
     *
     * <p>仅当用户代码类加载器是线程的当前类加载器时使用。
     *
     * Takes the given string (representing a pointer to a checkpoint) and resolves it to a file
     * status for the checkpoint's metadata file.
     *
     * <p>This should only be used when the user code class loader is the current classloader for
     * the thread.
     *
     * @param savepointPath The path to an external savepoint.
     * @return A state handle to savepoint's metadata.
     * @throws IOException Thrown, if the path cannot be resolved, the file system not accessed, or
     *     the path points to a location that does not seem to be a savepoint.
     */
    public static CheckpointMetadata loadSavepointMetadata(String savepointPath)
            throws IOException {
        CompletedCheckpointStorageLocation location =
                AbstractFsCheckpointStorageAccess.resolveCheckpointPointer(savepointPath);

        try (DataInputStream stream =
                new DataInputStream(location.getMetadataHandle().openInputStream())) {
            return Checkpoints.loadCheckpointMetadata(
                    stream, Thread.currentThread().getContextClassLoader(), savepointPath);
        }
    }
}
