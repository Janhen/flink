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

import org.apache.flink.streaming.api.watermark.Watermark;

import javax.annotation.Nullable;

/**
 * {@code AssignerWithPunctuatedWatermarks} 将事件时间戳分配给元素，并生成低水位标记来指示流中的事件时间进度。
 * 这些时间戳和水印由操作事件时间的函数和操作符使用，例如事件时间窗口。
 *
 * <p>如果某些特殊元素用作标志事件时间进展的标记，以及当您想在特定事件上发出水印时，请使用这个类。如果探测值是非空且
 * 时间戳大于前一个水印的时间戳，系统将生成一个新的水印(以保持升序水印的契约)。
 *
 * <p>对于应该根据元素时间戳周期性地发出水印的用例，使用 {@link AssignerWithPeriodicWatermarks} 代替。
 *
 * <p> 下面的示例演示了如何使用此时间戳提取器和水印生成器。它假设元素携带一个时间戳，用来描述它们被创建的时间，并且
 * 一些元素带有一个标志，将它们标记为序列的结束，这样具有更小时间戳的元素就不会再出现了。
 *
 * The {@code AssignerWithPunctuatedWatermarks} assigns event time timestamps to elements, and
 * generates low watermarks that signal event time progress within the stream. These timestamps and
 * watermarks are used by functions and operators that operate on event time, for example event time
 * windows.
 *
 * <p>Use this class if certain special elements act as markers that signify event time progress,
 * and when you want to emit watermarks specifically at certain events. The system will generate a
 * new watermark, if the probed value is non-null and has a timestamp larger than that of the
 * previous watermark (to preserve the contract of ascending watermarks).
 *
 * <p>For use cases that should periodically emit watermarks based on element timestamps, use the
 * {@link AssignerWithPeriodicWatermarks} instead.
 *
 * <p>The following example illustrates how to use this timestamp extractor and watermark generator.
 * It assumes elements carry a timestamp that describes when they were created, and that some
 * elements carry a flag, marking them as the end of a sequence such that no elements with smaller
 * timestamps can come anymore.
 *
 * <pre>{@code
 * public class WatermarkOnFlagAssigner implements AssignerWithPunctuatedWatermarks<MyElement> {
 *
 *     public long extractTimestamp(MyElement element, long previousElementTimestamp) {
 *         return element.getSequenceTimestamp();
 *     }
 *
 *     public Watermark checkAndGetNextWatermark(MyElement lastElement, long extractedTimestamp) {
 *         return lastElement.isEndOfSequence() ? new Watermark(extractedTimestamp) : null;
 *     }
 * }
 * }</pre>
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
public interface AssignerWithPunctuatedWatermarks<T> extends TimestampAssigner<T> {

    /**
     * 询问该实现是否要发出水印。这个方法在 {@link #extractTimestamp(Object, long)} 方法之后被调用。
     *
     * Asks this implementation if it wants to emit a watermark. This method is called right after
     * the {@link #extractTimestamp(Object, long)} method.
     *
     * <p>The returned watermark will be emitted only if it is non-null and its timestamp is larger
     * than that of the previously emitted watermark (to preserve the contract of ascending
     * watermarks). If a null value is returned, or the timestamp of the returned watermark is
     * smaller than that of the last emitted one, then no new watermark will be generated.
     *
     * <p>For an example how to use this method, see the documentation of {@link
     * AssignerWithPunctuatedWatermarks this class}.
     *
     * @return {@code Null}, if no watermark should be emitted, or the next watermark to emit.
     */
    @Nullable
    Watermark checkAndGetNextWatermark(T lastElement, long extractedTimestamp);
}
