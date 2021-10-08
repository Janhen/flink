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

package org.apache.flink.streaming.util;

import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.metrics.DescriptiveStatisticsHistogram;
import org.apache.flink.streaming.runtime.streamrecord.LatencyMarker;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link LatencyStats}对象用于跟踪和报告跨度量的延迟行为。
 *
 * The {@link LatencyStats} objects are used to track and report on the behavior of latencies across
 * measurements.
 */
public class LatencyStats {
    // 相关的直方图统计信息
    private final Map<String, DescriptiveStatisticsHistogram> latencyStats = new HashMap<>();
    private final MetricGroup metricGroup;
    // 历史大小
    private final int historySize;
    // 自任务的索引
    private final int subtaskIndex;
    // 算子 ID
    private final OperatorID operatorId;
    // 粒度
    private final Granularity granularity;

    public LatencyStats(
            MetricGroup metricGroup,
            int historySize,
            int subtaskIndex,
            OperatorID operatorID,
            Granularity granularity) {
        this.metricGroup = metricGroup;
        this.historySize = historySize;
        this.subtaskIndex = subtaskIndex;
        this.operatorId = operatorID;
        this.granularity = granularity;
    }

    public void reportLatency(LatencyMarker marker) {
        // 根据 marker，算子 id，子任务索引创建唯一的直方图名称
        final String uniqueName =
                granularity.createUniqueHistogramName(marker, operatorId, subtaskIndex);

        DescriptiveStatisticsHistogram latencyHistogram = this.latencyStats.get(uniqueName);
        if (latencyHistogram == null) {
            latencyHistogram = new DescriptiveStatisticsHistogram(this.historySize);
            this.latencyStats.put(uniqueName, latencyHistogram);
            granularity
                    .createSourceMetricGroups(metricGroup, marker, operatorId, subtaskIndex)
                    .addGroup("operator_id", String.valueOf(operatorId))
                    .addGroup("operator_subtask_index", String.valueOf(subtaskIndex))
                    .histogram("latency", latencyHistogram);
        }

        long now = System.currentTimeMillis();
        // 与 flink 处理时间相比，延迟的时间
        latencyHistogram.update(now - marker.getMarkedTime());
    }

    /** Granularity for latency metrics. */
    // 延迟度量的粒度
    public enum Granularity {
        // 单个并行度的?
        SINGLE {
            @Override
            String createUniqueHistogramName(
                    LatencyMarker marker, OperatorID operatorId, int operatorSubtaskIndex) {
                return String.valueOf(operatorId) + operatorSubtaskIndex;
            }

            @Override
            MetricGroup createSourceMetricGroups(
                    MetricGroup base,
                    LatencyMarker marker,
                    OperatorID operatorId,
                    int operatorSubtaskIndex) {
                return base;
            }
        },
        // 算子
        OPERATOR {
            @Override
            String createUniqueHistogramName(
                    LatencyMarker marker, OperatorID operatorId, int operatorSubtaskIndex) {
                return String.valueOf(marker.getOperatorId()) + operatorId + operatorSubtaskIndex;
            }

            @Override
            MetricGroup createSourceMetricGroups(
                    MetricGroup base,
                    LatencyMarker marker,
                    OperatorID operatorId,
                    int operatorSubtaskIndex) {
                return base.addGroup("source_id", String.valueOf(marker.getOperatorId()));
            }
        },
        // 子任务
        SUBTASK {
            @Override
            String createUniqueHistogramName(
                    LatencyMarker marker, OperatorID operatorId, int operatorSubtaskIndex) {
                return String.valueOf(marker.getOperatorId())
                        + marker.getSubtaskIndex()
                        + operatorId
                        + operatorSubtaskIndex;
            }

            @Override
            MetricGroup createSourceMetricGroups(
                    MetricGroup base,
                    LatencyMarker marker,
                    OperatorID operatorId,
                    int operatorSubtaskIndex) {
                return base.addGroup("source_id", String.valueOf(marker.getOperatorId()))
                        .addGroup("source_subtask_index", String.valueOf(marker.getSubtaskIndex()));
            }
        };

        abstract String createUniqueHistogramName(
                LatencyMarker marker, OperatorID operatorId, int operatorSubtaskIndex);

        abstract MetricGroup createSourceMetricGroups(
                MetricGroup base,
                LatencyMarker marker,
                OperatorID operatorId,
                int operatorSubtaskIndex);
    }
}
