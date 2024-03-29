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

package org.apache.flink.streaming.runtime.tasks;

import org.apache.flink.metrics.Gauge;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.metrics.WatermarkGauge;
import org.apache.flink.streaming.runtime.streamrecord.LatencyMarker;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.streamstatus.StreamStatusProvider;
import org.apache.flink.util.OutputTag;
import org.apache.flink.util.XORShiftRandom;

import java.util.Random;

// 广播输出收集器
class BroadcastingOutputCollector<T> implements WatermarkGaugeExposingOutput<StreamRecord<T>> {

    // J: 下游所有的 ...
    protected final Output<StreamRecord<T>>[] outputs;
    private final Random random = new XORShiftRandom();
    private final StreamStatusProvider streamStatusProvider;
    private final WatermarkGauge watermarkGauge = new WatermarkGauge();

    public BroadcastingOutputCollector(
            Output<StreamRecord<T>>[] outputs, StreamStatusProvider streamStatusProvider) {
        this.outputs = outputs;
        this.streamStatusProvider = streamStatusProvider;
    }

    @Override
    public void emitWatermark(Watermark mark) {
        watermarkGauge.setCurrentWatermark(mark.getTimestamp());
        // 确定流状态为激活的，向保存的所有 output 写入水印
        if (streamStatusProvider.getStreamStatus().isActive()) {
            for (Output<StreamRecord<T>> output : outputs) {
                output.emitWatermark(mark);
            }
        }
    }

    @Override
    public void emitLatencyMarker(LatencyMarker latencyMarker) {
        if (outputs.length <= 0) {
            // ignore
        } else if (outputs.length == 1) {
            outputs[0].emitLatencyMarker(latencyMarker);
        } else {
            // randomly select an output
            // 对于延迟的，随机选择一个输出
            outputs[random.nextInt(outputs.length)].emitLatencyMarker(latencyMarker);
        }
    }

    @Override
    public Gauge<Long> getWatermarkGauge() {
        return watermarkGauge;
    }

    @Override
    public void collect(StreamRecord<T> record) {
        for (Output<StreamRecord<T>> output : outputs) {
            output.collect(record);
        }
    }

    @Override
    public <X> void collect(OutputTag<X> outputTag, StreamRecord<X> record) {
        // 输出到指定的测出流...
        for (Output<StreamRecord<T>> output : outputs) {
            output.collect(outputTag, record);
        }
    }

    @Override
    public void close() {
        for (Output<StreamRecord<T>> output : outputs) {
            output.close();
        }
    }
}
