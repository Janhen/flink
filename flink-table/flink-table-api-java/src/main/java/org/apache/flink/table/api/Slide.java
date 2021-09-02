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

package org.apache.flink.table.api;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.expressions.ExpressionParser;

/**
 * 用于创建滑动窗口的助手类。滑动窗口具有固定大小并按指定的滑动间隔滑动。如果滑动间隔小于窗口大小，则滑动窗口重叠。
 * 因此，一个元素可以分配给多个窗口。
 *
 * <p>例如，大小为 15 分钟、滑动间隔为 5 分钟的滑动窗口将 15 分钟的元素分组并每五分钟评估一次。每个元素都包含在三个
 * 连续的窗口评估中。
 *
 * Helper class for creating a sliding window. Sliding windows have a fixed size and slide by a
 * specified slide interval. If the slide interval is smaller than the window size, sliding windows
 * are overlapping. Thus, an element can be assigned to multiple windows.
 *
 * <p>For example, a sliding window of size 15 minutes with 5 minutes sliding interval groups
 * elements of 15 minutes and evaluates every five minutes. Each element is contained in three
 * consecutive window evaluations.
 *
 * <p>Java Example:
 *
 * <pre>{@code
 * Slide.over("10.minutes").every("5.minutes").on("rowtime").as("w")
 * }</pre>
 *
 * <p>Scala Example:
 *
 * <pre>{@code
 * Slide over 10.minutes every 5.minutes on 'rowtime as 'w
 * }</pre>
 */
@PublicEvolving
public final class Slide {

    /**
     * 创建一个滑动窗口。滑动窗口具有固定大小并按指定的滑动间隔滑动。如果滑动间隔小于窗口大小，则滑动窗口重叠。因此，
     * 一个元素可以分配给多个窗口。
     *
     * <p>例如，大小为 15 分钟、滑动间隔为 5 分钟的滑动窗口将 15 分钟的元素分组并每五分钟评估一次。每个元素都包含在
     *   三个连续的窗口评估中。
     *
     * Creates a sliding window. Sliding windows have a fixed size and slide by a specified slide
     * interval. If the slide interval is smaller than the window size, sliding windows are
     * overlapping. Thus, an element can be assigned to multiple windows.
     *
     * <p>For example, a sliding window of size 15 minutes with 5 minutes sliding interval groups
     * elements of 15 minutes and evaluates every five minutes. Each element is contained in three
     * consecutive window evaluations.
     *
     * @param size the size of the window as time or row-count interval
     * @return a partially specified sliding window
     * @deprecated use {@link #over(Expression)}
     */
    @Deprecated
    public static SlideWithSize over(String size) {
        return over(ExpressionParser.parseExpression(size));
    }

    /**
     * Creates a sliding window. Sliding windows have a fixed size and slide by a specified slide
     * interval. If the slide interval is smaller than the window size, sliding windows are
     * overlapping. Thus, an element can be assigned to multiple windows.
     *
     * <p>For example, a sliding window of size 15 minutes with 5 minutes sliding interval groups
     * elements of 15 minutes and evaluates every five minutes. Each element is contained in three
     * consecutive
     *
     * @param size the size of the window as time or row-count interval
     * @return a partially specified sliding window
     */
    public static SlideWithSize over(Expression size) {
        return new SlideWithSize(size);
    }
}
