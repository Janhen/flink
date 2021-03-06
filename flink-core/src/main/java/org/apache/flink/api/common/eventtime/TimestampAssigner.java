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

package org.apache.flink.api.common.eventtime;

import org.apache.flink.annotation.Public;

/**
 * {@code TimestampAssigner}将事件时间戳分配给元素。所有操作事件时间的函数都使用这些时间戳，例如事件时间窗口。
 *
 * 时间戳可以是任意的{@code long}值，但所有内置实现都将其表示为从Epoch (midnight, January 1, 1970 UTC)以来的
 * 毫秒数，与{@link System#currentTimeMillis()}的方法相同。
 *
 * A {@code TimestampAssigner} assigns event time timestamps to elements. These timestamps are used
 * by all functions that operate on event time, for example event time windows.
 *
 * <p>Timestamps can be an arbitrary {@code long} value, but all built-in implementations represent
 * it as the milliseconds since the Epoch (midnight, January 1, 1970 UTC), the same way as {@link
 * System#currentTimeMillis()} does it.
 *
 * @param <T> The type of the elements to which this assigner assigns timestamps.
 */
@Public
@FunctionalInterface
public interface TimestampAssigner<T> {

    /**
     * 当记录上没有附加以前的时间戳时，传递给{@link #extractTimestamp}的值。
     *
     * The value that is passed to {@link #extractTimestamp} when there is no previous timestamp
     * attached to the record.
     */
    long NO_TIMESTAMP = Long.MIN_VALUE;

    /**
     * 将时间戳(以毫秒为单位)分配给元素。这是独立于任何特定的时区或日历。
     *
     * <p>该方法传递先前分配的元素时间戳。以前的时间戳可能是从以前的赋值器分配的。如果元素之前没有携带时间戳，这个值
     * 是{@link #NO_TIMESTAMP} (= {@code Long.MIN_VALUE}: {@value Long#MIN_VALUE})。
*    *
     * Assigns a timestamp to an element, in milliseconds since the Epoch. This is independent of
     * any particular time zone or calendar.
     *
     * <p>The method is passed the previously assigned timestamp of the element. That previous
     * timestamp may have been assigned from a previous assigner. If the element did not carry a
     * timestamp before, this value is {@link #NO_TIMESTAMP} (= {@code Long.MIN_VALUE}: {@value
     * Long#MIN_VALUE}).
     *
     * @param element The element that the timestamp will be assigned to.
     *                时间戳将被赋值给的元素。
     * @param recordTimestamp The current internal timestamp of the element, or a negative value, if
     *     no timestamp has been assigned yet.
     *     元素的当前内部时间戳，如果还没有分配时间戳，则为负值。
     * @return The new timestamp.
     */
    long extractTimestamp(T element, long recordTimestamp);
}
