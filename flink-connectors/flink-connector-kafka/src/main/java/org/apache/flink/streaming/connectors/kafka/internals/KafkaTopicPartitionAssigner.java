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

package org.apache.flink.streaming.connectors.kafka.internals;

import org.apache.flink.annotation.Internal;

/** Utility for assigning Kafka partitions to consumer subtasks. */
@Internal
public class KafkaTopicPartitionAssigner {

    /**
     * 返回指定 Kafka 分区应该分配给的目标子任务的索引。
     *
     * <p>单个主题分区的结果分布具有以下契约:
     *
     * <li>1.均匀分布在子任务之间
     * <li>2. 分区是循环分布的(严格顺时针w.r.t.升序子任务索引)，使用分区id作为起始索引的偏移量(即，将分配给
     * 主题的分区0的子任务的索引，使用主题名称确定)。
     *
     * <p>上述合同至关重要，不可毁约。消费者子任务依赖此契约在本地过滤掉不应该订阅的分区，从而保证单个主题的所有
     * 分区始终以统一分布的方式分配给某些子任务。
     *
     * Returns the index of the target subtask that a specific Kafka partition should be assigned
     * to.
     *
     * <p>The resulting distribution of partitions of a single topic has the following contract:
     *
     * <ul>
     *   <li>1. Uniformly distributed across subtasks
     *   <li>2. Partitions are round-robin distributed (strictly clockwise w.r.t. ascending subtask
     *       indices) by using the partition id as the offset from a starting index (i.e., the index
     *       of the subtask which partition 0 of the topic will be assigned to, determined using the
     *       topic name).
     * </ul>
     *
     * <p>The above contract is crucial and cannot be broken. Consumer subtasks rely on this
     * contract to locally filter out partitions that it should not subscribe to, guaranteeing that
     * all partitions of a single topic will always be assigned to some subtask in a uniformly
     * distributed manner.
     *
     * @param partition the Kafka partition
     * @param numParallelSubtasks total number of parallel subtasks
     * @return index of the target subtask that the Kafka partition should be assigned to.
     */
    public static int assign(KafkaTopicPartition partition, int numParallelSubtasks) {
        // J: 根据 topic 确定 start index
        int startIndex =
                ((partition.getTopic().hashCode() * 31) & 0x7FFFFFFF) % numParallelSubtasks;

        // here, the assumption is that the id of Kafka partitions are always ascending
        // starting from 0, and therefore can be used directly as the offset clockwise from the
        // start index
        // 在这里，假设Kafka分区的id总是从0开始升序，因此可以直接用作从开始索引顺时针方向的偏移量
        return (startIndex + partition.getPartition()) % numParallelSubtasks;
    }
}
