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

package org.apache.flink.table.runtime.operators.wmassigners;

import org.apache.flink.metrics.Gauge;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeCallback;
import org.apache.flink.table.data.RowData;

/**
 * 在给定时间段内发出小批量标记的流运算符。这个小批量分配器在处理时间内工作，这意味着小批量标记是在给定的时间段内使用
 * 处理时间生成的。一旦收到的 mini-batch id 前进，下游运营商将触发 mini-batch。
 *
 * <p>注意：目前，我们使用{@link Watermark} 来表示小批量标记。
 *
 * <p>该算子与{@link RowTimeMiniBatchAssginerOperator}的区别在于，该算子利用处理时间自行生成水印，而另一个算子
 *   从上游转发水印。
 *
 * A stream operator that emits mini-batch marker in a given period. This mini-batch assigner works
 * in processing time, which means the mini-batch marker is generated in the given period using the
 * processing time. The downstream operators will trigger mini-batch once the received mini-batch id
 * advanced.
 *
 * <p>NOTE: currently, we use {@link Watermark} to represents the mini-batch marker.
 *
 * <p>The difference between this operator and {@link RowTimeMiniBatchAssginerOperator} is that,
 * this operator generates watermarks by itself using processing time, but the other forwards
 * watermarks from upstream.
 */
public class ProcTimeMiniBatchAssignerOperator extends AbstractStreamOperator<RowData>
        implements OneInputStreamOperator<RowData, RowData>, ProcessingTimeCallback {

    private static final long serialVersionUID = 1L;

    private final long intervalMs;

    private transient long currentWatermark;

    public ProcTimeMiniBatchAssignerOperator(long intervalMs) {
        this.intervalMs = intervalMs;
        this.chainingStrategy = ChainingStrategy.ALWAYS;
    }

    @Override
    public void open() throws Exception {
        super.open();

        currentWatermark = 0;

        long now = getProcessingTimeService().getCurrentProcessingTime();
        getProcessingTimeService().registerTimer(now + intervalMs, this);

        // report marker metric
        getRuntimeContext()
                .getMetricGroup()
                .gauge("currentBatch", (Gauge<Long>) () -> currentWatermark);
    }

    @Override
    public void processElement(StreamRecord<RowData> element) throws Exception {
        long now = getProcessingTimeService().getCurrentProcessingTime();
        long currentBatch = now - now % intervalMs;
        if (currentBatch > currentWatermark) {
            currentWatermark = currentBatch;
            // emit
            output.emitWatermark(new Watermark(currentBatch));
        }
        output.collect(element);
    }

    @Override
    public void onProcessingTime(long timestamp) throws Exception {
        long now = getProcessingTimeService().getCurrentProcessingTime();
        long currentBatch = now - now % intervalMs;
        if (currentBatch > currentWatermark) {
            currentWatermark = currentBatch;
            // emit
            output.emitWatermark(new Watermark(currentBatch));
        }
        getProcessingTimeService().registerTimer(currentBatch + intervalMs, this);
    }

    /**
     * Override the base implementation to completely ignore watermarks propagated from upstream (we
     * rely only on the {@link AssignerWithPeriodicWatermarks} to emit watermarks from here).
     */
    @Override
    public void processWatermark(Watermark mark) throws Exception {
        // if we receive a Long.MAX_VALUE watermark we forward it since it is used
        // to signal the end of input and to not block watermark progress downstream
        if (mark.getTimestamp() == Long.MAX_VALUE && currentWatermark != Long.MAX_VALUE) {
            currentWatermark = Long.MAX_VALUE;
            output.emitWatermark(mark);
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
    }
}
