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

package org.apache.flink.connector.kafka.source.enumerator.subscriber;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.TopicPartition;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Kafka消费者允许一些不同的方式来消费主题，包括:
 *
 * <ol>
 *     <li>从一个主题集合订阅。
 *     <li>使用Java {@code Regex}订阅主题模式
 *     <li>分配特定的分区。
 * </ol>
 *
 * <p> KafkaSubscriber 为 Kafka source提供了一个统一的接口来支持这三种订阅模式。
 *
 * Kafka consumer allows a few different ways to consume from the topics, including:
 *
 * <ol>
 *   <li>Subscribe from a collection of topics.
 *   <li>Subscribe to a topic pattern using Java {@code Regex}.
 *   <li>Assign specific partitions.
 * </ol>
 *
 * <p>The KafkaSubscriber provides a unified interface for the Kafka source to support all these
 * three types of subscribing mode.
 */
public interface KafkaSubscriber extends Serializable {

    /**
     * 获取与当前分区分配相比的分区更改。
     *
     * <p>虽然Kafka分区只能扩展，不会缩小，但是当删除主题时，分区仍然可能消失。
     *
     * Get the partitions changes compared to the current partition assignment.
     *
     * <p>Although Kafka partitions can only expand and will not shrink, the partitions may still
     * disappear when the topic is deleted.
     *
     * @param adminClient The admin client used to retrieve partition information.
     * @param currentAssignment the partitions that are currently assigned to the source readers.
     * @return The partition changes compared with the currently assigned partitions.
     */
    PartitionChange getPartitionChanges(
            AdminClient adminClient, Set<TopicPartition> currentAssignment);

    /** A container class to hold the newly added partitions and removed partitions. */
    // 一个容器类，用于存放新添加的分区和删除的分区
    class PartitionChange {
        private final Set<TopicPartition> newPartitions;
        private final Set<TopicPartition> removedPartitions;

        PartitionChange(Set<TopicPartition> newPartitions, Set<TopicPartition> removedPartitions) {
            this.newPartitions = newPartitions;
            this.removedPartitions = removedPartitions;
        }

        public Set<TopicPartition> getNewPartitions() {
            return newPartitions;
        }

        public Set<TopicPartition> getRemovedPartitions() {
            return removedPartitions;
        }
    }

    // ----------------- factory methods --------------

    static KafkaSubscriber getTopicListSubscriber(List<String> topics) {
        return new TopicListSubscriber(topics);
    }

    static KafkaSubscriber getTopicPatternSubscriber(Pattern topicPattern) {
        return new TopicPatternSubscriber(topicPattern);
    }

    static KafkaSubscriber getPartitionSetSubscriber(Set<TopicPartition> partitions) {
        return new PartitionSetSubscriber(partitions);
    }
}
