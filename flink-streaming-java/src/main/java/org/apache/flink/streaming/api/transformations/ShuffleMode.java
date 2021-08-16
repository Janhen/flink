/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.transformations;

import org.apache.flink.annotation.PublicEvolving;

/** The shuffle mode defines the data exchange mode between operators. */
// shuffle 模式定义了算子之间的数据交换模式。
@PublicEvolving
public enum ShuffleMode {
    /**
     * 生产者和消费者同时在线。消费者立即收到生成的数据。
     *
     * Producer and consumer are online at the same time. Produced data is received by consumer
     * immediately.
     */
    PIPELINED,

    /**
     * 生产者首先产生其整个结果并完成。之后，消费者启动并可以消费数据。
     *
     * The producer first produces its entire result and finishes. After that, the consumer is
     * started and may consume the data.
     */
    BATCH,

    /**
     * 随机播放模式未定义。它让框架来决定随机播放模式。框架最终会选择 {@link ShuffleMode#BATCH} 或
     * {@link ShuffleMode#PIPELINED} 之一。
     *
     * The shuffle mode is undefined. It leaves it up to the framework to decide the shuffle mode.
     * The framework will pick one of {@link ShuffleMode#BATCH} or {@link ShuffleMode#PIPELINED} in
     * the end.
     */
    UNDEFINED
}
