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

package org.apache.flink.streaming.api;

import org.apache.flink.annotation.PublicEvolving;

/** Interface for working with time and timers. */
// 与时间和计时器工作的接口
@PublicEvolving
public interface TimerService {

    /** Error string for {@link UnsupportedOperationException} on registering timers. */
    // 注册计时器时，{@link UnsupportedOperationException} 的错误字符串。
    String UNSUPPORTED_REGISTER_TIMER_MSG = "Setting timers is only supported on a keyed streams.";

    /** Error string for {@link UnsupportedOperationException} on deleting timers. */
    // {@link UnsupportedOperationException} 在删除计时器时出错。
    String UNSUPPORTED_DELETE_TIMER_MSG = "Deleting timers is only supported on a keyed streams.";

    /** Returns the current processing time. */
    // 返回当前处理时间。
    long currentProcessingTime();

    /** Returns the current event-time watermark. */
    // 返回当前事件时间水印。
    long currentWatermark();

    /**
     * 注册一个计时器，当处理时间经过给定时间时触发。
     *
     * <p>定时器可以在内部限定为键和或窗口。当您在关键字上下文中设置计时器时，例如在
     *   {@link org.apache.flink.streaming.api.datastream.KeyedStream}，那么当你收到定时器通知时，该上下文也
     *   将被激活
     *
     * Registers a timer to be fired when processing time passes the given time.
     *
     * <p>Timers can internally be scoped to keys and/or windows. When you set a timer in a keyed
     * context, such as in an operation on {@link
     * org.apache.flink.streaming.api.datastream.KeyedStream} then that context will also be active
     * when you receive the timer notification.
     */
    void registerProcessingTimeTimer(long time);

    /**
     * 注册一个计时器，当事件时间水印经过给定时间时触发。
     *
     * <p>定时器可以在内部限定为键和或窗口。当您在关键字上下文中设置计时器时， 例如在
     * {@link org.apache.flink.streaming.api.datastream.KeyedStream}，那么当你收到定时器通知时，该上下文
     * 也将被激活。
     *
     * Registers a timer to be fired when the event time watermark passes the given time.
     *
     * <p>Timers can internally be scoped to keys and/or windows. When you set a timer in a keyed
     * context, such as in an operation on {@link
     * org.apache.flink.streaming.api.datastream.KeyedStream} then that context will also be active
     * when you receive the timer notification.
     */
    void registerEventTimeTimer(long time);

    /**
     * 删除给定触发器时间的处理时间计时器。此方法只有在这样的计时器之前已注册且尚未过期时才有效果。
     *
     * <p>计时器可以在内部作用域为键和或窗口。当删除一个计时器时，它将从当前键控上下文中删除。
     *
     * Deletes the processing-time timer with the given trigger time. This method has only an effect
     * if such a timer was previously registered and did not already expire.
     *
     * <p>Timers can internally be scoped to keys and/or windows. When you delete a timer, it is
     * removed from the current keyed context.
     */
    void deleteProcessingTimeTimer(long time);

    /**
     * 删除具有给定触发时间的事件时间计时器。这个方法只有在这样的计时器之前已经注册并且还没有过期的情况下才有效。
     *
     * <p>定时器可以在内部限定为键和或窗口。当删除计时器时，它将从当前关键字上下文中删除。
     *
     * Deletes the event-time timer with the given trigger time. This method has only an effect if
     * such a timer was previously registered and did not already expire.
     *
     * <p>Timers can internally be scoped to keys and/or windows. When you delete a timer, it is
     * removed from the current keyed context.
     */
    void deleteEventTimeTimer(long time);
}
