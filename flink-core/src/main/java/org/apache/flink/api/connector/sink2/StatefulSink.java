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

package org.apache.flink.api.connector.sink2;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.core.io.SimpleVersionedSerializer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * 带有状态{@link Sink}的{@link Sink}。
 *
 * <p> {@link StatefulSink}需要是可序列化的。所有的配置都应该被急切地验证。各自的接收写入器是临时的，只能在任务
 * 管理器上的子任务中创建。
 *
 * A {@link Sink} with a stateful {@link SinkWriter}.
 *
 * <p>The {@link StatefulSink} needs to be serializable. All configuration should be validated
 * eagerly. The respective sink writers are transient and will only be created in the subtasks on
 * the taskmanagers.
 *
 * @param <InputT> The type of the sink's input
 * @param <WriterStateT> The type of the sink writer's state
 */
@PublicEvolving
public interface StatefulSink<InputT, WriterStateT> extends Sink<InputT> {

    /**
     * 覆盖了 {@link Sink#createWriter(InitContext)}
     * 
     * Create a {@link StatefulSinkWriter}.
     *
     * @param context the runtime context.
     * @return A sink writer.
     * @throws IOException for any failure during creation.
     */
    StatefulSinkWriter<InputT, WriterStateT> createWriter(InitContext context) throws IOException;

    /**
     * Create a {@link StatefulSinkWriter} from a recovered state.
     *
     * @param context the runtime context.
     * @return A sink writer.
     * @throws IOException for any failure during creation.
     */
    StatefulSinkWriter<InputT, WriterStateT> restoreWriter(
            InitContext context, Collection<WriterStateT> recoveredState) throws IOException;

    /**
     * 任何有状态接收器都需要提供这个状态序列化器，并正确地实现{@link StatefulSinkWriter#snapshotState(long)}。
     * 各自的状态在{@link #restoreWriter(InitContext, Collection)}中用于恢复。
     *
     * Any stateful sink needs to provide this state serializer and implement {@link
     * StatefulSinkWriter#snapshotState(long)} properly. The respective state is used in {@link
     * #restoreWriter(InitContext, Collection)} on recovery.
     *
     * @return the serializer of the writer's state type.
     */
    SimpleVersionedSerializer<WriterStateT> getWriterStateSerializer();

    /**
     * {@link StatefulSink} 的混合，允许用户从具有兼容状态的接收器迁移到此接收器。
     *
     * A mix-in for {@link StatefulSink} that allows users to migrate from a sink with a compatible
     * state to this sink.
     */
    @PublicEvolving
    interface WithCompatibleState {
        /**
         * A list of state names of sinks from which the state can be restored. For example, the new
         * {@code FileSink} can resume from the state of an old {@code StreamingFileSink} as a
         * drop-in replacement when resuming from a checkpoint/savepoint.
         */
        Collection<String> getCompatibleWriterStateNames();
    }

    /**
     * {@link SinkWriter} 的状态需要被检查点。
     *
     * A {@link SinkWriter} whose state needs to be checkpointed.
     *
     * @param <InputT> The type of the sink writer's input
     * @param <WriterStateT> The type of the writer's state
     */
    @PublicEvolving
    interface StatefulSinkWriter<InputT, WriterStateT> extends SinkWriter<InputT> {
        /**
         * @return The writer's state.
         * @throws IOException if fail to snapshot writer's state.
         */
        List<WriterStateT> snapshotState(long checkpointId) throws IOException;
    }
}
