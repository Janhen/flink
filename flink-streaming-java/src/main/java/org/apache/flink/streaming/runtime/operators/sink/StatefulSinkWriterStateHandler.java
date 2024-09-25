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

package org.apache.flink.streaming.runtime.operators.sink;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeutils.base.array.BytePrimitiveArraySerializer;
import org.apache.flink.api.connector.sink2.Sink.InitContext;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.StatefulSink;
import org.apache.flink.api.connector.sink2.StatefulSink.StatefulSinkWriter;
import org.apache.flink.api.connector.sink2.StatefulSink.WithCompatibleState;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.streaming.api.operators.util.SimpleVersionedListState;
import org.apache.flink.util.CollectionUtil;

import org.apache.flink.shaded.guava30.com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** {@link SinkWriterStateHandler} for stateful sinks. */
@Internal
final class StatefulSinkWriterStateHandler<InputT, WriterStateT>
        implements SinkWriterStateHandler<InputT> {

    /** The operator's state descriptor. */
    // J: 操作符的状态描述符。
    private static final ListStateDescriptor<byte[]> WRITER_RAW_STATES_DESC =
            new ListStateDescriptor<>("writer_raw_states", BytePrimitiveArraySerializer.INSTANCE);

    /** The writer operator's state serializer. */
    // 写入操作符的状态序列化器。
    private final SimpleVersionedSerializer<WriterStateT> writerStateSimpleVersionedSerializer;

    /**
     * 前一个接收操作符的状态名。我们允许从不同的(兼容的)接收器实现中恢复状态，例如
     * {@link org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink}。
     * 这允许迁移到更新的Sink实现。
     *
     * The previous sink operator's state name. We allow restoring state from a different
     * (compatible) sink implementation such as {@link
     * org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink}. This allows
     * migration to newer Sink implementations.
     */
    private final Collection<String> previousSinkStateNames;

    // J: 有状态的 Sink
    private final StatefulSink<InputT, WriterStateT> sink;

    // ------------------------------- runtime fields ---------------------------------------

    /**
     * 前一个接收操作符的状态。允许从不同的(兼容的)接收器实现中恢复状态，例如
     * {@link org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink}。
     * 这允许迁移到更新的Sink实现。
     *
     * The previous sink operator's state. We allow restoring state from a different (compatible)
     * sink implementation such as {@link
     * org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink}. This allows
     * migration to newer Sink implementations.
     */
    private List<ListState<WriterStateT>> previousSinkStates = new ArrayList<>();

    /** The operator's state. */
    // operator 的状态。
    private ListState<WriterStateT> writerState;

    private StatefulSinkWriter<InputT, WriterStateT> sinkWriter;

    public StatefulSinkWriterStateHandler(StatefulSink<InputT, WriterStateT> sink) {
        this.sink = sink;
        Collection<String> previousSinkStateNames =
                sink instanceof StatefulSink.WithCompatibleState
                        ? ((WithCompatibleState) sink).getCompatibleWriterStateNames()  // J: 兼容状态
                        : Collections.emptyList();
        this.writerStateSimpleVersionedSerializer = sink.getWriterStateSerializer();
        this.previousSinkStateNames = previousSinkStateNames;
    }

    @Override
    public SinkWriter<InputT> createWriter(
            InitContext initContext, StateInitializationContext context) throws Exception {
        // J: 获取 list state
        // J: 原始的 byte[]
        final ListState<byte[]> rawState =
                context.getOperatorStateStore().getListState(WRITER_RAW_STATES_DESC);
        // J: 根据 byte[] 原始的状态转换成需要的类型
        writerState =
                new SimpleVersionedListState<>(rawState, writerStateSimpleVersionedSerializer);

        // J: 需要恢复的
        // 如果从前一次执行的快照恢复状态，则返回 true
        if (context.isRestored()) {
            final List<WriterStateT> writerStates =
                    CollectionUtil.iterableToList(writerState.get());
            final List<WriterStateT> states = new ArrayList<>(writerStates);

            // J: 之前的状态名获取
            for (String previousSinkStateName : previousSinkStateNames) {
                // J: 提取
                final ListStateDescriptor<byte[]> preSinkStateDesc =
                        new ListStateDescriptor<>(
                                previousSinkStateName, BytePrimitiveArraySerializer.INSTANCE);
                // J: 之前的原始状态
                final ListState<byte[]> preRawState =
                        context.getOperatorStateStore().getListState(preSinkStateDesc);
                // J: 对应之前的...
                SimpleVersionedListState<WriterStateT> previousSinkState =
                        new SimpleVersionedListState<>(
                                preRawState, writerStateSimpleVersionedSerializer);
                previousSinkStates.add(previousSinkState);
                // J: 之前的元素统一放到一起
                Iterables.addAll(states, previousSinkState.get());
            }
            // J: 当前的状态元素 + 恢复之前的元素
            sinkWriter = sink.restoreWriter(initContext, states);
        } else {
            sinkWriter = sink.createWriter(initContext);
        }
        return sinkWriter;
    }

    @Override
    public void snapshotState(long checkpointId) throws Exception {
        writerState.update(sinkWriter.snapshotState(checkpointId));
        // J: 之前状态清理掉...
        previousSinkStates.forEach(ListState::clear);
    }
}
