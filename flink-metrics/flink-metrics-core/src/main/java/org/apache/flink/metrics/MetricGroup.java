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

package org.apache.flink.metrics;

import java.util.Map;

/**
 * MetricGroup 是 {@link Metric Metrics} 和更多指标子组的命名容器。
 *
 * <p>此类的实例可用于向 Flink 注册新指标并基于组名称创建嵌套层次结构。
 *
 * <p>MetricGroup 由它在层次结构中的位置和名称唯一标识。
 *
 * A MetricGroup is a named container for {@link Metric Metrics} and further metric subgroups.
 *
 * <p>Instances of this class can be used to register new metrics with Flink and to create a nested
 * hierarchy based on the group names.
 *
 * <p>A MetricGroup is uniquely identified by it's place in the hierarchy and name.
 */
public interface MetricGroup {

    // ------------------------------------------------------------------------
    //  Metrics
    // ------------------------------------------------------------------------

    /**
     * 创建并注册一个新的 {@link org.apache.flink.metrics.Counter} 与Flink。
     *
     * Creates and registers a new {@link org.apache.flink.metrics.Counter} with Flink.
     *
     * @param name name of the counter
     * @return the created counter
     */
    Counter counter(int name);

    /**
     * 使用 Flink 创建并注册一个新的 {@link org.apache.flink.metrics.Counter}。
     *
     * Creates and registers a new {@link org.apache.flink.metrics.Counter} with Flink.
     *
     * @param name name of the counter
     * @return the created counter
     */
    Counter counter(String name);

    /**
     * 向 Flink 注册一个 {@link org.apache.flink.metrics.Counter}。
     *
     * Registers a {@link org.apache.flink.metrics.Counter} with Flink.
     *
     * @param name name of the counter
     * @param counter counter to register
     * @param <C> counter type
     * @return the given counter
     */
    <C extends Counter> C counter(int name, C counter);

    /**
     * 向 Flink 注册一个 {@link org.apache.flink.metrics.Counter}。
     *
     * Registers a {@link org.apache.flink.metrics.Counter} with Flink.
     *
     * @param name name of the counter
     * @param counter counter to register
     * @param <C> counter type
     * @return the given counter
     */
    <C extends Counter> C counter(String name, C counter);

    /**
     * 向 Flink 注册一个新的 {@link org.apache.flink.metrics.Gauge}。
     *
     * Registers a new {@link org.apache.flink.metrics.Gauge} with Flink.
     *
     * @param name name of the gauge
     * @param gauge gauge to register
     * @param <T> return type of the gauge
     * @return the given gauge
     */
    <T, G extends Gauge<T>> G gauge(int name, G gauge);

    /**
     * 注册一个新的 {@link org.apache.flink.metrics.Gauge} 与 Flink。
     *
     * Registers a new {@link org.apache.flink.metrics.Gauge} with Flink.
     *
     * @param name name of the gauge
     * @param gauge gauge to register
     * @param <T> return type of the gauge
     * @return the given gauge
     */
    <T, G extends Gauge<T>> G gauge(String name, G gauge);

    /**
     * Registers a new {@link Histogram} with Flink.
     *
     * @param name name of the histogram
     * @param histogram histogram to register
     * @param <H> histogram type
     * @return the registered histogram
     */
    <H extends Histogram> H histogram(String name, H histogram);

    /**
     * Registers a new {@link Histogram} with Flink.
     *
     * @param name name of the histogram
     * @param histogram histogram to register
     * @param <H> histogram type
     * @return the registered histogram
     */
    <H extends Histogram> H histogram(int name, H histogram);

    /**
     * Registers a new {@link Meter} with Flink.
     *
     * @param name name of the meter
     * @param meter meter to register
     * @param <M> meter type
     * @return the registered meter
     */
    <M extends Meter> M meter(String name, M meter);

    /**
     * 向 Flink 注册一个新的 {@link Meter}。
     *
     * Registers a new {@link Meter} with Flink.
     *
     * @param name name of the meter
     * @param meter meter to register
     * @param <M> meter type
     * @return the registered meter
     */
    <M extends Meter> M meter(int name, M meter);

    // ------------------------------------------------------------------------
    // Groups
    // ------------------------------------------------------------------------

    /**
     * 创建一个新的 MetricGroup 并将其添加到此组子组中。
     *
     * Creates a new MetricGroup and adds it to this groups sub-groups.
     *
     * @param name name of the group
     * @return the created group
     */
    MetricGroup addGroup(int name);

    /**
     * 创建一个新的 MetricGroup 并将其添加到此组子组中。
     *
     * Creates a new MetricGroup and adds it to this groups sub-groups.
     *
     * @param name name of the group
     * @return the created group
     */
    MetricGroup addGroup(String name);

    /**
     * 创建一个新的键值 MetricGroup 对。键组被添加到这个组的子组中，而值组被添加到键组的子组中。此方法返回值组。
     *
     * <p>调用这个方法和 {@code group.addGroup(key).addGroup(value)} 的唯一区别是值组的
     * {@link #getAllVariables()} 返回一个额外的 {@code "<key>" ="value"} 对。
     *
     * Creates a new key-value MetricGroup pair. The key group is added to this groups sub-groups,
     * while the value group is added to the key group's sub-groups. This method returns the value
     * group.
     *
     * <p>The only difference between calling this method and {@code
     * group.addGroup(key).addGroup(value)} is that {@link #getAllVariables()} of the value group
     * return an additional {@code "<key>"="value"} pair.
     *
     * @param key name of the first group
     * @param value name of the second group
     * @return the second created group
     */
    MetricGroup addGroup(String key, String value);

    // ------------------------------------------------------------------------
    // Scope
    // ------------------------------------------------------------------------

    /**
     * 以范围组件的数组形式获取范围，例如 {@code ["host-7", "taskmanager-2", "window_word_count", "my-mapper"]}。
     *
     * Gets the scope as an array of the scope components, for example {@code ["host-7",
     * "taskmanager-2", "window_word_count", "my-mapper"]}.
     *
     * @see #getMetricIdentifier(String)
     * @see #getMetricIdentifier(String, CharacterFilter)
     */
    String[] getScopeComponents();

    /**
     * 返回所有变量及其关联值的映射，例如 {@code {"<host>"="host-7", "<tm_id>"="taskmanager-2"}}。
     *
     * Returns a map of all variables and their associated value, for example {@code
     * {"<host>"="host-7", "<tm_id>"="taskmanager-2"}}.
     *
     * @return map of all variables and their associated value
     */
    Map<String, String> getAllVariables();

    /**
     * 返回完全限定的指标名称，例如 {@code "host-7.task manager-2.window word_count.my-mapper.metric Name"}。
     *
     * Returns the fully qualified metric name, for example {@code
     * "host-7.taskmanager-2.window_word_count.my-mapper.metricName"}.
     *
     * @param metricName metric name
     * @return fully qualified metric name
     */
    String getMetricIdentifier(String metricName);

    /**
     * Returns the fully qualified metric name, for example {@code
     * "host-7.taskmanager-2.window_word_count.my-mapper.metricName"}.
     *
     * @param metricName metric name
     * @param filter character filter which is applied to the scope components if not null.
     * @return fully qualified metric name
     */
    String getMetricIdentifier(String metricName, CharacterFilter filter);
}
