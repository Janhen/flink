/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.runtime.operators.sink;

import org.apache.flink.api.common.eventtime.TimestampAssigner;
import org.apache.flink.api.common.operators.MailboxExecutor;
import org.apache.flink.api.common.serialization.SerializationSchema.InitializationContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeutils.base.array.BytePrimitiveArraySerializer;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.Sink.InitContext;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.StatefulSink;
import org.apache.flink.api.connector.sink2.TwoPhaseCommittingSink;
import org.apache.flink.api.connector.sink2.TwoPhaseCommittingSink.PrecommittingSinkWriter;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.metrics.groups.OperatorMetricGroup;
import org.apache.flink.metrics.groups.SinkWriterMetricGroup;
import org.apache.flink.runtime.metrics.groups.InternalSinkWriterMetricGroup;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.StateSnapshotContext;
import org.apache.flink.streaming.api.connector.sink2.CommittableMessage;
import org.apache.flink.streaming.api.connector.sink2.CommittableSummary;
import org.apache.flink.streaming.api.connector.sink2.CommittableWithLineage;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.BoundedOneInput;
import org.apache.flink.streaming.api.operators.InternalTimerService;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.api.operators.util.SimpleVersionedListState;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.util.UserCodeClassLoader;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalLong;

import static org.apache.flink.util.IOUtils.closeAll;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * 一个操作符，用于处理要写入{@link org.apache.flink.api.connector.sink.Sink}的记录。它也有一种方法来处理
 * 具有相同并行性的可提交文件，或者将它们发送到具有不同并行性的下游{@link CommitterOperator}。
 *
 * <p>操作符始终是汇聚管道的一部分，并且是第一个操作符。
 *
 * An operator that processes records to be written into a {@link
 * org.apache.flink.api.connector.sink.Sink}. It also has a way to process committables with the
 * same parallelism or send them downstream to a {@link CommitterOperator} with a different
 * parallelism.
 *
 * <p>The operator is always part of a sink pipeline and is the first operator.
 *
 * @param <InputT> the type of the committable
 * @param <CommT> the type of the committable (to send to downstream operators)
 */
class SinkWriterOperator<InputT, CommT> extends AbstractStreamOperator<CommittableMessage<CommT>>
        implements OneInputStreamOperator<InputT, CommittableMessage<CommT>>, BoundedOneInput {

    /**
     * 为了支持从1.14开始的状态迁移，其中sinkWriter和committer是同一操作符的一部分。
     *
     * To support state migrations from 1.14 where the sinkWriter and committer where part of the
     * same operator.
     */
    private static final ListStateDescriptor<byte[]> STREAMING_COMMITTER_RAW_STATES_DESC =
            new ListStateDescriptor<>(
                    "streaming_committer_raw_states", BytePrimitiveArraySerializer.INSTANCE);

    // 2PC Sink 时初始化
    @Nullable private final SimpleVersionedSerializer<CommT> committableSerializer;
    private final List<CommT> legacyCommittables = new ArrayList<>();

    /** The runtime information of the input element. */
    // 输入元素的运行时信息
    private final Context<InputT> context;

    private final boolean emitDownstream;

    // ------------------------------- runtime fields ---------------------------------------

    /** We listen to this ourselves because we don't have an {@link InternalTimerService}. */
    private Long currentWatermark = Long.MIN_VALUE;

    private SinkWriter<InputT> sinkWriter;

    // J: sink write 状态处理器，分为有状态的和无状态的
    private final SinkWriterStateHandler<InputT> writerStateHandler;

    // J: Mailbox 线程模型
    private final MailboxExecutor mailboxExecutor;

    private boolean endOfInput = false;

    SinkWriterOperator(
            Sink<InputT> sink,
            ProcessingTimeService processingTimeService,
            MailboxExecutor mailboxExecutor) {
        this.processingTimeService = checkNotNull(processingTimeService);
        this.mailboxExecutor = checkNotNull(mailboxExecutor);
        this.context = new Context<>();
        // J: 确定是否是两阶段提交的
        this.emitDownstream = sink instanceof TwoPhaseCommittingSink;

        if (sink instanceof StatefulSink) {
            writerStateHandler =
                    new StatefulSinkWriterStateHandler<>((StatefulSink<InputT, ?>) sink);
        } else {
            writerStateHandler = new StatelessSinkWriterStateHandler<>(sink);
        }

        if (sink instanceof TwoPhaseCommittingSink) {
            // J: 2PC sink 获取 可提交类型的序列化器
            committableSerializer =
                    ((TwoPhaseCommittingSink<InputT, CommT>) sink).getCommittableSerializer();
        } else {
            committableSerializer = null;
        }
    }

    @Override
    public void initializeState(StateInitializationContext context) throws Exception {
        super.initializeState(context);
        OptionalLong checkpointId = context.getRestoredCheckpointId();
        InitContext initContext =
                createInitContext(checkpointId.isPresent() ? checkpointId.getAsLong() : null);
        if (context.isRestored()) {
            if (committableSerializer != null) {
                // J: 提取原始的
                final ListState<List<CommT>> legacyCommitterState =
                        new SimpleVersionedListState<>(
                                context.getOperatorStateStore()
                                        .getListState(STREAMING_COMMITTER_RAW_STATES_DESC),
                                new SinkV1WriterCommittableSerializer<>(committableSerializer));
                // 遗留提交者状态
                legacyCommitterState.get().forEach(legacyCommittables::addAll);
            }
        }
        // J: sink writer 由 writer state handler 中创建
        sinkWriter = writerStateHandler.createWriter(initContext, context);
    }

    @Override
    public void snapshotState(StateSnapshotContext context) throws Exception {
        super.snapshotState(context);
        // J: 状态的 snapshot state 处理
        writerStateHandler.snapshotState(context.getCheckpointId());
    }

    @Override
    public void processElement(StreamRecord<InputT> element) throws Exception {
        context.element = element;
        // J: 元素写入到 sink writer
        sinkWriter.write(element.getValue(), context);
    }

    @Override
    public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
        super.prepareSnapshotPreBarrier(checkpointId);
        if (!endOfInput) {
            sinkWriter.flush(false);
            emitCommittables(checkpointId);
        }
        // no records are expected to emit after endOfInput
    }

    @Override
    public void processWatermark(Watermark mark) throws Exception {
        super.processWatermark(mark);
        this.currentWatermark = mark.getTimestamp();
        // J: 水印的写入
        sinkWriter.writeWatermark(
                new org.apache.flink.api.common.eventtime.Watermark(mark.getTimestamp()));
    }

    @Override
    public void endInput() throws Exception {
        endOfInput = true;
        sinkWriter.flush(true);
        emitCommittables(Long.MAX_VALUE);
    }

    private void emitCommittables(Long checkpointId) throws IOException, InterruptedException {
        if (!emitDownstream) {
            // To support SinkV1 topologies with only a writer we have to call prepareCommit
            // although no committables are forwarded
            // 为了支持只有一个写入器的SinkV1拓扑，我们必须调用prepareCommit，尽管没有提交被转发
            if (sinkWriter instanceof PrecommittingSinkWriter) {
                ((PrecommittingSinkWriter<?, ?>) sinkWriter).prepareCommit();
            }
            return;
        }
        Collection<CommT> committables =
                ((PrecommittingSinkWriter<?, CommT>) sinkWriter).prepareCommit();
        StreamingRuntimeContext runtimeContext = getRuntimeContext();
        final int indexOfThisSubtask = runtimeContext.getIndexOfThisSubtask();
        final int numberOfParallelSubtasks = runtimeContext.getNumberOfParallelSubtasks();

        // Emit only committable summary if there are legacy committables
        if (!legacyCommittables.isEmpty()) {
            checkState(checkpointId > InitContext.INITIAL_CHECKPOINT_ID);
            emit(
                    indexOfThisSubtask,
                    numberOfParallelSubtasks,
                    InitContext.INITIAL_CHECKPOINT_ID,
                    legacyCommittables);
            legacyCommittables.clear();
        }
        emit(indexOfThisSubtask, numberOfParallelSubtasks, checkpointId, committables);
    }

    @Override
    public void close() throws Exception {
        closeAll(sinkWriter, super::close);
    }

    /**
     * Skip registering numRecordsOut counter on output.
     *
     * <p>Metric "numRecordsOut" is defined as the total number of records written to the external
     * system in FLIP-33, but this metric is occupied in AbstractStreamOperator as the number of
     * records sent to downstream operators, which is number of Committable batches sent to
     * SinkCommitter. So we skip registering this metric on output and leave this metric to sink
     * writer implementations to report.
     */
    @Override
    protected Output<StreamRecord<CommittableMessage<CommT>>> registerCounterOnOutput(
            Output<StreamRecord<CommittableMessage<CommT>>> output,
            OperatorMetricGroup operatorMetricGroup) {
        return output;
    }

    private void emit(
            int indexOfThisSubtask,
            int numberOfParallelSubtasks,
            long checkpointId,
            Collection<CommT> committables) {
        output.collect(
                new StreamRecord<>(
                        new CommittableSummary<>(
                                indexOfThisSubtask,
                                numberOfParallelSubtasks,
                                checkpointId,
                                committables.size(),
                                committables.size(),
                                0)));
        for (CommT committable : committables) {
            output.collect(
                    new StreamRecord<>(
                            new CommittableWithLineage<>(
                                    committable, checkpointId, indexOfThisSubtask)));
        }
    }

    private Sink.InitContext createInitContext(@Nullable Long restoredCheckpointId) {
        return new InitContextImpl(
                getRuntimeContext(),
                processingTimeService,
                mailboxExecutor,
                InternalSinkWriterMetricGroup.wrap(getMetricGroup()),
                restoredCheckpointId);
    }

    private class Context<IN> implements SinkWriter.Context {

        private StreamRecord<IN> element;

        @Override
        public long currentWatermark() {
            return currentWatermark;
        }

        @Override
        public Long timestamp() {
            if (element.hasTimestamp()
                    && element.getTimestamp() != TimestampAssigner.NO_TIMESTAMP) {
                return element.getTimestamp();
            }
            return null;
        }
    }

    private static class InitContextImpl implements Sink.InitContext {

        private final ProcessingTimeService processingTimeService;

        private final MailboxExecutor mailboxExecutor;

        private final SinkWriterMetricGroup metricGroup;

        @Nullable private final Long restoredCheckpointId;

        private final StreamingRuntimeContext runtimeContext;

        public InitContextImpl(
                StreamingRuntimeContext runtimeContext,
                ProcessingTimeService processingTimeService,
                MailboxExecutor mailboxExecutor,
                SinkWriterMetricGroup metricGroup,
                @Nullable Long restoredCheckpointId) {
            this.runtimeContext = checkNotNull(runtimeContext);
            this.mailboxExecutor = checkNotNull(mailboxExecutor);
            this.processingTimeService = checkNotNull(processingTimeService);
            this.metricGroup = checkNotNull(metricGroup);
            this.restoredCheckpointId = restoredCheckpointId;
        }

        @Override
        public UserCodeClassLoader getUserCodeClassLoader() {
            return new UserCodeClassLoader() {
                @Override
                public ClassLoader asClassLoader() {
                    return runtimeContext.getUserCodeClassLoader();
                }

                @Override
                public void registerReleaseHookIfAbsent(
                        String releaseHookName, Runnable releaseHook) {
                    runtimeContext.registerUserCodeClassLoaderReleaseHookIfAbsent(
                            releaseHookName, releaseHook);
                }
            };
        }

        @Override
        public int getNumberOfParallelSubtasks() {
            return runtimeContext.getNumberOfParallelSubtasks();
        }

        @Override
        public MailboxExecutor getMailboxExecutor() {
            return mailboxExecutor;
        }

        @Override
        public org.apache.flink.api.common.operators.ProcessingTimeService
                getProcessingTimeService() {
            return processingTimeService;
        }

        @Override
        public int getSubtaskId() {
            return runtimeContext.getIndexOfThisSubtask();
        }

        @Override
        public SinkWriterMetricGroup metricGroup() {
            return metricGroup;
        }

        @Override
        public OptionalLong getRestoredCheckpointId() {
            return restoredCheckpointId == null
                    ? OptionalLong.empty()
                    : OptionalLong.of(restoredCheckpointId);
        }

        @Override
        public InitializationContext asSerializationSchemaInitializationContext() {
            return new InitContextInitializationContextAdapter(
                    getUserCodeClassLoader(), () -> metricGroup.addGroup("user"));
        }
    }
}
