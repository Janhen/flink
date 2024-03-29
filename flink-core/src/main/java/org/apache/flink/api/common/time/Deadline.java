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
import org.apache.flink.util.clock.Clock;
import org.apache.flink.util.clock.SystemClock;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

/** This class stores a deadline, as obtained via {@link #now()} or from {@link #plus(Duration)}. */
// 此类存储截止日期，通过 {@link #now()} 或 {@link #plus(Duration)} 获得。
@Internal
public class Deadline {

    /** The deadline, relative to {@link System#nanoTime()}. */
    private final long timeNanos;

    /** Clock providing the time for this deadline. */
    private final Clock clock;

    private Deadline(long deadline, Clock clock) {
        this.timeNanos = deadline;
        this.clock = clock;
    }

    public Deadline plus(Duration other) {
        return new Deadline(Math.addExact(timeNanos, other.toNanos()), this.clock);
    }

    /**
     * 返回截止日期和现在之间的剩余时间。如果截止日期已过，则结果是负的。
     *
     * Returns the time left between the deadline and now. The result is negative if the deadline
     * has passed.
     */
    public Duration timeLeft() {
        return Duration.ofNanos(Math.subtractExact(timeNanos, clock.relativeTimeNanos()));
    }

    /**
     * Returns the time left between the deadline and now. If no time is left, a {@link
     * TimeoutException} will be thrown.
     *
     * @throws TimeoutException if no time is left
     */
    public Duration timeLeftIfAny() throws TimeoutException {
        long nanos = Math.subtractExact(timeNanos, clock.relativeTimeNanos());
        if (nanos <= 0) {
            throw new TimeoutException();
        }
        return Duration.ofNanos(nanos);
    }

    /** Returns whether there is any time left between the deadline and now. */
    public boolean hasTimeLeft() {
        return !isOverdue();
    }

    /** Determines whether the deadline is in the past, i.e. whether the time left is negative. */
    // 确定最后期限是否在过去，即剩余时间是否为负数。
    public boolean isOverdue() {
        return timeNanos < clock.relativeTimeNanos();
    }

    // ------------------------------------------------------------------------
    //  Creating Deadlines
    // ------------------------------------------------------------------------

    /**
     * Constructs a {@link Deadline} that has now as the deadline. Use this and then extend via
     * {@link #plus(Duration)} to specify a deadline in the future.
     */
    public static Deadline now() {
        return new Deadline(System.nanoTime(), SystemClock.getInstance());
    }

    /** Constructs a Deadline that is a given duration after now. */
    public static Deadline fromNow(Duration duration) {
        return new Deadline(
                Math.addExact(System.nanoTime(), duration.toNanos()), SystemClock.getInstance());
    }

    /**
     * Constructs a Deadline that is a given duration after now, where now and all other times from
     * this deadline are defined by the given {@link Clock}.
     *
     * @param duration Duration for this deadline.
     * @param clock Time provider for this deadline.
     */
    public static Deadline fromNowWithClock(Duration duration, Clock clock) {
        return new Deadline(Math.addExact(clock.relativeTimeNanos(), duration.toNanos()), clock);
    }

    @Override
    public String toString() {
        return LocalDateTime.now().plus(timeLeft()).toString();
    }
}
