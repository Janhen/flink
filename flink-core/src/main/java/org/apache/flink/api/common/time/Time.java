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

import org.apache.flink.annotation.PublicEvolving;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * 时间间隔的定义。类似于 {@link java.time.Duration} 的简单版本。
 *
 * <p>注意:这个类将完全取代 Flink 2.0 中的 org.apache.Flink.streaming.api.windowing.time.Time
 *
 * The definition of a time interval. Similar to a simpler version of {@link java.time.Duration}.
 *
 * <p>Note: This class will fully replace org.apache.flink.streaming.api.windowing.time.Time in
 * Flink 2.0
 */
@PublicEvolving
public final class Time implements Serializable {

    private static final long serialVersionUID = -350254188460915999L;

    /** The time unit for this policy's time interval. */
    private final TimeUnit unit;

    /** The size of the windows generated by this policy. */
    private final long size;

    /** Instantiation only via factory method. */
    private Time(long size, TimeUnit unit) {
        this.unit = checkNotNull(unit, "time unit may not be null");
        this.size = size;
    }

    // ------------------------------------------------------------------------
    //  Properties
    // ------------------------------------------------------------------------

    /**
     * Gets the time unit for this policy's time interval.
     *
     * @return The time unit for this policy's time interval.
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * Gets the length of this policy's time interval.
     *
     * @return The length of this policy's time interval.
     */
    public long getSize() {
        return size;
    }

    /**
     * Converts the time interval to milliseconds.
     *
     * @return The time interval in milliseconds.
     */
    public long toMilliseconds() {
        return unit.toMillis(size);
    }

    @Override
    public String toString() {
        return toMilliseconds() + " ms";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Time time = (Time) o;
        return toMilliseconds() == time.toMilliseconds();
    }

    @Override
    public int hashCode() {
        return Objects.hash(toMilliseconds());
    }

    // ------------------------------------------------------------------------
    //  Factory
    // ------------------------------------------------------------------------

    /**
     * Creates a new {@link Time} of the given duration and {@link TimeUnit}.
     *
     * @param size The duration of time.
     * @param unit The unit of time of the duration, for example {@code TimeUnit.SECONDS}.
     * @return The time policy.
     */
    public static Time of(long size, TimeUnit unit) {
        return new Time(size, unit);
    }

    /** Creates a new {@link Time} that represents the given number of milliseconds. */
    public static Time milliseconds(long milliseconds) {
        return of(milliseconds, TimeUnit.MILLISECONDS);
    }

    /** Creates a new {@link Time} that represents the given number of seconds. */
    public static Time seconds(long seconds) {
        return of(seconds, TimeUnit.SECONDS);
    }

    /** Creates a new {@link Time} that represents the given number of minutes. */
    public static Time minutes(long minutes) {
        return of(minutes, TimeUnit.MINUTES);
    }

    /** Creates a new {@link Time} that represents the given number of hours. */
    public static Time hours(long hours) {
        return of(hours, TimeUnit.HOURS);
    }

    /** Creates a new {@link Time} that represents the given number of days. */
    public static Time days(long days) {
        return of(days, TimeUnit.DAYS);
    }
}
