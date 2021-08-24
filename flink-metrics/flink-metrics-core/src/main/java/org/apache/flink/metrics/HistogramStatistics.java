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
 * 直方图统计数据表示直方图中记录的元素的当前快照。
 *
 * <p>直方图统计允许计算分位数、平均值、标准偏差、最小值和最大值的值。
 *
 * Histogram statistics represent the current snapshot of elements recorded in the histogram.
 *
 * <p>The histogram statistics allow to calculate values for quantiles, the mean, the standard
 * deviation, the minimum and the maximum.
 */
public abstract class HistogramStatistics {

    /**
     * 根据表示的直方图统计返回给定分位数的值。
     *
     * <p>J: p50,p75,p95,p98,p99,p999
     *
     * Returns the value for the given quantile based on the represented histogram statistics.
     *
     * @param quantile Quantile to calculate the value for
     * @return Value for the given quantile
     */
    public abstract double getQuantile(double quantile);

    /**
     * 返回统计样本的元素。
     *
     * Returns the elements of the statistics' sample.
     *
     * @return Elements of the statistics' sample
     */
    public abstract long[] getValues();

    /**
     * 返回统计样本的大小。
     *
     * Returns the size of the statistics' sample.
     *
     * @return Size of the statistics' sample
     */
    public abstract int size();

    /**
     * 返回直方图值的平均值。
     *
     * Returns the mean of the histogram values.
     *
     * @return Mean of the histogram values
     */
    public abstract double getMean();

    /**
     * 返回由直方图统计反映的分布的标准偏差。
     *
     * Returns the standard deviation of the distribution reflected by the histogram statistics.
     *
     * @return Standard deviation of histogram distribution
     */
    public abstract double getStdDev();

    /**
     * 返回直方图的最大值。
     *
     * Returns the maximum value of the histogram.
     *
     * @return Maximum value of the histogram
     */
    public abstract long getMax();

    /**
     * 返回直方图的最小值。
     *
     * Returns the minimum value of the histogram.
     *
     * @return Minimum value of the histogram
     */
    public abstract long getMin();
}
