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

package org.apache.flink.runtime.metrics;

import org.apache.flink.metrics.HistogramStatistics;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.SecondMoment;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.ranking.NaNStrategy;

import java.util.Arrays;

/**
 * {@link DescriptiveStatisticsHistogram} 返回的 DescriptiveStatistics 直方图统计实现。
 *
 * <p>统计信息获取 {@link DescriptiveStatistics} 实例的时间点快照，并允许从中检索优化的指标。
 *
 * DescriptiveStatistics histogram statistics implementation returned by {@link
 * DescriptiveStatisticsHistogram}.
 *
 * <p>The statistics takes a point-in-time snapshot of a {@link DescriptiveStatistics} instance and
 * allows optimised metrics retrieval from this.
 */
public class DescriptiveStatisticsHistogramStatistics extends HistogramStatistics {
    private final CommonMetricsSnapshot statisticsSummary = new CommonMetricsSnapshot();

    public DescriptiveStatisticsHistogramStatistics(
            DescriptiveStatisticsHistogram.CircularDoubleArray histogramValues) {
        statisticsSummary.evaluate(histogramValues.toUnsortedArray());
    }

    @Override
    public double getQuantile(double quantile) {
        return statisticsSummary.getPercentile(quantile * 100);
    }

    @Override
    public long[] getValues() {
        return Arrays.stream(statisticsSummary.getValues()).mapToLong(i -> (long) i).toArray();
    }

    @Override
    public int size() {
        return (int) statisticsSummary.getCount();
    }

    @Override
    public double getMean() {
        return statisticsSummary.getMean();
    }

    @Override
    public double getStdDev() {
        return statisticsSummary.getStandardDeviation();
    }

    @Override
    public long getMax() {
        return (long) statisticsSummary.getMax();
    }

    @Override
    public long getMin() {
        return (long) statisticsSummary.getMin();
    }

    /**
     * 以优化的方式提取几个常用指标的函数，即尽可能少地运行数据计算。
     *
     * <p>请注意，对 {@link #evaluate(double[])} 或 {@link #evaluate(double[], int, int)} 的调用不会
     *   返回值，而是填充此类，以便可以获取更多值从中检索。
     *
     * Function to extract several commonly used metrics in an optimised way, i.e. with as few runs
     * over the data / calculations as possible.
     *
     * <p>Note that calls to {@link #evaluate(double[])} or {@link #evaluate(double[], int, int)}
     * will not return a value but instead populate this class so that further values can be
     * retrieved from it.
     */
    private static class CommonMetricsSnapshot implements UnivariateStatistic {
        private long count = 0;
        private double min = Double.NaN;
        private double max = Double.NaN;
        private double mean = Double.NaN;
        private double stddev = Double.NaN;
        private Percentile percentilesImpl = new Percentile().withNaNStrategy(NaNStrategy.FIXED);

        @Override
        public double evaluate(final double[] values) throws MathIllegalArgumentException {
            return evaluate(values, 0, values.length);
        }

        @Override
        public double evaluate(double[] values, int begin, int length)
                throws MathIllegalArgumentException {
            this.count = length;
            percentilesImpl.setData(values, begin, length);

            SimpleStats secondMoment = new SimpleStats();
            secondMoment.evaluate(values, begin, length);
            this.mean = secondMoment.getMean();
            this.min = secondMoment.getMin();
            this.max = secondMoment.getMax();

            this.stddev = new StandardDeviation(secondMoment).getResult();

            return Double.NaN;
        }

        @Override
        public CommonMetricsSnapshot copy() {
            CommonMetricsSnapshot result = new CommonMetricsSnapshot();
            result.count = count;
            result.min = min;
            result.max = max;
            result.mean = mean;
            result.stddev = stddev;
            result.percentilesImpl = percentilesImpl.copy();
            return result;
        }

        long getCount() {
            return count;
        }

        double getMin() {
            return min;
        }

        double getMax() {
            return max;
        }

        double getMean() {
            return mean;
        }

        double getStandardDeviation() {
            return stddev;
        }

        double getPercentile(double p) {
            return percentilesImpl.evaluate(p);
        }

        double[] getValues() {
            return percentilesImpl.getData();
        }
    }

    /**
     * 计算最小值、最大值、平均值（第一时刻）以及一次遍历值数组的第二时刻。
     *
     * Calculates min, max, mean (first moment), as well as the second moment in one go over the
     * value array.
     */
    private static class SimpleStats extends SecondMoment {
        private static final long serialVersionUID = 1L;

        private double min = Double.NaN;
        private double max = Double.NaN;

        @Override
        public void increment(double d) {
            if (d < min || Double.isNaN(min)) {
                min = d;
            }
            if (d > max || Double.isNaN(max)) {
                max = d;
            }
            super.increment(d);
        }

        @Override
        public SecondMoment copy() {
            SimpleStats result = new SimpleStats();
            SecondMoment.copy(this, result);
            result.min = min;
            result.max = max;
            return result;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getMean() {
            return m1;
        }
    }
}
