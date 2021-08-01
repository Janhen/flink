/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.runtime.tasks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 定义当前处理时间并处理所有相关操作，例如为 future 要执行的任务注册计时器。
 *
 * <p>通过 {@link #getCurrentProcessingTime()} 访问时间总是可用的，不管计时器服务是否已经关闭。
 *
 * Defines the current processing time and handles all related actions, such as register timers for
 * tasks to be executed in the future.
 *
 * <p>The access to the time via {@link #getCurrentProcessingTime()} is always available, regardless
 * of whether the timer service has been shut down.
 */
public interface ProcessingTimeService {

    /** Returns the current processing time. */
    // 返回当前处理时间。
    long getCurrentProcessingTime();

    /**
     * 注册一个要在(处理)时间为{@code timestamp}时执行的任务。
     *
     * Registers a task to be executed when (processing) time is {@code timestamp}.
     *
     * @param timestamp Time when the task is to be executed (in processing time)
     * @param target The task to be executed
     * @return The future that represents the scheduled task. This always returns some future, even
     *     if the timer was shut down
     */
    ScheduledFuture<?> registerTimer(long timestamp, ProcessingTimeCallback target);

    /**
     * 注册一个以固定速率重复执行的任务。
     *
     * Registers a task to be executed repeatedly at a fixed rate.
     *
     * <p>This call behaves similar to {@link
     * org.apache.flink.runtime.concurrent.ScheduledExecutor#scheduleAtFixedRate(Runnable, long,
     * long, TimeUnit)}.
     *
     * @param callback to be executed after the initial delay and then after each period
     * @param initialDelay initial delay to start executing callback
     * @param period after the initial delay after which the callback is executed
     * @return Scheduled future representing the task to be executed repeatedly
     */
    ScheduledFuture<?> scheduleAtFixedRate(
            ProcessingTimeCallback callback, long initialDelay, long period);

    /**
     * 注册一个以固定延迟重复执行的任务。
     *
     * Registers a task to be executed repeatedly with a fixed delay.
     *
     * <p>This call behaves similar to {@link
     * org.apache.flink.runtime.concurrent.ScheduledExecutor#scheduleWithFixedDelay(Runnable, long,
     * long, TimeUnit)}.
     *
     * @param callback to be executed after the initial delay and then after each period
     * @param initialDelay initial delay to start executing callback
     * @param period after the initial delay after which the callback is executed
     * @return Scheduled future representing the task to be executed repeatedly
     */
    ScheduledFuture<?> scheduleWithFixedDelay(
            ProcessingTimeCallback callback, long initialDelay, long period);

    /**
     * 这个方法将服务放入一个状态，其中它不注册新的计时器，但每次调用 {@link #registerTimer} 或
     * {@link #scheduleAtFixedRate} 都会返回一个“mock”future，而这个“mock”future将永远不会完成。此外，之前
     * 注册的计时器不会被触发，但允许正在运行的计时器完成。
     *
     * This method puts the service into a state where it does not register new timers, but returns
     * for each call to {@link #registerTimer} or {@link #scheduleAtFixedRate} a "mock" future and
     * the "mock" future will be never completed. Furthermore, the timers registered before are
     * prevented from firing, but the timers in running are allowed to finish.
     *
     * <p>If no timer is running, the quiesce-completed future is immediately completed and
     * returned. Otherwise, the future returned will be completed when all running timers have
     * finished.
     */
    CompletableFuture<Void> quiesce();
}
