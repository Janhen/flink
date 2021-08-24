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

/**
 * 与 Flink 的度量系统一起使用的直方图接口。
 *
 * <p>直方图允许记录值、获取记录值的当前计数并为当前看到的元素创建直方图统计信息。
 *
 * Histogram interface to be used with Flink's metrics system.
 *
 * <p>The histogram allows to record values, get the current count of recorded values and create
 * histogram statistics for the currently seen elements.
 */
public interface Histogram extends Metric {

    /**
     * 使用给定值更新直方图。
     *
     * Update the histogram with the given value.
     *
     * @param value Value to update the histogram with
     */
    void update(long value);

    /**
     * 获取所见元素的计数。
     *
     * Get the count of seen elements.
     *
     * @return Count of seen elements
     */
    long getCount();

    /**
     * 为当前记录的元素创建统计信息。
     *
     * Create statistics for the currently recorded elements.
     *
     * @return Statistics about the currently recorded elements
     */
    HistogramStatistics getStatistics();
}
