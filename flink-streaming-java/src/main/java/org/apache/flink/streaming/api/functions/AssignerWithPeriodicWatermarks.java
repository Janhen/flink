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

package org.apache.flink.streaming.api.functions;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.streaming.api.watermark.Watermark;

import javax.annotation.Nullable;

/**
 * {@code AssignerWithPeriodicWatermarks}将事件时间戳分配给元素，并生成低水位标记来表示流中的事件时间进度。对事件
 * 时间(例如事件时间窗口)进行操作的函数和操作符使用这些时间戳和水印。
 *
 * <p>用于按周期间隔生成水印。在大多数{@code i}毫秒(通过{@link ExecutionConfig#getAutoWatermarkInterval()}配
 * 置)，系统将调用{@link #getCurrentWatermark()}方法来探测下一个水印值。如果探测到的值为非空，且时间戳比前一个水印
 * 的时间戳大，系统将生成一个新的水印(以保持水印升序的一致性)。
 *
 * <p>系统调用{@link #getCurrentWatermark()}方法的频率可能低于{@code i}每毫秒一次，如果上次调用该方法后没有新元
 * 素到达的话。
 *
 * <p>时间戳和水印被定义为{@code longs}，表示从Epoch (midnight, January 1, 1970 UTC)开始的毫秒数。具有特定值
 * {@code t}的水印表示不再出现具有事件时间戳{@code x}(其中{@code x}小于或等于{@code t})的元素。
 *
 * The {@code AssignerWithPeriodicWatermarks} assigns event time timestamps to elements, and
 * generates low watermarks that signal event time progress within the stream. These timestamps and
 * watermarks are used by functions and operators that operate on event time, for example event time
 * windows.
 *
 * <p>Use this class to generate watermarks in a periodical interval. At most every {@code i}
 * milliseconds (configured via {@link ExecutionConfig#getAutoWatermarkInterval()}), the system will
 * call the {@link #getCurrentWatermark()} method to probe for the next watermark value. The system
 * will generate a new watermark, if the probed value is non-null and has a timestamp larger than
 * that of the previous watermark (to preserve the contract of ascending watermarks).
 *
 * <p>The system may call the {@link #getCurrentWatermark()} method less often than every {@code i}
 * milliseconds, if no new elements arrived since the last call to the method.
 *
 * <p>Timestamps and watermarks are defined as {@code longs} that represent the milliseconds since
 * the Epoch (midnight, January 1, 1970 UTC). A watermark with a certain value {@code t} indicates
 * that no elements with event timestamps {@code x}, where {@code x} is lower or equal to {@code t},
 * will occur any more.
 *
 * @param <T> The type of the elements to which this assigner assigns timestamps.
 * @see org.apache.flink.streaming.api.watermark.Watermark
 */
@Deprecated
public interface AssignerWithPeriodicWatermarks<T> extends TimestampAssigner<T> {

    /**
     * Returns the current watermark. This method is periodically called by the system to retrieve
     * the current watermark. The method may return {@code null} to indicate that no new Watermark
     * is available.
     *
     * <p>The returned watermark will be emitted only if it is non-null and its timestamp is larger
     * than that of the previously emitted watermark (to preserve the contract of ascending
     * watermarks). If the current watermark is still identical to the previous one, no progress in
     * event time has happened since the previous call to this method. If a null value is returned,
     * or the timestamp of the returned watermark is smaller than that of the last emitted one, then
     * no new watermark will be generated.
     *
     * <p>The interval in which this method is called and Watermarks are generated depends on {@link
     * ExecutionConfig#getAutoWatermarkInterval()}.
     *
     * @see org.apache.flink.streaming.api.watermark.Watermark
     * @see ExecutionConfig#getAutoWatermarkInterval()
     * @return {@code Null}, if no watermark should be emitted, or the next watermark to emit.
     */
    @Nullable
    Watermark getCurrentWatermark();
}
