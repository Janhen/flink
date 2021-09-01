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

package org.apache.flink.api.common.time;

import org.apache.flink.annotation.Internal;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/** This class stores a deadline, as obtained via {@link #now()} or from {@link #plus(Duration)}. */
// 这个类存储一个截止日期，可以通过 {@link now()} 或 {@link plus(Duration)} 获得。
@Internal
public class Deadline {

    /** The deadline, relative to {@link System#nanoTime()}. */
    // 截止日期，相对于 {@link System#nanoTime()}。
    private final long timeNanos;

    private Deadline(long deadline) {
        this.timeNanos = deadline;
    }

    public Deadline plus(Duration other) {
        return new Deadline(Math.addExact(timeNanos, other.toNanos()));
    }

    /**
     * 返回截止日期和现在之间的剩余时间。如果截止日期已过，则结果是否定的。
     *
     * Returns the time left between the deadline and now. The result is negative if the deadline
     * has passed.
     */
    public Duration timeLeft() {
        return Duration.ofNanos(Math.subtractExact(timeNanos, System.nanoTime()));
    }

    /**
     * 返回截止日期和现在之间的剩余时间。如果没有时间，将抛出 {@link TimeoutException}。
     *
     * Returns the time left between the deadline and now. If no time is left, a {@link
     * TimeoutException} will be thrown.
     *
     * @throws TimeoutException if no time is left
     */
    public Duration timeLeftIfAny() throws TimeoutException {
        long nanos = Math.subtractExact(timeNanos, System.nanoTime());
        if (nanos <= 0) {
            throw new TimeoutException();
        }
        return Duration.ofNanos(nanos);
    }

    /** Returns whether there is any time left between the deadline and now. */
    // 返回截止日期和现在之间是否还有时间。
    public boolean hasTimeLeft() {
        return !isOverdue();
    }

    /** Determines whether the deadline is in the past, i.e. whether the time left is negative. */
    // 确定截止日期是否已过去，即剩余时间是否为负数。
    public boolean isOverdue() {
        return timeNanos < System.nanoTime();
    }

    // ------------------------------------------------------------------------
    //  Creating Deadlines
    // ------------------------------------------------------------------------

    /**
     * 构造一个现在作为截止日期的 {@link Deadline}。使用它，然后通过 {@link #plus(Duration)} 扩展以指定将来的
     * 截止日期。
     *
     * Constructs a {@link Deadline} that has now as the deadline. Use this and then extend via
     * {@link #plus(Duration)} to specify a deadline in the future.
     */
    public static Deadline now() {
        return new Deadline(System.nanoTime());
    }

    /** Constructs a Deadline that is a given duration after now. */
    // 构造一个在现在之后给定持续时间的截止日期。
    public static Deadline fromNow(Duration duration) {
        return new Deadline(Math.addExact(System.nanoTime(), duration.toNanos()));
    }
}
