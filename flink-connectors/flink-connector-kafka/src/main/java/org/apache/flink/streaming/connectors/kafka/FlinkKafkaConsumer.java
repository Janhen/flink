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

package org.apache.flink.streaming.connectors.kafka;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.connectors.kafka.config.OffsetCommitMode;
import org.apache.flink.streaming.connectors.kafka.internal.KafkaFetcher;
import org.apache.flink.streaming.connectors.kafka.internal.KafkaPartitionDiscoverer;
import org.apache.flink.streaming.connectors.kafka.internals.AbstractFetcher;
import org.apache.flink.streaming.connectors.kafka.internals.AbstractPartitionDiscoverer;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaDeserializationSchemaWrapper;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicsDescriptor;
import org.apache.flink.util.PropertiesUtil;
import org.apache.flink.util.SerializedValue;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.PropertiesUtil.getBoolean;
import static org.apache.flink.util.PropertiesUtil.getLong;

/**
 * Flink Kafka Consumer 是一个流数据源，从Apache Kafka拉出并行数据流。消费者可以在多个并行实例中运行，每个实例
 * 将从一个或多个Kafka分区中提取数据。
 *
 * <p> Flink Kafka Consumer参与检查点，并保证在失败期间没有数据丢失，并且计算处理元素“恰好一次”。(注意:这些保证
 * 自然假设Kafka本身不会丢失任何数据。)
 *
 * <p>请注意，Flink 在内部将偏移量作为其分布式检查点的一部分进行快照。Kafka 承诺的偏移只是为了让外部的进程视图与
 * Flink 的进程视图保持同步。通过这种方式，监控和其他工作可以了解Flink Kafka用户对某个主题的消费情况。
 *
 * <p>关于可用的配置属性，请参考Kafka的文档:
 * http://kafka.apache.org/documentation.html#newconsumerconfigs
 *
 * The Flink Kafka Consumer is a streaming data source that pulls a parallel data stream from Apache
 * Kafka. The consumer can run in multiple parallel instances, each of which will pull data from one
 * or more Kafka partitions.
 *
 * <p>The Flink Kafka Consumer participates in checkpointing and guarantees that no data is lost
 * during a failure, and that the computation processes elements "exactly once". (Note: These
 * guarantees naturally assume that Kafka itself does not loose any data.)
 *
 * <p>Please note that Flink snapshots the offsets internally as part of its distributed
 * checkpoints. The offsets committed to Kafka are only to bring the outside view of progress in
 * sync with Flink's view of the progress. That way, monitoring and other jobs can get a view of how
 * far the Flink Kafka consumer has consumed a topic.
 *
 * <p>Please refer to Kafka's documentation for the available configuration properties:
 * http://kafka.apache.org/documentation.html#newconsumerconfigs
 */
@PublicEvolving
public class FlinkKafkaConsumer<T> extends FlinkKafkaConsumerBase<T> {

    /** Configuration key to change the polling timeout. * */
    // 修改轮询超时的配置键。
    public static final String KEY_POLL_TIMEOUT = "flink.poll-timeout";

    /**
     * 来自Kafka的Javadoc:如果数据不可用，在轮询中等待的时间，以毫秒为单位。如果为0，则立即返回当前可用的任何记录。
     *
     * From Kafka's Javadoc: The time, in milliseconds, spent waiting in poll if data is not
     * available. If 0, returns immediately with any records that are available now.
     */
    public static final long DEFAULT_POLL_TIMEOUT = 100L;

    // ------------------------------------------------------------------------

    /** User-supplied properties for Kafka. * */
    // Kafka的用户提供的属性。
    protected final Properties properties;

    /**
     * 来自Kafka的Javadoc:如果数据不可用，在轮询中等待的时间，以毫秒为单位。如果为0，则立即返回当前可用的任何记录
     *
     * From Kafka's Javadoc: The time, in milliseconds, spent waiting in poll if data is not
     * available. If 0, returns immediately with any records that are available now
     */
    protected final long pollTimeout;

    // ------------------------------------------------------------------------

    /**
     * 创建一个新的Kafka流源消费者。
     *
     * Creates a new Kafka streaming source consumer.
     *
     * @param topic The name of the topic that should be consumed.
     * @param valueDeserializer The de-/serializer used to convert between Kafka's byte messages and
     *     Flink's objects.
     * @param props
     */
    public FlinkKafkaConsumer(
            String topic, DeserializationSchema<T> valueDeserializer, Properties props) {
        this(Collections.singletonList(topic), valueDeserializer, props);
    }

    /**
     * Creates a new Kafka streaming source consumer.
     *
     * <p>This constructor allows passing a {@see KafkaDeserializationSchema} for reading key/value
     * pairs, offsets, and topic names from Kafka.
     *
     * @param topic The name of the topic that should be consumed.
     * @param deserializer The keyed de-/serializer used to convert between Kafka's byte messages
     *     and Flink's objects.
     * @param props
     */
    public FlinkKafkaConsumer(
            String topic, KafkaDeserializationSchema<T> deserializer, Properties props) {
        this(Collections.singletonList(topic), deserializer, props);
    }

    /**
     * 创建一个新的Kafka流源消费者。
     *
     * <p>这个构造函数允许向使用者传递多个主题。
     *
     * Creates a new Kafka streaming source consumer.
     *
     * <p>This constructor allows passing multiple topics to the consumer.
     *
     * @param topics The Kafka topics to read from.
     * @param deserializer The de-/serializer used to convert between Kafka's byte messages and
     *     Flink's objects.
     * @param props
     */
    public FlinkKafkaConsumer(
            List<String> topics, DeserializationSchema<T> deserializer, Properties props) {
        this(topics, new KafkaDeserializationSchemaWrapper<>(deserializer), props);
    }

    /**
     * 创建一个新的Kafka流源消费者。
     *
     * <p> 这个构造函数允许传递多个主题和一个键值反序列化模式。
     *
     * Creates a new Kafka streaming source consumer.
     *
     * <p>This constructor allows passing multiple topics and a key/value deserialization schema.
     *
     * @param topics The Kafka topics to read from.
     * @param deserializer The keyed de-/serializer used to convert between Kafka's byte messages
     *     and Flink's objects.
     * @param props
     */
    public FlinkKafkaConsumer(
            List<String> topics, KafkaDeserializationSchema<T> deserializer, Properties props) {
        this(topics, null, deserializer, props);
    }

    /**
     * 创建一个新的Kafka流源消费者。使用此构造函数可基于正则表达式模式订阅多个主题。
     *
     * <p> 如果启用分区发现(通过在属性中为 {@link FlinkKafkaConsumer#KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS}
     * 设置一个非负值)，名称匹配模式的主题也将被订阅，因为它们是动态创建的。
     *
     * Creates a new Kafka streaming source consumer. Use this constructor to subscribe to multiple
     * topics based on a regular expression pattern.
     *
     * <p>If partition discovery is enabled (by setting a non-negative value for {@link
     * FlinkKafkaConsumer#KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS} in the properties), topics with
     * names matching the pattern will also be subscribed to as they are created on the fly.
     *
     * @param subscriptionPattern The regular expression for a pattern of topic names to subscribe
     *     to.
     * @param valueDeserializer The de-/serializer used to convert between Kafka's byte messages and
     *     Flink's objects.
     * @param props
     */
    public FlinkKafkaConsumer(
            Pattern subscriptionPattern,
            DeserializationSchema<T> valueDeserializer,
            Properties props) {
        this(
                null,
                subscriptionPattern,
                new KafkaDeserializationSchemaWrapper<>(valueDeserializer),
                props);
    }

    /**
     * Creates a new Kafka streaming source consumer. Use this constructor to subscribe to multiple
     * topics based on a regular expression pattern.
     *
     * <p>If partition discovery is enabled (by setting a non-negative value for {@link
     * FlinkKafkaConsumer#KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS} in the properties), topics with
     * names matching the pattern will also be subscribed to as they are created on the fly.
     *
     * <p>This constructor allows passing a {@see KafkaDeserializationSchema} for reading key/value
     * pairs, offsets, and topic names from Kafka.
     *
     * @param subscriptionPattern The regular expression for a pattern of topic names to subscribe
     *     to.
     * @param deserializer The keyed de-/serializer used to convert between Kafka's byte messages
     *     and Flink's objects.
     * @param props
     */
    public FlinkKafkaConsumer(
            Pattern subscriptionPattern,
            KafkaDeserializationSchema<T> deserializer,
            Properties props) {
        this(null, subscriptionPattern, deserializer, props);
    }

    private FlinkKafkaConsumer(
            List<String> topics,
            Pattern subscriptionPattern,
            KafkaDeserializationSchema<T> deserializer,
            Properties props) {

        super(
                topics,
                subscriptionPattern,
                deserializer,
                getLong(
                        checkNotNull(props, "props"),
                        // 配置键定义使用者的分区发现间隔
                        KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS,
                        // 执行分区发现的默认间隔
                        PARTITION_DISCOVERY_DISABLED),
                // 配置键禁用度量跟踪
                !getBoolean(props, KEY_DISABLE_METRICS, false));

        this.properties = props;
        setDeserializer(this.properties);

        // configure the polling timeout
        try {
            if (properties.containsKey(KEY_POLL_TIMEOUT)) {
                this.pollTimeout = Long.parseLong(properties.getProperty(KEY_POLL_TIMEOUT));
            } else {
                this.pollTimeout = DEFAULT_POLL_TIMEOUT;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot parse poll timeout for '" + KEY_POLL_TIMEOUT + '\'', e);
        }
    }

    @Override
    protected AbstractFetcher<T, ?> createFetcher(
            SourceContext<T> sourceContext,
            Map<KafkaTopicPartition, Long> assignedPartitionsWithInitialOffsets,
            SerializedValue<WatermarkStrategy<T>> watermarkStrategy,
            StreamingRuntimeContext runtimeContext,
            OffsetCommitMode offsetCommitMode,
            MetricGroup consumerMetricGroup,
            boolean useMetrics)
            throws Exception {

        // make sure that auto commit is disabled when our offset commit mode is ON_CHECKPOINTS;
        // this overwrites whatever setting the user configured in the properties
        // 当我们的偏移提交模式是 ON_CHECKPOINTS 时，确保自动提交是禁用的;
        // 这将覆盖用户在属性中配置的任何设置
        adjustAutoCommitConfig(properties, offsetCommitMode);

        return new KafkaFetcher<>(
                sourceContext,
                assignedPartitionsWithInitialOffsets,
                watermarkStrategy,
                runtimeContext.getProcessingTimeService(),
                runtimeContext.getExecutionConfig().getAutoWatermarkInterval(),
                runtimeContext.getUserCodeClassLoader(),
                runtimeContext.getTaskNameWithSubtasks(),
                deserializer,
                properties,
                pollTimeout,
                runtimeContext.getMetricGroup(),
                consumerMetricGroup,
                useMetrics);
    }

    @Override
    protected AbstractPartitionDiscoverer createPartitionDiscoverer(
            KafkaTopicsDescriptor topicsDescriptor,
            int indexOfThisSubtask,
            int numParallelSubtasks) {

        return new KafkaPartitionDiscoverer(
                topicsDescriptor, indexOfThisSubtask, numParallelSubtasks, properties);
    }

    @Override
    protected Map<KafkaTopicPartition, Long> fetchOffsetsWithTimestamp(
            Collection<KafkaTopicPartition> partitions, long timestamp) {

        Map<TopicPartition, Long> partitionOffsetsRequest = new HashMap<>(partitions.size());
        for (KafkaTopicPartition partition : partitions) {
            partitionOffsetsRequest.put(
                    new TopicPartition(partition.getTopic(), partition.getPartition()), timestamp);
        }

        final Map<KafkaTopicPartition, Long> result = new HashMap<>(partitions.size());
        // use a short-lived consumer to fetch the offsets;
        // this is ok because this is a one-time operation that happens only on startup
        try (KafkaConsumer<?, ?> consumer = new KafkaConsumer(properties)) {
            for (Map.Entry<TopicPartition, OffsetAndTimestamp> partitionToOffset :
                    consumer.offsetsForTimes(partitionOffsetsRequest).entrySet()) {

                result.put(
                        new KafkaTopicPartition(
                                partitionToOffset.getKey().topic(),
                                partitionToOffset.getKey().partition()),
                        (partitionToOffset.getValue() == null)
                                ? null
                                : partitionToOffset.getValue().offset());
            }
        }
        return result;
    }

    @Override
    protected boolean getIsAutoCommitEnabled() {
        // J: 默认自动提交间隔 5s
        return getBoolean(properties, ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
                && PropertiesUtil.getLong(
                                properties, ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000)
                        > 0;
    }

    /**
     * Makes sure that the ByteArrayDeserializer is registered in the Kafka properties.
     *
     * @param props The Kafka properties to register the serializer in.
     */
    private static void setDeserializer(Properties props) {
        final String deSerName = ByteArrayDeserializer.class.getName();

        Object keyDeSer = props.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
        Object valDeSer = props.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);

        if (keyDeSer != null && !keyDeSer.equals(deSerName)) {
            LOG.warn(
                    "Ignoring configured key DeSerializer ({})",
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
        }
        if (valDeSer != null && !valDeSer.equals(deSerName)) {
            LOG.warn(
                    "Ignoring configured value DeSerializer ({})",
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        }

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, deSerName);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deSerName);
    }
}
