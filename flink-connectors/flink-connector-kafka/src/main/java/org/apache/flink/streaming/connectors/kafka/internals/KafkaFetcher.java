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
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.util.Collector;
import org.apache.flink.util.SerializedValue;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import static org.apache.flink.util.Preconditions.checkState;

/**
 * A fetcher that fetches data from Kafka brokers via the Kafka consumer API.
 *
 * @param <T> The type of elements produced by the fetcher.
 */
@Internal
public class KafkaFetcher<T> extends AbstractFetcher<T, TopicPartition> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaFetcher.class);

    // ------------------------------------------------------------------------

    /** The schema to convert between Kafka's byte messages, and Flink's objects. */
    private final KafkaDeserializationSchema<T> deserializer;

    /** A collector to emit records in batch (bundle). * */
    private final KafkaCollector kafkaCollector;

    /** The handover of data and exceptions between the consumer thread and the task thread. */
    // 使用者线程和任务线程之间的数据和异常切换。
    final Handover handover;

    /**
     * The thread that runs the actual KafkaConsumer and hand the record batches to this fetcher.
     */
    final KafkaConsumerThread consumerThread;

    /** Flag to mark the main work loop as alive. */
    volatile boolean running = true;

    // ------------------------------------------------------------------------

    public KafkaFetcher(
            SourceFunction.SourceContext<T> sourceContext,
            Map<KafkaTopicPartition, Long> assignedPartitionsWithInitialOffsets,
            SerializedValue<WatermarkStrategy<T>> watermarkStrategy,
            ProcessingTimeService processingTimeProvider,
            long autoWatermarkInterval,
            ClassLoader userCodeClassLoader,
            String taskNameWithSubtasks,
            KafkaDeserializationSchema<T> deserializer,
            Properties kafkaProperties,
            long pollTimeout,
            MetricGroup subtaskMetricGroup,
            MetricGroup consumerMetricGroup,
            boolean useMetrics)
            throws Exception {
        super(
                sourceContext,
                assignedPartitionsWithInitialOffsets,
                watermarkStrategy,
                processingTimeProvider,
                autoWatermarkInterval,
                userCodeClassLoader,
                consumerMetricGroup,
                useMetrics);

        this.deserializer = deserializer;
        this.handover = new Handover();

        this.consumerThread =
                new KafkaConsumerThread(
                        LOG,
                        handover,
                        kafkaProperties,
                        unassignedPartitionsQueue,
                        getFetcherName() + " for " + taskNameWithSubtasks,
                        pollTimeout,
                        useMetrics,
                        consumerMetricGroup,
                        subtaskMetricGroup);
        this.kafkaCollector = new KafkaCollector();
    }

    // ------------------------------------------------------------------------
    //  Fetcher work methods
    // ------------------------------------------------------------------------

    // 实际获取 kafka 数据源的方法
    @Override
    public void runFetchLoop() throws Exception {
        try {
            // kick off the actual Kafka consumer
            // 启动实际的Kafka消费者
            consumerThread.start();

            while (running) {
                // 在我们得到下一个记录之前，它会自动地重新抛出在消费线程中遇到的异常
                // this blocks until we get the next records
                // it automatically re-throws exceptions encountered in the consumer thread
                final ConsumerRecords<byte[], byte[]> records = handover.pollNext();

                // get the records for each topic partition
                // 获取每个主题分区的记录
                for (KafkaTopicPartitionState<T, TopicPartition> partition :
                        subscribedPartitionStates()) {

                    List<ConsumerRecord<byte[], byte[]>> partitionRecords =
                            records.records(partition.getKafkaPartitionHandle());

                    partitionConsumerRecordsHandler(partitionRecords, partition);
                }
            }
        } finally {
            // this signals the consumer thread that no more work is to be done
            consumerThread.shutdown();
        }

        // on a clean exit, wait for the runner thread
        try {
            consumerThread.join();
        } catch (InterruptedException e) {
            // may be the result of a wake-up interruption after an exception.
            // we ignore this here and only restore the interruption state
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void cancel() {
        // flag the main thread to exit. A thread interrupt will come anyways.
        running = false;
        handover.close();
        consumerThread.shutdown();
    }

    /** Gets the name of this fetcher, for thread naming and logging purposes. */
    protected String getFetcherName() {
        return "Kafka Fetcher";
    }

    protected void partitionConsumerRecordsHandler(
            List<ConsumerRecord<byte[], byte[]>> partitionRecords,
            KafkaTopicPartitionState<T, TopicPartition> partition)
            throws Exception {

        for (ConsumerRecord<byte[], byte[]> record : partitionRecords) {
            deserializer.deserialize(record, kafkaCollector);

            // emit the actual records. this also updates offset state atomically and emits
            // watermarks
            // 发出实际的记录。这也会自动更新偏移状态并发出水印
            emitRecordsWithTimestamps(
                    kafkaCollector.getRecords(), partition, record.offset(), record.timestamp());

            if (kafkaCollector.isEndOfStreamSignalled()) {
                // end of stream signaled
                running = false;
                break;
            }
        }
    }

    // ------------------------------------------------------------------------
    //  Implement Methods of the AbstractFetcher
    // ------------------------------------------------------------------------

    @Override
    public TopicPartition createKafkaPartitionHandle(KafkaTopicPartition partition) {
        return new TopicPartition(partition.getTopic(), partition.getPartition());
    }

    @Override
    protected void doCommitInternalOffsetsToKafka(
            Map<KafkaTopicPartition, Long> offsets, @Nonnull KafkaCommitCallback commitCallback)
            throws Exception {

        @SuppressWarnings("unchecked")
        List<KafkaTopicPartitionState<T, TopicPartition>> partitions = subscribedPartitionStates();

        Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>(partitions.size());

        for (KafkaTopicPartitionState<T, TopicPartition> partition : partitions) {
            Long lastProcessedOffset = offsets.get(partition.getKafkaTopicPartition());
            if (lastProcessedOffset != null) {
                checkState(lastProcessedOffset >= 0, "Illegal offset value to commit");

                // committed offsets through the KafkaConsumer need to be 1 more than the last
                // processed offset.
                // This does not affect Flink's checkpoints/saved state.
                long offsetToCommit = lastProcessedOffset + 1;

                offsetsToCommit.put(
                        partition.getKafkaPartitionHandle(), new OffsetAndMetadata(offsetToCommit));
                partition.setCommittedOffset(offsetToCommit);
            }
        }

        // record the work to be committed by the main consumer thread and make sure the consumer
        // notices that
        consumerThread.setOffsetsToCommit(offsetsToCommit, commitCallback);
    }

    private class KafkaCollector implements Collector<T> {
        private final Queue<T> records = new ArrayDeque<>();

        private boolean endOfStreamSignalled = false;

        @Override
        public void collect(T record) {
            // do not emit subsequent elements if the end of the stream reached
            if (endOfStreamSignalled || deserializer.isEndOfStream(record)) {
                endOfStreamSignalled = true;
                return;
            }
            records.add(record);
        }

        public Queue<T> getRecords() {
            return records;
        }

        public boolean isEndOfStreamSignalled() {
            return endOfStreamSignalled;
        }

        @Override
        public void close() {}
    }
}
