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

package org.apache.flink.metrics.reporter;

import org.apache.flink.metrics.Metric;
import org.apache.flink.metrics.MetricConfig;
import org.apache.flink.metrics.MetricGroup;

/**
 * 报告器用于将 {@link Metric Metrics} 导出到外部后端。
 *
 * <p>Reporters 可以通过
 *    a) 通过反射来实例化，在这种情况下，它们必须是公共的、非抽象的，并且具有公共的无参数构造函数。
 *    b) 通过 {@link MetricReporterFactory}，在这种情况下不适用任何限制。 （受到推崇的）
 *
 * <p>既不要求也不鼓励报告者支持两种实例化路径。
 *
 * Reporters are used to export {@link Metric Metrics} to an external backend.
 *
 * <p>Reporters are instantiated either a) via reflection, in which case they must be public,
 * non-abstract, and have a public no-argument constructor. b) via a {@link MetricReporterFactory},
 * in which case no restrictions apply. (recommended)
 *
 * <p>Reporters are neither required nor encouraged to support both instantiation paths.
 */
public interface MetricReporter {

    // ------------------------------------------------------------------------
    //  life cycle
    // ------------------------------------------------------------------------

    /**
     * 配置此报告器。
     *
     * <p>如果报告器是通用实例化的，因此没有参数，则此方法是报告器根据配置值设置其基本字段的地方。否则，此方法通常是
     *    空操作，因为可以在构造函数中获取资源。
     *
     * Configures this reporter.
     *
     * <p>If the reporter was instantiated generically and hence parameter-less, this method is the
     * place where the reporter sets it's basic fields based on configuration values. Otherwise,
     * this method will typically be a no-op since resources can be acquired in the constructor.
     *
     * <p>This method is always called first on a newly instantiated reporter.
     *
     * @param config A properties object that contains all parameters set for this reporter.
     */
    void open(MetricConfig config);

    /** Closes this reporter. Should be used to close channels, streams and release resources. */
    void close();

    // ------------------------------------------------------------------------
    //  adding / removing metrics
    // ------------------------------------------------------------------------

    /**
     * 添加新的 {@link Metric} 时调用。
     *
     * Called when a new {@link Metric} was added.
     *
     * @param metric the metric that was added
     * @param metricName the name of the metric
     * @param group the group that contains the metric
     */
    void notifyOfAddedMetric(Metric metric, String metricName, MetricGroup group);

    /**
     * 在删除 {@link Metric} 时调用。
     *
     * Called when a {@link Metric} was removed.
     *
     * @param metric the metric that should be removed
     * @param metricName the name of the metric
     * @param group the group that contains the metric
     */
    void notifyOfRemovedMetric(Metric metric, String metricName, MetricGroup group);
}
