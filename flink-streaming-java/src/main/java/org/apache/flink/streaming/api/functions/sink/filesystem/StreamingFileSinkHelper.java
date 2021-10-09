/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.functions.sink.filesystem;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.OperatorStateStore;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.api.common.typeutils.base.array.BytePrimitiveArraySerializer;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.operators.StreamOperator;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeCallback;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;

import javax.annotation.Nullable;

/**
 * 帮助{@link StreamingFileSink}。这个helper可以被{@link RichSinkFunction}或{@link StreamOperator}使用。
 *
 * Helper for {@link StreamingFileSink}. This helper can be used by {@link RichSinkFunction} or
 * {@link StreamOperator}.
 */
@Internal
public class StreamingFileSinkHelper<IN> implements ProcessingTimeCallback {

    // -------------------------- state descriptors ---------------------------

    // bucket 桶信息保存，实际准备批量写入的数据？
    private static final ListStateDescriptor<byte[]> BUCKET_STATE_DESC =
            new ListStateDescriptor<>("bucket-states", BytePrimitiveArraySerializer.INSTANCE);

    // 存放计数
    private static final ListStateDescriptor<Long> MAX_PART_COUNTER_STATE_DESC =
            new ListStateDescriptor<>("max-part-counter", LongSerializer.INSTANCE);

    // --------------------------- fields -----------------------------

    // 桶间隔时间，对于 process time 处理，非使用窗口聚合做批处理?
    private final long bucketCheckInterval;

    // 处理时间服务
    private final ProcessingTimeService procTimeService;

    // 写入的桶
    private final Buckets<IN, ?> buckets;

    // bucket 缓存的数据
    private final ListState<byte[]> bucketStates;

    // 最大分区计数
    private final ListState<Long> maxPartCountersState;

    //
    public StreamingFileSinkHelper(
            Buckets<IN, ?> buckets,
            // 用于确定是否是数据故障恢复使用
            boolean isRestored,
            // operator state store 用于处理 list state
            OperatorStateStore stateStore,
            ProcessingTimeService procTimeService,
            // 确定多长时间一批处理?
            long bucketCheckInterval)
            throws Exception {
        this.bucketCheckInterval = bucketCheckInterval;
        this.buckets = buckets;
        // 根据外部 richSinkFunction 获得到 state store 取出状态
        this.bucketStates = stateStore.getListState(BUCKET_STATE_DESC);
        // 获得的是 union list state
        // 操作符状态分区在恢复并发送到所有任务时被联合
        this.maxPartCountersState = stateStore.getUnionListState(MAX_PART_COUNTER_STATE_DESC);
        this.procTimeService = procTimeService;

        if (isRestored) {
            // 恢复处理逻辑
            buckets.initializeState(bucketStates, maxPartCountersState);
        }

        // 注册定时器处理，对应在当前处理时间 + 自定义的时间间隔
        long currentProcessingTime = procTimeService.getCurrentProcessingTime();
        procTimeService.registerTimer(currentProcessingTime + bucketCheckInterval, this);
    }

    public void commitUpToCheckpoint(long checkpointId) throws Exception {
        buckets.commitUpToCheckpoint(checkpointId);
    }

    public void snapshotState(long checkpointId) throws Exception {
        buckets.snapshotState(checkpointId, bucketStates, maxPartCountersState);
    }

    @Override
    public void onProcessingTime(long timestamp) throws Exception {
        // 获得 ...
        final long currentTime = procTimeService.getCurrentProcessingTime();
        // 对应桶根据当前时间处理
        buckets.onProcessingTime(currentTime);
        // 将元素放到 ...
        procTimeService.registerTimer(currentTime + bucketCheckInterval, this);
    }

    // 在 StreamingFileSink 的 invoke 处调用，进行相关的处理
    public void onElement(
            IN value,
            long currentProcessingTime,
            @Nullable Long elementTimestamp,
            long currentWatermark)
            throws Exception {
        buckets.onElement(value, currentProcessingTime, elementTimestamp, currentWatermark);
    }

    public void close() {
        // 相关的所有桶进行关闭
        this.buckets.close();
    }
}
