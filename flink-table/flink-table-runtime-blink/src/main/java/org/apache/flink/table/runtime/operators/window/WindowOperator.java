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

package org.apache.flink.table.runtime.operators.window;

import org.apache.flink.api.common.state.MergingState;
import org.apache.flink.api.common.state.State;
import org.apache.flink.api.common.state.StateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.state.internal.InternalMergingState;
import org.apache.flink.runtime.state.internal.InternalValueState;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.InternalTimer;
import org.apache.flink.streaming.api.operators.InternalTimerService;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.TimestampedCollector;
import org.apache.flink.streaming.api.operators.Triggerable;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.util.RowDataUtil;
import org.apache.flink.table.runtime.dataview.PerWindowStateDataViewStore;
import org.apache.flink.table.runtime.generated.NamespaceAggsHandleFunctionBase;
import org.apache.flink.table.runtime.operators.window.assigners.MergingWindowAssigner;
import org.apache.flink.table.runtime.operators.window.assigners.PanedWindowAssigner;
import org.apache.flink.table.runtime.operators.window.assigners.WindowAssigner;
import org.apache.flink.table.runtime.operators.window.internal.GeneralWindowProcessFunction;
import org.apache.flink.table.runtime.operators.window.internal.InternalWindowProcessFunction;
import org.apache.flink.table.runtime.operators.window.internal.MergingWindowProcessFunction;
import org.apache.flink.table.runtime.operators.window.internal.PanedWindowProcessFunction;
import org.apache.flink.table.runtime.operators.window.triggers.Trigger;
import org.apache.flink.table.runtime.typeutils.RowDataSerializer;
import org.apache.flink.table.runtime.util.TimeWindowUtil;
import org.apache.flink.table.types.logical.LogicalType;

import org.apache.commons.lang3.ArrayUtils;

import java.time.ZoneId;
import java.util.Collection;

import static java.util.Objects.requireNonNull;
import static org.apache.flink.table.runtime.util.TimeWindowUtil.toEpochMillsForTimer;
import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * 一个基于 {@link WindowAssigner} 和 {@link Trigger} 实现窗口逻辑的操作符。
 *
 * <p>这是 {@link AggregateWindowOperator} 和 {@link TableAggregateWindowOperator} 的基类。
 *   {@link AggregateWindowOperator} 和 {@link TableAggregateWindowOperator} 之间的最大区别是
 *   {@link AggregateWindowOperator} 对每个聚合组只发出一个结果，而 {@link TableAggregateWindowOperator}
 *   可以为每个聚合组发出多个结果。
 *
 * <p>当一个元素到达时，它被分配一个键使用 {@link KeySelector}，它被分配到零或多个窗口使用 {@link WindowAssigner}。
 *   在此基础上，将元素放入窗格中。窗格是具有相同键和相同 {@code Window} 的元素桶。如果一个元素被
 *   {@code WindowAssigner}分 配给多个窗口，那么它可以在多个窗格中。
 *
 * <p>每个窗格都有自己提供的 {@code Trigger} 实例。此触发器确定何时应处理窗格的内容以发出结果。当触发器触发时，调用
 *  给定的 {@link org.apache.flink.table.runtime.generated.NamespaceAggsHandleFunctionBase} 以生成为
 *  {@code Trigger} 所属的窗格发出的结果。
 *
 * <p>参数类型:{@code <IN>}: RowData {@code <OUT>}: JoinedRowData(KEY, AGG_RESULT) {@code <KEY>}:
 *   GenericRowData {@code <AGG_RESULT>}: GenericRowData {@code <ACC>}: GenericRowData
 *
 * An operator that implements the logic for windowing based on a {@link WindowAssigner} and {@link
 * Trigger}.
 *
 * <p>This is the base class for {@link AggregateWindowOperator} and {@link
 * TableAggregateWindowOperator}. The big difference between {@link AggregateWindowOperator} and
 * {@link TableAggregateWindowOperator} is {@link AggregateWindowOperator} emits only one result for
 * each aggregate group, while {@link TableAggregateWindowOperator} can emit multi results for each
 * aggregate group.
 *
 * <p>When an element arrives it gets assigned a key using a {@link KeySelector} and it gets
 * assigned to zero or more windows using a {@link WindowAssigner}. Based on this, the element is
 * put into panes. A pane is the bucket of elements that have the same key and same {@code Window}.
 * An element can be in multiple panes if it was assigned to multiple windows by the {@code
 * WindowAssigner}.
 *
 * <p>Each pane gets its own instance of the provided {@code Trigger}. This trigger determines when
 * the contents of the pane should be processed to emit results. When a trigger fires, the given
 * {@link org.apache.flink.table.runtime.generated.NamespaceAggsHandleFunctionBase} is invoked to
 * produce the results that are emitted for the pane to which the {@code Trigger} belongs.
 *
 * <p>The parameter types: {@code <IN>}: RowData {@code <OUT>}: JoinedRowData(KEY, AGG_RESULT)
 * {@code <KEY>}: GenericRowData {@code <AGG_RESULT>}: GenericRowData {@code <ACC>}: GenericRowData
 *
 * @param <K> The type of key returned by the {@code KeySelector}.
 * @param <W> The type of {@code Window} that the {@code WindowAssigner} assigns.
 */
public abstract class WindowOperator<K, W extends Window> extends AbstractStreamOperator<RowData>
        implements OneInputStreamOperator<RowData, RowData>, Triggerable<K, W> {

    private static final long serialVersionUID = 1L;

    // J: 丢弃的延迟数据数量
    private static final String LATE_ELEMENTS_DROPPED_METRIC_NAME = "numLateRecordsDropped";
    // J: 丢弃的延迟数据比率
    private static final String LATE_ELEMENTS_DROPPED_RATE_METRIC_NAME = "lateRecordsDroppedRate";
    // 水印延迟指标名
    private static final String WATERMARK_LATENCY_METRIC_NAME = "watermarkLatency";

    // ------------------------------------------------------------------------
    // Configuration values and user functions
    // ------------------------------------------------------------------------

    private final WindowAssigner<W> windowAssigner;

    private final Trigger<W> trigger;

    /** For serializing the window in checkpoints. */
    // 用于在检查点中序列化窗口。
    private final TypeSerializer<W> windowSerializer;

    private final LogicalType[] inputFieldTypes;

    // 累加器类型
    private final LogicalType[] accumulatorTypes;

    // 聚合结果类型
    private final LogicalType[] aggResultTypes;

    // 窗口属性类型
    private final LogicalType[] windowPropertyTypes;

    // 生产更新？
    protected final boolean produceUpdates;

    /**
     * 窗口的移位时区，如果 proctime 或 rowtime 类型为 TIMESTAMP_LTZ，则移位时区为tablecconfig中配置的时区用户，
     * 其他情况下，时区为UTC，这意味着在分配窗口时永远不会移位。
     *
     * The shift timezone of the window, if the proctime or rowtime type is TIMESTAMP_LTZ, the shift
     * timezone is the timezone user configured in TableConfig, other cases the timezone is UTC
     * which means never shift when assigning windows.
     */
    protected final ZoneId shiftTimeZone;

    // 事件时间索引
    private final int rowtimeIndex;

    /**
     * 元素允许的延迟。这用于:
     *
     *   <li>决定一个元素是否应该由于延迟而从窗口中删除。
     *   <li>当系统时间超过 {@code 窗口时，清除窗口状态。maxTimestamp + allowedlate} landmark。
     *
     * The allowed lateness for elements. This is used for:
     *
     * <ul>
     *   <li>Deciding if an element should be dropped from a window due to lateness.
     *   <li>Clearing the state of a window if the system time passes the {@code window.maxTimestamp
     *       + allowedLateness} landmark.
     * </ul>
     */
    private final long allowedLateness;

    // --------------------------------------------------------------------------------

    // 命名空间聚合处理
    protected NamespaceAggsHandleFunctionBase<W> windowAggregator;

    // --------------------------------------------------------------------------------

    // 内部窗口处理函数
    protected transient InternalWindowProcessFunction<K, W> windowFunction;

    /** This is used for emitting elements with a given timestamp. */
    // 这用于发出带有给定时间戳的元素。
    protected transient TimestampedCollector<RowData> collector;

    /** Flag to prevent duplicate function.close() calls in close() and dispose(). */
    // 标志以防止在 close() 和 dispose() 中重复调用 function.close()。
    private transient boolean functionsClosed = false;

    private transient InternalTimerService<W> internalTimerService;

    private transient InternalValueState<K, W, RowData> windowState;

    protected transient InternalValueState<K, W, RowData> previousState;

    private transient TriggerContext triggerContext;

    // ------------------------------------------------------------------------
    // Metrics
    // ------------------------------------------------------------------------

    private transient Counter numLateRecordsDropped;
    private transient Meter lateRecordsDroppedRate;
    private transient Gauge<Long> watermarkLatency;

    WindowOperator(
            NamespaceAggsHandleFunctionBase<W> windowAggregator,
            WindowAssigner<W> windowAssigner,
            Trigger<W> trigger,
            TypeSerializer<W> windowSerializer,
            LogicalType[] inputFieldTypes,
            LogicalType[] accumulatorTypes,
            LogicalType[] aggResultTypes,
            LogicalType[] windowPropertyTypes,
            int rowtimeIndex,
            boolean produceUpdates,
            long allowedLateness,
            ZoneId shiftTimeZone) {
        checkArgument(allowedLateness >= 0);
        this.windowAggregator = checkNotNull(windowAggregator);
        this.windowAssigner = checkNotNull(windowAssigner);
        this.trigger = checkNotNull(trigger);
        this.windowSerializer = checkNotNull(windowSerializer);
        this.inputFieldTypes = checkNotNull(inputFieldTypes);
        this.accumulatorTypes = checkNotNull(accumulatorTypes);
        this.aggResultTypes = checkNotNull(aggResultTypes);
        this.windowPropertyTypes = checkNotNull(windowPropertyTypes);
        this.allowedLateness = allowedLateness;
        this.produceUpdates = produceUpdates;

        // rowtime index should >= 0 when in event time mode
        checkArgument(!windowAssigner.isEventTime() || rowtimeIndex >= 0);
        this.rowtimeIndex = rowtimeIndex;
        this.shiftTimeZone = shiftTimeZone;
        setChainingStrategy(ChainingStrategy.ALWAYS);
    }

    WindowOperator(
            WindowAssigner<W> windowAssigner,
            Trigger<W> trigger,
            TypeSerializer<W> windowSerializer,
            LogicalType[] inputFieldTypes,
            LogicalType[] accumulatorTypes,
            LogicalType[] aggResultTypes,
            LogicalType[] windowPropertyTypes,
            int rowtimeIndex,
            boolean produceUpdates,
            long allowedLateness,
            ZoneId shiftTimeZone) {
        checkArgument(allowedLateness >= 0);
        this.windowAssigner = checkNotNull(windowAssigner);
        this.trigger = checkNotNull(trigger);
        this.windowSerializer = checkNotNull(windowSerializer);
        this.inputFieldTypes = checkNotNull(inputFieldTypes);
        this.accumulatorTypes = checkNotNull(accumulatorTypes);
        this.aggResultTypes = checkNotNull(aggResultTypes);
        this.windowPropertyTypes = checkNotNull(windowPropertyTypes);
        this.allowedLateness = allowedLateness;
        this.produceUpdates = produceUpdates;

        // rowtime index should >= 0 when in event time mode
        checkArgument(!windowAssigner.isEventTime() || rowtimeIndex >= 0);
        this.rowtimeIndex = rowtimeIndex;
        this.shiftTimeZone = shiftTimeZone;

        setChainingStrategy(ChainingStrategy.ALWAYS);
    }

    protected abstract void compileGeneratedCode();

    @Override
    public void open() throws Exception {
        super.open();

        functionsClosed = false;

        collector = new TimestampedCollector<>(output);
        collector.eraseTimestamp();

        internalTimerService = getInternalTimerService("window-timers", windowSerializer, this);

        triggerContext = new TriggerContext();
        triggerContext.open();

        // 窗口聚合状态
        StateDescriptor<ValueState<RowData>, RowData> windowStateDescriptor =
                new ValueStateDescriptor<>("window-aggs", new RowDataSerializer(accumulatorTypes));
        this.windowState =
                (InternalValueState<K, W, RowData>)
                        getOrCreateKeyedState(windowSerializer, windowStateDescriptor);

        if (produceUpdates) {
            LogicalType[] valueTypes = ArrayUtils.addAll(aggResultTypes, windowPropertyTypes);
            // 之前的聚合状态
            StateDescriptor<ValueState<RowData>, RowData> previousStateDescriptor =
                    new ValueStateDescriptor<>("previous-aggs", new RowDataSerializer(valueTypes));
            this.previousState =
                    (InternalValueState<K, W, RowData>)
                            getOrCreateKeyedState(windowSerializer, previousStateDescriptor);
        }

        // 编译生成的代码
        compileGeneratedCode();

        WindowContext windowContext = new WindowContext();
        windowAggregator.open(
                // 每个窗口状态数据视图存储
                new PerWindowStateDataViewStore(
                        getKeyedStateBackend(), windowSerializer, getRuntimeContext()));

        // 合并窗口指定
        if (windowAssigner instanceof MergingWindowAssigner) {
            this.windowFunction =
                    new MergingWindowProcessFunction<>(
                            (MergingWindowAssigner<W>) windowAssigner,
                            windowAggregator,
                            windowSerializer,
                            allowedLateness);
        } else if (windowAssigner instanceof PanedWindowAssigner) {
            // 窗格窗口指定
            this.windowFunction =
                    new PanedWindowProcessFunction<>(
                            (PanedWindowAssigner<W>) windowAssigner,
                            windowAggregator,
                            allowedLateness);
        } else {
            // 通用窗口处理
            this.windowFunction =
                    new GeneralWindowProcessFunction<>(
                            windowAssigner, windowAggregator, allowedLateness);
        }
        windowFunction.open(windowContext);

        // metrics
        this.numLateRecordsDropped = metrics.counter(LATE_ELEMENTS_DROPPED_METRIC_NAME);
        // 初始化窗口丢弃的数量
        this.lateRecordsDroppedRate =
                metrics.meter(
                        LATE_ELEMENTS_DROPPED_RATE_METRIC_NAME,
                        new MeterView(numLateRecordsDropped));
        this.watermarkLatency =
                metrics.gauge(
                        WATERMARK_LATENCY_METRIC_NAME,
                        () -> {
                            long watermark = internalTimerService.currentWatermark();
                            if (watermark < 0) {
                                return 0L;
                            } else {
                                return internalTimerService.currentProcessingTime() - watermark;
                            }
                        });
    }

    @Override
    public void close() throws Exception {
        super.close();
        collector = null;
        triggerContext = null;
        functionsClosed = true;
        if (windowAggregator != null) {
            windowAggregator.close();
        }
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
        collector = null;
        triggerContext = null;
        if (!functionsClosed) {
            functionsClosed = true;
            if (windowAggregator != null) {
                windowAggregator.close();
            }
        }
    }

    @Override
    public void processElement(StreamRecord<RowData> record) throws Exception {
        RowData inputRow = record.getValue();
        long timestamp;
        if (windowAssigner.isEventTime()) {
            // 若为事件时间指派，则取事件时间
            timestamp = inputRow.getLong(rowtimeIndex);
        } else {
            // 取处理时间
            timestamp = internalTimerService.currentProcessingTime();
        }

        // 根据时区确定新的时间戳
        timestamp = TimeWindowUtil.toUtcTimestampMills(timestamp, shiftTimeZone);

        // the windows which the input row should be placed into
        // 根据时间戳确定输入行应该放入的窗口，返回多个
        // 分配状态名称空间
        Collection<W> affectedWindows = windowFunction.assignStateNamespace(inputRow, timestamp);
        // 标记当前 element 是否被丢弃
        boolean isElementDropped = true;
        for (W window : affectedWindows) {
            isElementDropped = false;

            windowState.setCurrentNamespace(window);
            RowData acc = windowState.value();
            if (acc == null) {
                acc = windowAggregator.createAccumulators();
            }
            windowAggregator.setAccumulators(window, acc);

            // 是否是聚合的累积操作
            if (RowDataUtil.isAccumulateMsg(inputRow)) {
                windowAggregator.accumulate(inputRow);
            } else {
                windowAggregator.retract(inputRow);
            }
            acc = windowAggregator.getAccumulators();
            windowState.update(acc);
        }

        // the actual window which the input row is belongs to
        // 输入行所属的实际窗口
        Collection<W> actualWindows = windowFunction.assignActualWindows(inputRow, timestamp);
        for (W window : actualWindows) {
            isElementDropped = false;
            triggerContext.window = window;
            // 根据当前元素的时间戳和输入行确定是否触发
            boolean triggerResult = triggerContext.onElement(inputRow, timestamp);
            if (triggerResult) {
                // 将窗口结果发送
                emitWindowResult(window);
            }
            // register a clean up timer for the window
            // 为窗口注册一个清理计时器
            registerCleanupTimer(window);
        }

        if (isElementDropped) {
            // markEvent will increase numLateRecordsDropped
            // markEvent 将增加 numLateRecordsDropped
            lateRecordsDroppedRate.markEvent();
        }
    }

    @Override
    public void onEventTime(InternalTimer<K, W> timer) throws Exception {
        setCurrentKey(timer.getKey());

        triggerContext.window = timer.getNamespace();
        if (triggerContext.onEventTime(timer.getTimestamp())) {
            // fire
            emitWindowResult(triggerContext.window);
        }

        if (windowAssigner.isEventTime()) {
            windowFunction.cleanWindowIfNeeded(triggerContext.window, timer.getTimestamp());
        }
    }

    @Override
    public void onProcessingTime(InternalTimer<K, W> timer) throws Exception {
        if (functionsClosed) {
            return;
        }

        setCurrentKey(timer.getKey());

        triggerContext.window = timer.getNamespace();
        if (triggerContext.onProcessingTime(timer.getTimestamp())) {
            // fire
            emitWindowResult(triggerContext.window);
        }

        if (!windowAssigner.isEventTime()) {
            windowFunction.cleanWindowIfNeeded(triggerContext.window, timer.getTimestamp());
        }
    }

    /** Emits the window result of the given window. */
    protected abstract void emitWindowResult(W window) throws Exception;

    /**
     * 注册一个定时器来清除窗口的内容。
     *
     * Registers a timer to cleanup the content of the window.
     *
     * @param window the window whose state to discard
     */
    private void registerCleanupTimer(W window) {
        long cleanupTime = toEpochMillsForTimer(cleanupTime(window), shiftTimeZone);
        if (cleanupTime == Long.MAX_VALUE) {
            // don't set a GC timer for "end of time"
            return;
        }

        if (windowAssigner.isEventTime()) {
            triggerContext.registerEventTimeTimer(cleanupTime);
        } else {
            triggerContext.registerProcessingTimeTimer(cleanupTime);
        }
    }

    /**
     * Returns the cleanup time for a window, which is {@code window.maxTimestamp +
     * allowedLateness}. In case this leads to a value greated than {@link Long#MAX_VALUE} then a
     * cleanup time of {@link Long#MAX_VALUE} is returned.
     *
     * @param window the window whose cleanup time we are computing.
     */
    private long cleanupTime(W window) {
        if (windowAssigner.isEventTime()) {
            long cleanupTime = Math.max(0, window.maxTimestamp() + allowedLateness);
            return cleanupTime >= window.maxTimestamp() ? cleanupTime : Long.MAX_VALUE;
        } else {
            return Math.max(0, window.maxTimestamp());
        }
    }

    @SuppressWarnings("unchecked")
    private K currentKey() {
        return (K) getCurrentKey();
    }

    // ------------------------------------------------------------------------------

    /** Context of window. */
    private class WindowContext implements InternalWindowProcessFunction.Context<K, W> {

        @Override
        public <S extends State> S getPartitionedState(StateDescriptor<S, ?> stateDescriptor)
                throws Exception {
            requireNonNull(stateDescriptor, "The state properties must not be null");
            return WindowOperator.this.getPartitionedState(stateDescriptor);
        }

        @Override
        public K currentKey() {
            return WindowOperator.this.currentKey();
        }

        @Override
        public long currentProcessingTime() {
            return internalTimerService.currentProcessingTime();
        }

        @Override
        public long currentWatermark() {
            return internalTimerService.currentWatermark();
        }

        @Override
        public ZoneId getShiftTimeZone() {
            return shiftTimeZone;
        }

        @Override
        public RowData getWindowAccumulators(W window) throws Exception {
            windowState.setCurrentNamespace(window);
            return windowState.value();
        }

        @Override
        public void setWindowAccumulators(W window, RowData acc) throws Exception {
            windowState.setCurrentNamespace(window);
            windowState.update(acc);
        }

        @Override
        public void clearWindowState(W window) throws Exception {
            windowState.setCurrentNamespace(window);
            windowState.clear();
            windowAggregator.cleanup(window);
        }

        @Override
        public void clearPreviousState(W window) throws Exception {
            if (previousState != null) {
                previousState.setCurrentNamespace(window);
                previousState.clear();
            }
        }

        @Override
        public void clearTrigger(W window) throws Exception {
            triggerContext.window = window;
            triggerContext.clear();
        }

        @Override
        public void deleteCleanupTimer(W window) throws Exception {
            long cleanupTime = toEpochMillsForTimer(cleanupTime(window), shiftTimeZone);
            if (cleanupTime == Long.MAX_VALUE) {
                // no need to clean up because we didn't set one
                return;
            }
            if (windowAssigner.isEventTime()) {
                triggerContext.deleteEventTimeTimer(cleanupTime);
            } else {
                triggerContext.deleteProcessingTimeTimer(cleanupTime);
            }
        }

        @Override
        public void onMerge(W newWindow, Collection<W> mergedWindows) throws Exception {
            triggerContext.window = newWindow;
            triggerContext.mergedWindows = mergedWindows;
            triggerContext.onMerge();
        }
    }

    /**
     * {@code TriggerContext} is a utility for handling {@code Trigger} invocations. It can be
     * reused by setting the {@code key} and {@code window} fields. No internal state must be kept
     * in the {@code TriggerContext}
     */
    private class TriggerContext implements Trigger.OnMergeContext {

        private W window;
        private Collection<W> mergedWindows;

        public void open() throws Exception {
            trigger.open(this);
        }

        boolean onElement(RowData row, long timestamp) throws Exception {
            return trigger.onElement(row, timestamp, window);
        }

        boolean onProcessingTime(long time) throws Exception {
            return trigger.onProcessingTime(time, window);
        }

        boolean onEventTime(long time) throws Exception {
            return trigger.onEventTime(time, window);
        }

        void onMerge() throws Exception {
            trigger.onMerge(window, this);
        }

        @Override
        public long getCurrentProcessingTime() {
            return internalTimerService.currentProcessingTime();
        }

        @Override
        public long getCurrentWatermark() {
            return internalTimerService.currentWatermark();
        }

        @Override
        public MetricGroup getMetricGroup() {
            return WindowOperator.this.getMetricGroup();
        }

        @Override
        public void registerProcessingTimeTimer(long time) {
            internalTimerService.registerProcessingTimeTimer(window, time);
        }

        @Override
        public void registerEventTimeTimer(long time) {
            internalTimerService.registerEventTimeTimer(window, time);
        }

        @Override
        public void deleteProcessingTimeTimer(long time) {
            internalTimerService.deleteProcessingTimeTimer(window, time);
        }

        @Override
        public void deleteEventTimeTimer(long time) {
            internalTimerService.deleteEventTimeTimer(window, time);
        }

        @Override
        public ZoneId getShiftTimeZone() {
            return shiftTimeZone;
        }

        public void clear() throws Exception {
            trigger.clear(window);
        }

        @Override
        public <S extends State> S getPartitionedState(StateDescriptor<S, ?> stateDescriptor) {
            try {
                return WindowOperator.this.getPartitionedState(
                        window, windowSerializer, stateDescriptor);
            } catch (Exception e) {
                throw new RuntimeException("Could not retrieve state", e);
            }
        }

        @Override
        public <S extends MergingState<?, ?>> void mergePartitionedState(
                StateDescriptor<S, ?> stateDescriptor) {
            if (mergedWindows != null && mergedWindows.size() > 0) {
                try {
                    State state =
                            WindowOperator.this.getOrCreateKeyedState(
                                    windowSerializer, stateDescriptor);
                    if (state instanceof InternalMergingState) {
                        ((InternalMergingState<K, W, ?, ?, ?>) state)
                                .mergeNamespaces(window, mergedWindows);
                    } else {
                        throw new IllegalArgumentException(
                                "The given state descriptor does not refer to a mergeable state (MergingState)");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error while merging state.", e);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------
    // Visible For Testing
    // ------------------------------------------------------------------------------

    protected Counter getNumLateRecordsDropped() {
        return numLateRecordsDropped;
    }

    protected Gauge<Long> getWatermarkLatency() {
        return watermarkLatency;
    }
}
