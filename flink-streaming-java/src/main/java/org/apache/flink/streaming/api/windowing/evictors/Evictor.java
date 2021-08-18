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

package org.apache.flink.streaming.api.windowing.evictors;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.api.windowing.windows.Window;
import org.apache.flink.streaming.runtime.operators.windowing.TimestampedValue;

import java.io.Serializable;

/**
 * {@code Evictor} 可以在 WindowFunction 评估之后和窗口评估被
 * {@link org.apache.flink.streaming.api.windowing.triggers.Trigger} 触发之后从窗格中删除元素
 *
 * <p>窗格是具有相同键（由 {@link org.apache.flink.api.java.functions.KeySelector} 分配）和相同
 *   {@link Window} 的元素的桶。一个元素可以在它的多个窗格中被
 *   {@link org.apache.flink.streaming.api.windowing.assigners.WindowAssigner} 分配给多个窗口。这些窗格
 *   都有自己的 {@code Evictor} 实例。
 *
 * An {@code Evictor} can remove elements from a pane before/after the evaluation of WindowFunction
 * and after the window evaluation gets triggered by a {@link
 * org.apache.flink.streaming.api.windowing.triggers.Trigger}
 *
 * <p>A pane is the bucket of elements that have the same key (assigned by the {@link
 * org.apache.flink.api.java.functions.KeySelector}) and same {@link Window}. An element can be in
 * multiple panes of it was assigned to multiple windows by the {@link
 * org.apache.flink.streaming.api.windowing.assigners.WindowAssigner}. These panes all have their
 * own instance of the {@code Evictor}.
 *
 * @param <T> The type of elements that this {@code Evictor} can evict.
 * @param <W> The type of {@link Window Windows} on which this {@code Evictor} can operate.
 */
@PublicEvolving
public interface Evictor<T, W extends Window> extends Serializable {

    /**
     * 可选地驱逐元素。在窗口函数之前调用。
     *
     * Optionally evicts elements. Called before windowing function.
     *
     * @param elements The elements currently in the pane.
     * @param size The current number of elements in the pane.
     * @param window The {@link Window}
     * @param evictorContext The context for the Evictor
     */
    void evictBefore(
            Iterable<TimestampedValue<T>> elements,
            int size,
            W window,
            EvictorContext evictorContext);

    /**
     * 可选地驱逐元素。在窗口函数之后调用。
     *
     * Optionally evicts elements. Called after windowing function.
     *
     * @param elements The elements currently in the pane.
     * @param size The current number of elements in the pane.
     * @param window The {@link Window}
     * @param evictorContext The context for the Evictor
     */
    void evictAfter(
            Iterable<TimestampedValue<T>> elements,
            int size,
            W window,
            EvictorContext evictorContext);

    /** A context object that is given to {@link Evictor} methods. */
    // 提供给 {@link Evictor} 方法的上下文对象。
    interface EvictorContext {

        /** Returns the current processing time. */
        // 返回当前处理时间。
        long getCurrentProcessingTime();

        /**
         * 返回此 {@link Evictor} 的指标组。这与从用户函数中的 {@link RuntimeContext#getMetricGroup()}
         * 返回的指标组相同。
         *
         * <p>不能多次调用创建度量对象的方法（例如 {@link MetricGroup#counter(int)}，而是调用一次并将度量
         *   对象存储在一个字段中。
         *
         * Returns the metric group for this {@link Evictor}. This is the same metric group that
         * would be returned from {@link RuntimeContext#getMetricGroup()} in a user function.
         *
         * <p>You must not call methods that create metric objects (such as {@link
         * MetricGroup#counter(int)} multiple times but instead call once and store the metric
         * object in a field.
         */
        MetricGroup getMetricGroup();

        /** Returns the current watermark time. */
        // 返回当前水印时间。
        long getCurrentWatermark();
    }
}
