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

package org.apache.flink.util.clock;

import org.apache.flink.annotation.PublicEvolving;

/**
 * 一个可以访问时间的时钟。这个时钟返回两种时间：
 *
 * <h3>绝对时间<h3>
 *
 * <p>这是指真实世界的挂钟时间，通常源自系统时钟。受时钟漂移和不准确的影响，如果调整系统时钟，可能会发生跳跃。绝对时间的
 * 行为类似于 {@link System#currentTimeMillis()}。
 *
 * <h3>相对时间<h3>
 *
 * <p>这个时间以与<i>绝对时间<i>相同的速度前进，但时间戳只能相对于彼此引用。时间戳没有绝对意义，不能跨 JVM 进程进行比
 * 较。时间戳的来源不受系统时钟调整的影响，因此它永远不会跳跃。相对时间的行为类似于 {@link System#nanoTime()}。
 *
 * A clock that gives access to time. This clock returns two flavors of time:
 *
 * <h3>Absolute Time</h3>
 *
 * <p>This refers to real world wall clock time, and it is typically derived from a system clock. It
 * is subject to clock drift and inaccuracy, and can jump if the system clock is adjusted. Absolute
 * time behaves similar to {@link System#currentTimeMillis()}.
 *
 * <h3>Relative Time</h3>
 *
 * <p>This time advances at the same speed as the <i>absolute time</i>, but the timestamps can only
 * be referred to relative to each other. The timestamps have no absolute meaning and cannot be
 * compared across JVM processes. The source for the timestamps is not affected by adjustments to
 * the system clock, so it never jumps. Relative time behaves similar to {@link System#nanoTime()}.
 */
@PublicEvolving
public abstract class Clock {

    /** Gets the current absolute time, in milliseconds. */
    public abstract long absoluteTimeMillis();

    /** Gets the current relative time, in milliseconds. */
    public abstract long relativeTimeMillis();

    /** Gets the current relative time, in nanoseconds. */
    public abstract long relativeTimeNanos();
}
