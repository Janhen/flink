/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.watermark;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.streaming.runtime.streamrecord.StreamElement;

/**
 * 一个时间戳，告知算子所有事件早于等于 Watermark 的事件或记录都已经到达，不会再有比 watermark 更早的记录
 * 苏州奶可以根据 watermark 触发窗口的计算、清理资源
 *
 * Watermark告诉操作符，时间戳比水印时间戳早或等于水印时间戳的元素不应该到达操作符。水印从源发出，并通过拓扑的运算符传播。
 * 操作符必须自己使用{@link org.apache.flink. api.operator.outputemitwatermark (Watermark)}向下游操作符发出水印。
 * 没有在内部缓冲元素的操作符总是可以转发它们接收到的水印。缓冲元素的操作符，如窗口操作符，必须在被到达的水印触发的元素发出后转发一个水印。
 * <p>在某些情况下，水印只是一种启发式，操作符应该能够处理后期元素。它们可以丢弃这些结果，也可以更新结果并向下游操作发出updatesretractions。
 * <p>当一个源关闭时，它将发出一个带有时间戳{@code Long.MAX_VALUE}的最终水印。当操作符接收到这个信息时，它将知道以后不会有更多的输入到达。
 *
 * A Watermark tells operators that no elements with a timestamp older or equal to the watermark
 * timestamp should arrive at the operator. Watermarks are emitted at the sources and propagate
 * through the operators of the topology. Operators must themselves emit watermarks to downstream
 * operators using {@link org.apache.flink.streaming.api.operators.Output#emitWatermark(Watermark)}.
 * Operators that do not internally buffer elements can always forward the watermark that they
 * receive. Operators that buffer elements, such as window operators, must forward a watermark after
 * emission of elements that is triggered by the arriving watermark.
 *
 * <p>In some cases a watermark is only a heuristic and operators should be able to deal with late
 * elements. They can either discard those or update the result and emit updates/retractions to
 * downstream operations.
 *
 * <p>When a source closes it will emit a final watermark with timestamp {@code Long.MAX_VALUE}.
 * When an operator receives this it will know that no more input will be arriving in the future.
 */
@PublicEvolving
public final class Watermark extends StreamElement {

    /** The watermark that signifies end-of-event-time. */
    public static final Watermark MAX_WATERMARK = new Watermark(Long.MAX_VALUE);

    // ------------------------------------------------------------------------

    /** The timestamp of the watermark in milliseconds. */
    private final long timestamp;

    /** Creates a new watermark with the given timestamp in milliseconds. */
    public Watermark(long timestamp) {
        this.timestamp = timestamp;
    }

    /** Returns the timestamp associated with this {@link Watermark} in milliseconds. */
    public long getTimestamp() {
        return timestamp;
    }

    // ------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        return this == o
                || o != null
                        && o.getClass() == Watermark.class
                        && ((Watermark) o).timestamp == this.timestamp;
    }

    @Override
    public int hashCode() {
        return (int) (timestamp ^ (timestamp >>> 32));
    }

    @Override
    public String toString() {
        return "Watermark @ " + timestamp;
    }
}
