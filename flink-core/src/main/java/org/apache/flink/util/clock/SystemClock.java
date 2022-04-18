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
 * 返回系统进程时间的时钟。
 *
 * <p>该时钟使用{@link System#currentTimeMillis()}表示<i>绝对时间<i>，使用{@link System#nanoTime()}表示<i>相对时间<i>。
 *
 * <p> SystemClock作为一个单例实例存在。
 *
 * A clock that returns the time of the system / process.
 *
 * <p>This clock uses {@link System#currentTimeMillis()} for <i>absolute time</i> and {@link
 * System#nanoTime()} for <i>relative time</i>.
 *
 * <p>This SystemClock exists as a singleton instance.
 */
@PublicEvolving
public final class SystemClock extends Clock {

    private static final SystemClock INSTANCE = new SystemClock();

    public static SystemClock getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------

    @Override
    public long absoluteTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long relativeTimeMillis() {
        return System.nanoTime() / 1_000_000;
    }

    @Override
    public long relativeTimeNanos() {
        return System.nanoTime();
    }

    // ------------------------------------------------------------------------

    private SystemClock() {}
}
