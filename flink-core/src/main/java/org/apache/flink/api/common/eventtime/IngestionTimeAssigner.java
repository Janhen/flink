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
 * 一个时间戳分配器，它基于机器的挂钟分配时间戳。如果这个赋值器在一个流源之后使用，它实现了“摄取时间”语义。
 *
 * A timestamp assigner that assigns timestamps based on the machine's wall clock. If this assigner
 * is used after a stream source, it realizes "ingestion time" semantics.
 *
 * @param <T> The type of the elements that get timestamps assigned.
 *            获得分配时间戳的元素的类型。
 */
@Public
public final class IngestionTimeAssigner<T> implements TimestampAssigner<T> {

    private long maxTimestamp;

    @Override
    public long extractTimestamp(T element, long recordTimestamp) {
        // make sure timestamps are monotonously increasing, even when the system clock re-syncs
        final long now = Math.max(System.currentTimeMillis(), maxTimestamp);
        maxTimestamp = now;
        return now;
    }
}
