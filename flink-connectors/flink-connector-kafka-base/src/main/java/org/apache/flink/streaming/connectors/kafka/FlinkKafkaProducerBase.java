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

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.ClosureCleaner;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.connectors.kafka.internals.KeyedSerializationSchemaWrapper;
import org.apache.flink.streaming.connectors.kafka.internals.metrics.KafkaMetricWrapper;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema;
import org.apache.flink.util.NetUtils;
import org.apache.flink.util.SerializableObject;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Flink Sink 生成数据到 Kafka 主题中。
 *
 * <p>请注意，当启用检查点并设置 setFlushOnCheckpoint(true) 时，此生成器提供至少一次的可靠性保证。否则，生产者
 *   不提供任何可靠性保证。
 *
 * Flink Sink to produce data into a Kafka topic.
 *
 * <p>Please note that this producer provides at-least-once reliability guarantees when checkpoints
 * are enabled and setFlushOnCheckpoint(true) is set. Otherwise, the producer doesn't provide any
 * reliability guarantees.
 *
 * @param <IN> Type of the messages to write into Kafka.
 */
@Internal
public abstract class FlinkKafkaProducerBase<IN> extends RichSinkFunction<IN>
        implements CheckpointedFunction {

    private static final Logger LOG = LoggerFactory.getLogger(FlinkKafkaProducerBase.class);

    private static final long serialVersionUID = 1L;

    /** Configuration key for disabling the metrics reporting. */
    // 禁用度量报告的配置键。
    public static final String KEY_DISABLE_METRICS = "flink.disable-metrics";

    /** User defined properties for the Producer. */
    // 生产者的用户定义属性。
    protected final Properties producerConfig;

    /** The name of the default topic this producer is writing data to. */
    // 此生成器要写入数据的默认主题的名称。
    protected final String defaultTopicId;

    /**
     * (Serializable)将 Flink中 使用的对象转换为。kafka 的 byte[]。
     *
     * (Serializable) SerializationSchema for turning objects used with Flink into. byte[] for
     * Kafka.
     */
    protected final KeyedSerializationSchema<IN> schema;

    /** User-provided partitioner for assigning an object to a Kafka partition for each topic. */
    // 用户提供的分区器，用于为每个主题分配一个对象到 Kafka 分区。
    protected final FlinkKafkaPartitioner<IN> flinkKafkaPartitioner;

    /** Partitions of each topic. */
    // 每个主题的分区。
    protected final Map<String, int[]> topicPartitionsMap;

    /** Flag indicating whether to accept failures (and log them), or to fail on failures. */
    // 指示是接受失败(并记录它们)，还是在失败时失败的标志。
    protected boolean logFailuresOnly;

    /**
     * 如果为真，producer 将等待直到所有未完成的记录都发送给 broker。
     *
     * If true, the producer will wait until all outstanding records have been send to the broker.
     */
    protected boolean flushOnCheckpoint = true;

    // -------------------------------- Runtime fields ------------------------------------------

    /** KafkaProducer instance. */
    // KafkaProducer实例。
    protected transient KafkaProducer<byte[], byte[]> producer;

    /** The callback than handles error propagation or logging callbacks. */
    // 回调比处理错误传播或记录回调。
    protected transient Callback callback;

    /** Errors encountered in the async producer are stored here. */
    // 在异步生成器中遇到的错误存储在这里。
    protected transient volatile Exception asyncException;

    /** Lock for accessing the pending records. */
    // 锁定用于访问挂起记录。
    protected final SerializableObject pendingRecordsLock = new SerializableObject();

    /** Number of unacknowledged records. */
    // 未确认的记录数。
    protected long pendingRecords;

    /**
     * 创建 FlinkKafkaProducer 的主构造函数。
     *
     * The main constructor for creating a FlinkKafkaProducer.
     *
     * @param defaultTopicId The default topic to write data to
     * @param serializationSchema A serializable serialization schema for turning user objects into
     *     a kafka-consumable byte[] supporting key/value messages
     * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is
     *     the only required argument.
     * @param customPartitioner A serializable partitioner for assigning messages to Kafka
     *     partitions. Passing null will use Kafka's partitioner.
     */
    public FlinkKafkaProducerBase(
            String defaultTopicId,
            KeyedSerializationSchema<IN> serializationSchema,
            Properties producerConfig,
            FlinkKafkaPartitioner<IN> customPartitioner) {
        requireNonNull(defaultTopicId, "TopicID not set");
        requireNonNull(serializationSchema, "serializationSchema not set");
        requireNonNull(producerConfig, "producerConfig not set");
        ClosureCleaner.clean(
                customPartitioner, ExecutionConfig.ClosureCleanerLevel.RECURSIVE, true);
        ClosureCleaner.ensureSerializable(serializationSchema);

        this.defaultTopicId = defaultTopicId;
        this.schema = serializationSchema;
        this.producerConfig = producerConfig;
        this.flinkKafkaPartitioner = customPartitioner;

        // set the producer configuration properties for kafka record key value serializers.
        if (!producerConfig.containsKey(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)) {
            this.producerConfig.put(
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    ByteArraySerializer.class.getName());
        } else {
            LOG.warn(
                    "Overwriting the '{}' is not recommended",
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
        }

        if (!producerConfig.containsKey(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)) {
            this.producerConfig.put(
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    ByteArraySerializer.class.getName());
        } else {
            LOG.warn(
                    "Overwriting the '{}' is not recommended",
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
        }

        // eagerly ensure that bootstrap servers are set.
        if (!this.producerConfig.containsKey(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            throw new IllegalArgumentException(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
                            + " must be supplied in the producer config properties.");
        }

        this.topicPartitionsMap = new HashMap<>();
    }

    // ---------------------------------- Properties --------------------------

    /**
     * 定义生产者是否应该在错误时失败，或者只记录它们。如果设置为 true，则只记录异常，如果设置为 false，则最终会
     * 抛出异常并导致流程序失败(并进入恢复)。
     *
     * Defines whether the producer should fail on errors, or only log them. If this is set to true,
     * then exceptions will be only logged, if set to false, exceptions will be eventually thrown
     * and cause the streaming program to fail (and enter recovery).
     *
     * @param logFailuresOnly The flag to indicate logging-only on exceptions.
     */
    public void setLogFailuresOnly(boolean logFailuresOnly) {
        this.logFailuresOnly = logFailuresOnly;
    }

    /**
     * 如果设置为 true, Flink 生产者将等待 Kafka 缓冲区中所有未完成的消息在检查点被 Kafka 生产者确认。这样，
     * 生产者可以保证 Kafka 缓冲区中的消息是检查点的一部分。
     *
     * If set to true, the Flink producer will wait for all outstanding messages in the Kafka
     * buffers to be acknowledged by the Kafka producer on a checkpoint. This way, the producer can
     * guarantee that messages in the Kafka buffers are part of the checkpoint.
     *
     * @param flush Flag indicating the flushing mode (true = flush on checkpoint)
     */
    public void setFlushOnCheckpoint(boolean flush) {
        this.flushOnCheckpoint = flush;
    }

    /** Used for testing only. */
    @VisibleForTesting
    // 仅用于测试。
    protected <K, V> KafkaProducer<K, V> getKafkaProducer(Properties props) {
        return new KafkaProducer<>(props);
    }

    // ----------------------------------- Utilities --------------------------

    /** Initializes the connection to Kafka. */
    // 初始化到 Kafka 的连接。
    @Override
    public void open(Configuration configuration) throws Exception {
        if (schema instanceof KeyedSerializationSchemaWrapper) {
            ((KeyedSerializationSchemaWrapper<IN>) schema)
                    .getSerializationSchema()
                    .open(() -> getRuntimeContext().getMetricGroup().addGroup("user"));
        }
        producer = getKafkaProducer(this.producerConfig);

        RuntimeContext ctx = getRuntimeContext();

        // J: 自定义分区 open
        if (null != flinkKafkaPartitioner) {
            flinkKafkaPartitioner.open(
                    ctx.getIndexOfThisSubtask(), ctx.getNumberOfParallelSubtasks());
        }

        LOG.info(
                "Starting FlinkKafkaProducer ({}/{}) to produce into default topic {}",
                ctx.getIndexOfThisSubtask() + 1,
                ctx.getNumberOfParallelSubtasks(),
                defaultTopicId);

        // register Kafka metrics to Flink accumulators
        // 将 Kafka 指标注册到 Flink 累加器
        if (!Boolean.parseBoolean(producerConfig.getProperty(KEY_DISABLE_METRICS, "false"))) {
            // 获取到 kafka 中的 metrics
            Map<MetricName, ? extends Metric> metrics = this.producer.metrics();

            if (metrics == null) {
                // MapR's Kafka implementation returns null here.
                LOG.info("Producer implementation does not support metrics");
            } else {
                // 增加 KafkaProducer 指标组
                final MetricGroup kafkaMetricGroup =
                        getRuntimeContext().getMetricGroup().addGroup("KafkaProducer");
                // J: 将 kafka 中的 metrics 放到 flink 中
                for (Map.Entry<MetricName, ? extends Metric> metric : metrics.entrySet()) {
                    // 增加 gauge 指标
                    kafkaMetricGroup.gauge(
                            metric.getKey().name(), new KafkaMetricWrapper(metric.getValue()));
                }
            }
        }

        if (flushOnCheckpoint
                && !((StreamingRuntimeContext) this.getRuntimeContext()).isCheckpointingEnabled()) {
            // J: 不符合约定的配置警告
            LOG.warn(
                    "Flushing on checkpoint is enabled, but checkpointing is not enabled. Disabling flushing.");
            flushOnCheckpoint = false;
        }

        if (logFailuresOnly) {
            // 发送失败的时候写错误日志，不保存异步生成中的异常
            callback =
                    new Callback() {
                        @Override
                        public void onCompletion(RecordMetadata metadata, Exception e) {
                            if (e != null) {
                                LOG.error(
                                        "Error while sending record to Kafka: " + e.getMessage(),
                                        e);
                            }
                            acknowledgeMessage();
                        }
                    };
        } else {
            callback =
                    new Callback() {
                        @Override
                        public void onCompletion(RecordMetadata metadata, Exception exception) {
                            if (exception != null && asyncException == null) {
                                asyncException = exception;
                            }
                            acknowledgeMessage();
                        }
                    };
        }
    }

    /**
     * 当新的数据到达 sink 时调用，并将其转发给 Kafka。
     *
     * Called when new data arrives to the sink, and forwards it to Kafka.
     *
     * @param next The incoming data
     */
    @Override
    public void invoke(IN next, Context context) throws Exception {
        // propagate asynchronous errors
        // 异步传播错误
        checkErroneous();

        byte[] serializedKey = schema.serializeKey(next);
        byte[] serializedValue = schema.serializeValue(next);
        String targetTopic = schema.getTargetTopic(next);
        if (targetTopic == null) {
            targetTopic = defaultTopicId;
        }

        int[] partitions = this.topicPartitionsMap.get(targetTopic);
        if (null == partitions) {
            partitions = getPartitionsByTopic(targetTopic, producer);
            this.topicPartitionsMap.put(targetTopic, partitions);
        }

        ProducerRecord<byte[], byte[]> record;
        if (flinkKafkaPartitioner == null) {
            record = new ProducerRecord<>(targetTopic, serializedKey, serializedValue);
        } else {
            record =
                    new ProducerRecord<>(
                            targetTopic,
                            flinkKafkaPartitioner.partition(
                                    next, serializedKey, serializedValue, targetTopic, partitions),
                            serializedKey,
                            serializedValue);
        }
        if (flushOnCheckpoint) {
            // J: 等待的记录增长
            synchronized (pendingRecordsLock) {
                pendingRecords++;
            }
        }
        producer.send(record, callback);
    }

    @Override
    public void close() throws Exception {
        if (producer != null) {
            producer.close();
        }

        // make sure we propagate pending errors
        // 确保我们传播挂起的错误
        checkErroneous();
    }

    // ------------------- Logic for handling checkpoint flushing -------------------------- //
    // 处理检查点刷新的逻辑

    private void acknowledgeMessage() {
        if (flushOnCheckpoint) {
            synchronized (pendingRecordsLock) {
                pendingRecords--;
                if (pendingRecords == 0) {
                    // 唤醒 ...
                    pendingRecordsLock.notifyAll();
                }
            }
        }
    }

    /** Flush pending records. */
    protected abstract void flush();

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        // nothing to do
    }

    @Override
    public void snapshotState(FunctionSnapshotContext ctx) throws Exception {
        // check for asynchronous errors and fail the checkpoint if necessary
        // 检查异步错误并在必要时使检查点失败
        checkErroneous();

        if (flushOnCheckpoint) {
            // flushing is activated: We need to wait until pendingRecords is 0
            // 刷新被激活:我们需要等待，直到 pendingRecords 为 0
            flush();
            synchronized (pendingRecordsLock) {
                if (pendingRecords != 0) {
                    throw new IllegalStateException(
                            "Pending record count must be zero at this point: " + pendingRecords);
                }

                // if the flushed requests has errors, we should propagate it also and fail the
                // checkpoint
                // 如果刷新的请求有错误，我们也应该传播它并使检查点失败
                checkErroneous();
            }
        }
    }

    // ----------------------------------- Utilities --------------------------

    protected void checkErroneous() throws Exception {
        Exception e = asyncException;
        if (e != null) {
            // prevent double throwing
            asyncException = null;
            throw new Exception("Failed to send data to Kafka: " + e.getMessage(), e);
        }
    }

    // J: 根据配置 broker 验证并扩展参数
    public static Properties getPropertiesFromBrokerList(String brokerList) {
        String[] elements = brokerList.split(",");

        // validate the broker addresses
        // 验证代理地址
        for (String broker : elements) {
            NetUtils.getCorrectHostnamePort(broker);
        }

        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        return props;
    }

    protected static int[] getPartitionsByTopic(
            String topic, KafkaProducer<byte[], byte[]> producer) {
        // the fetched list is immutable, so we're creating a mutable copy in order to sort it
        // fetched list 是不可变的，所以要创建一个可变副本来排序
        List<PartitionInfo> partitionsList = new ArrayList<>(producer.partitionsFor(topic));

        // sort the partitions by partition id to make sure the fetched partition list is the same
        // across subtasks
        // 按分区 id 对分区进行排序，以确保获取的分区列表在各个子任务中是相同的
        Collections.sort(
                partitionsList,
                new Comparator<PartitionInfo>() {
                    @Override
                    public int compare(PartitionInfo o1, PartitionInfo o2) {
                        return Integer.compare(o1.partition(), o2.partition());
                    }
                });

        int[] partitions = new int[partitionsList.size()];
        for (int i = 0; i < partitions.length; i++) {
            partitions[i] = partitionsList.get(i).partition();
        }

        return partitions;
    }

    @VisibleForTesting
    protected long numPendingRecords() {
        synchronized (pendingRecordsLock) {
            return pendingRecords;
        }
    }
}
