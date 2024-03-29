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

package org.apache.flink.runtime.metrics;

import org.apache.flink.metrics.Metric;
import org.apache.flink.runtime.metrics.dump.MetricQueryService;
import org.apache.flink.runtime.metrics.groups.AbstractMetricGroup;
import org.apache.flink.runtime.metrics.scope.ScopeFormats;

import javax.annotation.Nullable;

/** Interface for a metric registry. */
// 用于度量注册中心的接口
public interface MetricRegistry {

    /**
     * Returns the global delimiter.
     *
     * @return global delimiter
     */
    char getDelimiter();

    /** Returns the number of registered reporters. */
    int getNumberReporters();

    /**
     * Registers a new {@link Metric} with this registry.
     *
     * @param metric the metric that was added
     * @param metricName the name of the metric
     * @param group the group that contains the metric
     */
    void register(Metric metric, String metricName, AbstractMetricGroup group);

    /**
     * Un-registers the given {@link Metric} with this registry.
     *
     * @param metric the metric that should be removed
     * @param metricName the name of the metric
     * @param group the group that contains the metric
     */
    void unregister(Metric metric, String metricName, AbstractMetricGroup group);

    /**
     * Returns the scope formats.
     *
     * @return scope formats
     */
    ScopeFormats getScopeFormats();

    /**
     * 返回{@link MetricQueryService}的网关，如果没有启动，则返回null。
     *
     * Returns the gateway of the {@link MetricQueryService} or null, if none is started.
     *
     * @return Gateway of the MetricQueryService or null, if none is started
     */
    @Nullable
    default String getMetricQueryServiceGatewayRpcAddress() {
        return null;
    }
}
