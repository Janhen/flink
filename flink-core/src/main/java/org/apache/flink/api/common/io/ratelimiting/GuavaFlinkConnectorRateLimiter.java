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

package org.apache.flink.api.common.io.ratelimiting;

import org.apache.flink.api.common.functions.RuntimeContext;

import org.apache.flink.shaded.guava30.com.google.common.util.concurrent.RateLimiter;

/**
 * {@link FlinkConnectorRateLimiter}的实现，它使用Guava的RateLimiter进行速率限制。
 *
 * An implementation of {@link FlinkConnectorRateLimiter} that uses Guava's RateLimiter for rate
 * limiting.
 */
public class GuavaFlinkConnectorRateLimiter implements FlinkConnectorRateLimiter {

    private static final long serialVersionUID = -3680641524643737192L;

    /** Rate in bytes per second for the consumer on a whole. */
    // 总的来说，用户的速率以字节每秒为单位。
    private long globalRateBytesPerSecond;

    /** Rate in bytes per second per subtask of the consumer. */
    // 消费者的每个子任务每秒的字节率。
    private long localRateBytesPerSecond;

    /** Runtime context. * */
    private RuntimeContext runtimeContext;

    /** RateLimiter. * */
    private RateLimiter rateLimiter;

    /**
     * Creates a rate limiter with the runtime context provided.
     *
     * @param runtimeContext
     */
    @Override
    public void open(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
        localRateBytesPerSecond =
                globalRateBytesPerSecond / runtimeContext.getNumberOfParallelSubtasks();
        this.rateLimiter = RateLimiter.create(localRateBytesPerSecond);
    }

    /**
     * Set the global per consumer and per sub-task rates.
     *
     * @param globalRate Value of rate in bytes per second.
     */
    @Override
    public void setRate(long globalRate) {
        this.globalRateBytesPerSecond = globalRate;
    }

    @Override
    public void acquire(int permits) {
        // Ensure permits > 0
        rateLimiter.acquire(Math.max(1, permits));
    }

    @Override
    public long getRate() {
        return globalRateBytesPerSecond;
    }

    @Override
    public void close() {}
}
