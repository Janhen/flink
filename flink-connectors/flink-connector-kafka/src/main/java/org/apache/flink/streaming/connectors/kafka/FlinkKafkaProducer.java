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
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.SimpleTypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.base.TypeSerializerSingleton;
import org.apache.flink.api.java.ClosureCleaner;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.connectors.kafka.internal.FlinkKafkaInternalProducer;
import org.apache.flink.streaming.connectors.kafka.internal.TransactionalIdsGenerator;
import org.apache.flink.streaming.connectors.kafka.internal.metrics.KafkaMetricMutableWrapper;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaSerializationSchemaWrapper;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkFixedPartitioner;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.NetUtils;
import org.apache.flink.util.TemporaryClassLoaderContext;

import org.apache.flink.shaded.guava18.com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.errors.InvalidTxnStateException;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * Flink Sink 生成数据到 Kafka主题。默认情况下，生产者将使用 {@link FlinkKafkaProducer.Semantic#AT_LEAST_ONCE}
 * 语义。在使用 {@link FlinkKafkaProducer。请参考 Flink 的 Kafka 连接器文档。
 *
 * Flink Sink to produce data into a Kafka topic. By default producer will use {@link
 * FlinkKafkaProducer.Semantic#AT_LEAST_ONCE} semantic. Before using {@link
 * FlinkKafkaProducer.Semantic#EXACTLY_ONCE} please refer to Flink's Kafka connector documentation.
 */
@PublicEvolving
public class FlinkKafkaProducer<IN>
        extends TwoPhaseCommitSinkFunction<
                IN,
                FlinkKafkaProducer.KafkaTransactionState,
                FlinkKafkaProducer.KafkaTransactionContext> {

    /**
     * Semantics that can be chosen.
     * <li>{@link #EXACTLY_ONCE}
     * <li>{@link #AT_LEAST_ONCE}
     * <li>{@link #NONE}
     */
    public enum Semantic {

        /**
         * Semantic.EXACTLY_ONCE the Flink producer will write all messages in a Kafka transaction
         * that will be committed to Kafka on a checkpoint.
         *
         * <p>In this mode {@link FlinkKafkaProducer} sets up a pool of {@link
         * FlinkKafkaInternalProducer}. Between each checkpoint a Kafka transaction is created,
         * which is committed on {@link FlinkKafkaProducer#notifyCheckpointComplete(long)}. If
         * checkpoint complete notifications are running late, {@link FlinkKafkaProducer} can run
         * out of {@link FlinkKafkaInternalProducer}s in the pool. In that case any subsequent
         * {@link FlinkKafkaProducer#snapshotState(FunctionSnapshotContext)} requests will fail and
         * {@link FlinkKafkaProducer} will keep using the {@link FlinkKafkaInternalProducer} from
         * the previous checkpoint. To decrease the chance of failing checkpoints there are four
         * options:
         * <li>decrease number of max concurrent checkpoints
         * <li>make checkpoints more reliable (so that they complete faster)
         * <li>increase the delay between checkpoints
         * <li>increase the size of {@link FlinkKafkaInternalProducer}s pool
         */
        EXACTLY_ONCE,

        /**
         * Semantic.AT_LEAST_ONCE the Flink producer will wait for all outstanding messages in the
         * Kafka buffers to be acknowledged by the Kafka producer on a checkpoint.
         */
        AT_LEAST_ONCE,

        /**
         * Semantic.NONE means that nothing will be guaranteed. Messages can be lost and/or
         * duplicated in case of failure.
         */
        NONE
    }

    private static final Logger LOG = LoggerFactory.getLogger(FlinkKafkaProducer.class);

    private static final long serialVersionUID = 1L;

    /**
     * 这个系数决定什么是安全的缩小系数。
     *
     * <p>如果 Flink 应用程序在第一个检查点完成之前失败，或者我们正在从头开始新一批 {@link FlinkKafkaProducer}
     *    而没有干净地关闭前一个，{@link FlinkKafkaProducer} 不知道以前的集合是什么使用了 Kafka 的
     *    transactionalId。在这种情况下，它将尝试安全运行并中止以下范围内的所有可能的 transactionalId：
     *    {@code [0, getNumberOfParallelSubtasks() kafkaProducersPoolSize SAFE_SCALE_DOWN_FACTOR)}
     *
     * <p> 可以使用事务 id 的范围是：{@code [0, getNumberOfParallelSubtasks() kafkaProducersPoolSize) }
     *
     * <p> 这意味着如果我们将 {@code getNumberOfParallelSubtasks()} 减少一个大于
     *     {@code SAFE_SCALE_DOWN_FACTOR} 的因子，我们可能会留下一些挥之不去的事务。
     *
     * This coefficient determines what is the safe scale down factor.
     *
     * <p>If the Flink application previously failed before first checkpoint completed or we are
     * starting new batch of {@link FlinkKafkaProducer} from scratch without clean shutdown of the
     * previous one, {@link FlinkKafkaProducer} doesn't know what was the set of previously used
     * Kafka's transactionalId's. In that case, it will try to play safe and abort all of the
     * possible transactionalIds from the range of: {@code [0, getNumberOfParallelSubtasks() *
     * kafkaProducersPoolSize * SAFE_SCALE_DOWN_FACTOR) }
     *
     * <p>The range of available to use transactional ids is: {@code [0,
     * getNumberOfParallelSubtasks() * kafkaProducersPoolSize) }
     *
     * <p>This means that if we decrease {@code getNumberOfParallelSubtasks()} by a factor larger
     * than {@code SAFE_SCALE_DOWN_FACTOR} we can have a left some lingering transaction.
     */
    public static final int SAFE_SCALE_DOWN_FACTOR = 5;

    /**
     * 池中 KafkaProducers 的默认数量。请参阅 {@link FlinkKafkaProducer.Semantic#EXACTLY_ONCE}。
     *
     * Default number of KafkaProducers in the pool. See {@link
     * FlinkKafkaProducer.Semantic#EXACTLY_ONCE}.
     */
    public static final int DEFAULT_KAFKA_PRODUCERS_POOL_SIZE = 5;

    /** Default value for kafka transaction timeout. */
    // kafka 事务超时的默认值。
    public static final Time DEFAULT_KAFKA_TRANSACTION_TIMEOUT = Time.hours(1);

    /** Configuration key for disabling the metrics reporting. */
    // 用于禁用指标报告的配置键。
    public static final String KEY_DISABLE_METRICS = "flink.disable-metrics";

    /**
     * 事务 ID 列表的描述符。注意：此状态由 Kryo Serializer 序列化，存在兼容性问题，稍后将删除。请使用
     * NEXT_TRANSACTIONAL_ID_HINT_DESCRIPTOR_V2。
     *
     * Descriptor of the transactional IDs list. Note: This state is serialized by Kryo Serializer
     * and it has compatibility problem that will be removed later. Please use
     * NEXT_TRANSACTIONAL_ID_HINT_DESCRIPTOR_V2.
     */
    @Deprecated
    private static final ListStateDescriptor<FlinkKafkaProducer.NextTransactionalIdHint>
            NEXT_TRANSACTIONAL_ID_HINT_DESCRIPTOR =
                    new ListStateDescriptor<>(
                            "next-transactional-id-hint",
                            TypeInformation.of(NextTransactionalIdHint.class));

    private static final ListStateDescriptor<FlinkKafkaProducer.NextTransactionalIdHint>
            NEXT_TRANSACTIONAL_ID_HINT_DESCRIPTOR_V2 =
                    new ListStateDescriptor<>(
                            "next-transactional-id-hint-v2",
                            new NextTransactionalIdHintSerializer());

    /** State for nextTransactionalIdHint. */
    private transient ListState<FlinkKafkaProducer.NextTransactionalIdHint>
            nextTransactionalIdHintState;

    /** Generator for Transactional IDs. */
    private transient TransactionalIdsGenerator transactionalIdsGenerator;

    /** Hint for picking next transactional id. */
    // 提示选择下一个 transactional ID。
    private transient FlinkKafkaProducer.NextTransactionalIdHint nextTransactionalIdHint;

    /** User defined properties for the Producer. */
    protected final Properties producerConfig;

    /** The name of the default topic this producer is writing data to. */
    protected final String defaultTopicId;

    /**
     * (Serializable) SerializationSchema for turning objects used with Flink into. byte[] for
     * Kafka.
     */
    @Nullable private final KeyedSerializationSchema<IN> keyedSchema;

    /**
     * (Serializable) 序列化模式，用于将记录序列化为 {@link ProducerRecord ProducerRecords}。
     *
     * (Serializable) serialization schema for serializing records to {@link ProducerRecord
     * ProducerRecords}.
     */
    @Nullable private final KafkaSerializationSchema<IN> kafkaSchema;

    /** User-provided partitioner for assigning an object to a Kafka partition for each topic. */
    // 用户提供的分区器，用于为每个主题将对象分配给 Kafka 分区。
    @Nullable private final FlinkKafkaPartitioner<IN> flinkKafkaPartitioner;

    /** Partitions of each topic. */
    protected final Map<String, int[]> topicPartitionsMap;

    /**
     * 池中生产者的最大数量。如果所有生产者都在使用，快照状态将抛出异常。
     *
     * Max number of producers in the pool. If all producers are in use, snapshoting state will
     * throw an exception.
     */
    private final int kafkaProducersPoolSize;

    /** Pool of available transactional ids. */
    // 可用事务 ID 池。
    private final BlockingDeque<String> availableTransactionalIds = new LinkedBlockingDeque<>();

    /** Flag controlling whether we are writing the Flink record's timestamp into Kafka. */
    // 控制我们是否将 Flink 记录的时间戳写入 Kafka 的标志。
    protected boolean writeTimestampToKafka = false;

    /** Flag indicating whether to accept failures (and log them), or to fail on failures. */
    // 指示是否接受失败（并记录它们）或失败时失败的标志。
    private boolean logFailuresOnly;

    /** Semantic chosen for this instance. */
    // 为此实例选择的语义。
    protected FlinkKafkaProducer.Semantic semantic;

    // -------------------------------- Runtime fields ------------------------------------------

    /** The callback than handles error propagation or logging callbacks. */
    // 回调比处理错误传播或日志回调。
    @Nullable protected transient Callback callback;

    /** Errors encountered in the async producer are stored here. */
    // 异步生产者中遇到的错误存储在这里
    @Nullable protected transient volatile Exception asyncException;

    /** Number of unacknowledged records. */
    // 未确认的记录数。
    protected final AtomicLong pendingRecords = new AtomicLong();

    /**
     * 缓存指标以替换已注册的指标，而不是覆盖现有指标。
     *
     * Cache of metrics to replace already registered metrics instead of overwriting existing ones.
     */
    private final Map<String, KafkaMetricMutableWrapper> previouslyCreatedMetrics = new HashMap<>();

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces a DataStream to the topic.
     *
     * @param brokerList Comma separated addresses of the brokers
     * @param topicId ID of the Kafka topic.
     * @param serializationSchema User defined (keyless) serialization schema.
     */
    public FlinkKafkaProducer(
            String brokerList, String topicId, SerializationSchema<IN> serializationSchema) {
        this(topicId, serializationSchema, getPropertiesFromBrokerList(brokerList));
    }

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces a DataStream to the topic.
     *
     * <p>Using this constructor, the default {@link FlinkFixedPartitioner} will be used as the
     * partitioner. This default partitioner maps each sink subtask to a single Kafka partition
     * (i.e. all records received by a sink subtask will end up in the same Kafka partition).
     *
     * <p>To use a custom partitioner, please use {@link #FlinkKafkaProducer(String,
     * SerializationSchema, Properties, Optional)} instead.
     *
     * @param topicId ID of the Kafka topic.
     * @param serializationSchema User defined key-less serialization schema.
     * @param producerConfig Properties with the producer configuration.
     */
    public FlinkKafkaProducer(
            String topicId,
            SerializationSchema<IN> serializationSchema,
            Properties producerConfig) {
        this(
                topicId,
                serializationSchema,
                producerConfig,
                Optional.of(new FlinkFixedPartitioner<>()));
    }

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces its input to the topic. It
     * accepts a key-less {@link SerializationSchema} and possibly a custom {@link
     * FlinkKafkaPartitioner}.
     *
     * <p>Since a key-less {@link SerializationSchema} is used, all records sent to Kafka will not
     * have an attached key. Therefore, if a partitioner is also not provided, records will be
     * distributed to Kafka partitions in a round-robin fashion.
     *
     * @param topicId The topic to write data to
     * @param serializationSchema A key-less serializable serialization schema for turning user
     *     objects into a kafka-consumable byte[]
     * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is
     *     the only required argument.
     * @param customPartitioner A serializable partitioner for assigning messages to Kafka
     *     partitions. If a partitioner is not provided, records will be distributed to Kafka
     *     partitions in a round-robin fashion.
     */
    public FlinkKafkaProducer(
            String topicId,
            SerializationSchema<IN> serializationSchema,
            Properties producerConfig,
            Optional<FlinkKafkaPartitioner<IN>> customPartitioner) {
        this(
                topicId,
                serializationSchema,
                producerConfig,
                customPartitioner.orElse(null),
                Semantic.AT_LEAST_ONCE,
                DEFAULT_KAFKA_PRODUCERS_POOL_SIZE);
    }

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces its input to the topic. It
     * accepts a key-less {@link SerializationSchema} and possibly a custom {@link
     * FlinkKafkaPartitioner}.
     *
     * <p>Since a key-less {@link SerializationSchema} is used, all records sent to Kafka will not
     * have an attached key. Therefore, if a partitioner is also not provided, records will be
     * distributed to Kafka partitions in a round-robin fashion.
     *
     * @param topicId The topic to write data to
     * @param serializationSchema A key-less serializable serialization schema for turning user
     *     objects into a kafka-consumable byte[]
     * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is
     *     the only required argument.
     * @param customPartitioner A serializable partitioner for assigning messages to Kafka
     *     partitions. If a partitioner is not provided, records will be distributed to Kafka
     *     partitions in a round-robin fashion.
     * @param semantic Defines semantic that will be used by this producer (see {@link
     *     FlinkKafkaProducer.Semantic}).
     * @param kafkaProducersPoolSize Overwrite default KafkaProducers pool size (see {@link
     *     FlinkKafkaProducer.Semantic#EXACTLY_ONCE}).
     */
    public FlinkKafkaProducer(
            String topicId,
            SerializationSchema<IN> serializationSchema,
            Properties producerConfig,
            @Nullable FlinkKafkaPartitioner<IN> customPartitioner,
            FlinkKafkaProducer.Semantic semantic,
            int kafkaProducersPoolSize) {
        this(
                topicId,
                null,
                null,
                new KafkaSerializationSchemaWrapper<>(
                        topicId, customPartitioner, false, serializationSchema),
                producerConfig,
                semantic,
                kafkaProducersPoolSize);
    }

    // ------------------- Key/Value serialization schema constructors ----------------------

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces a DataStream to the topic.
     *
     * <p>Using this constructor, the default {@link FlinkFixedPartitioner} will be used as the
     * partitioner. This default partitioner maps each sink subtask to a single Kafka partition
     * (i.e. all records received by a sink subtask will end up in the same Kafka partition).
     *
     * <p>To use a custom partitioner, please use {@link #FlinkKafkaProducer(String,
     * KeyedSerializationSchema, Properties, Optional)} instead.
     *
     * @param brokerList Comma separated addresses of the brokers
     * @param topicId ID of the Kafka topic.
     * @param serializationSchema User defined serialization schema supporting key/value messages
     * @deprecated use {@link #FlinkKafkaProducer(String, KafkaSerializationSchema, Properties,
     *     FlinkKafkaProducer.Semantic)}
     */
    @Deprecated
    public FlinkKafkaProducer(
            String brokerList, String topicId, KeyedSerializationSchema<IN> serializationSchema) {
        this(
                topicId,
                serializationSchema,
                getPropertiesFromBrokerList(brokerList),
                Optional.of(new FlinkFixedPartitioner<IN>()));
    }

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces a DataStream to the topic.
     *
     * <p>Using this constructor, the default {@link FlinkFixedPartitioner} will be used as the
     * partitioner. This default partitioner maps each sink subtask to a single Kafka partition
     * (i.e. all records received by a sink subtask will end up in the same Kafka partition).
     *
     * <p>To use a custom partitioner, please use {@link #FlinkKafkaProducer(String,
     * KeyedSerializationSchema, Properties, Optional)} instead.
     *
     * @param topicId ID of the Kafka topic.
     * @param serializationSchema User defined serialization schema supporting key/value messages
     * @param producerConfig Properties with the producer configuration.
     * @deprecated use {@link #FlinkKafkaProducer(String, KafkaSerializationSchema, Properties,
     *     FlinkKafkaProducer.Semantic)}
     */
    @Deprecated
    public FlinkKafkaProducer(
            String topicId,
            KeyedSerializationSchema<IN> serializationSchema,
            Properties producerConfig) {
        this(
                topicId,
                serializationSchema,
                producerConfig,
                Optional.of(new FlinkFixedPartitioner<IN>()));
    }

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces a DataStream to the topic.
     *
     * <p>Using this constructor, the default {@link FlinkFixedPartitioner} will be used as the
     * partitioner. This default partitioner maps each sink subtask to a single Kafka partition
     * (i.e. all records received by a sink subtask will end up in the same Kafka partition).
     *
     * @param topicId ID of the Kafka topic.
     * @param serializationSchema User defined serialization schema supporting key/value messages
     * @param producerConfig Properties with the producer configuration.
     * @param semantic Defines semantic that will be used by this producer (see {@link
     *     FlinkKafkaProducer.Semantic}).
     * @deprecated use {@link #FlinkKafkaProducer(String, KafkaSerializationSchema, Properties,
     *     FlinkKafkaProducer.Semantic)}
     */
    @Deprecated
    public FlinkKafkaProducer(
            String topicId,
            KeyedSerializationSchema<IN> serializationSchema,
            Properties producerConfig,
            FlinkKafkaProducer.Semantic semantic) {
        this(
                topicId,
                serializationSchema,
                producerConfig,
                Optional.of(new FlinkFixedPartitioner<IN>()),
                semantic,
                DEFAULT_KAFKA_PRODUCERS_POOL_SIZE);
    }

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces its input to the topic. It
     * accepts a keyed {@link KeyedSerializationSchema} and possibly a custom {@link
     * FlinkKafkaPartitioner}.
     *
     * <p>If a partitioner is not provided, written records will be partitioned by the attached key
     * of each record (as determined by {@link KeyedSerializationSchema#serializeKey(Object)}). If
     * written records do not have a key (i.e., {@link
     * KeyedSerializationSchema#serializeKey(Object)} returns {@code null}), they will be
     * distributed to Kafka partitions in a round-robin fashion.
     *
     * @param defaultTopicId The default topic to write data to
     * @param serializationSchema A serializable serialization schema for turning user objects into
     *     a kafka-consumable byte[] supporting key/value messages
     * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is
     *     the only required argument.
     * @param customPartitioner A serializable partitioner for assigning messages to Kafka
     *     partitions. If a partitioner is not provided, records will be partitioned by the key of
     *     each record (determined by {@link KeyedSerializationSchema#serializeKey(Object)}). If the
     *     keys are {@code null}, then records will be distributed to Kafka partitions in a
     *     round-robin fashion.
     * @deprecated use {@link #FlinkKafkaProducer(String, KafkaSerializationSchema, Properties,
     *     FlinkKafkaProducer.Semantic)}
     */
    @Deprecated
    public FlinkKafkaProducer(
            String defaultTopicId,
            KeyedSerializationSchema<IN> serializationSchema,
            Properties producerConfig,
            Optional<FlinkKafkaPartitioner<IN>> customPartitioner) {
        this(
                defaultTopicId,
                serializationSchema,
                producerConfig,
                customPartitioner,
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE,
                DEFAULT_KAFKA_PRODUCERS_POOL_SIZE);
    }

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces its input to the topic. It
     * accepts a keyed {@link KeyedSerializationSchema} and possibly a custom {@link
     * FlinkKafkaPartitioner}.
     *
     * <p>If a partitioner is not provided, written records will be partitioned by the attached key
     * of each record (as determined by {@link KeyedSerializationSchema#serializeKey(Object)}). If
     * written records do not have a key (i.e., {@link
     * KeyedSerializationSchema#serializeKey(Object)} returns {@code null}), they will be
     * distributed to Kafka partitions in a round-robin fashion.
     *
     * @param defaultTopicId The default topic to write data to
     * @param serializationSchema A serializable serialization schema for turning user objects into
     *     a kafka-consumable byte[] supporting key/value messages
     * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is
     *     the only required argument.
     * @param customPartitioner A serializable partitioner for assigning messages to Kafka
     *     partitions. If a partitioner is not provided, records will be partitioned by the key of
     *     each record (determined by {@link KeyedSerializationSchema#serializeKey(Object)}). If the
     *     keys are {@code null}, then records will be distributed to Kafka partitions in a
     *     round-robin fashion.
     * @param semantic Defines semantic that will be used by this producer (see {@link
     *     FlinkKafkaProducer.Semantic}).
     * @param kafkaProducersPoolSize Overwrite default KafkaProducers pool size (see {@link
     *     FlinkKafkaProducer.Semantic#EXACTLY_ONCE}).
     * @deprecated use {@link #FlinkKafkaProducer(String, KafkaSerializationSchema, Properties,
     *     FlinkKafkaProducer.Semantic)}
     */
    @Deprecated
    public FlinkKafkaProducer(
            String defaultTopicId,
            KeyedSerializationSchema<IN> serializationSchema,
            Properties producerConfig,
            Optional<FlinkKafkaPartitioner<IN>> customPartitioner,
            FlinkKafkaProducer.Semantic semantic,
            int kafkaProducersPoolSize) {
        this(
                defaultTopicId,
                serializationSchema,
                customPartitioner.orElse(null),
                null, /* kafka serialization schema */
                producerConfig,
                semantic,
                kafkaProducersPoolSize);
    }

    /**
     * Creates a {@link FlinkKafkaProducer} for a given topic. The sink produces its input to the
     * topic. It accepts a {@link KafkaSerializationSchema} for serializing records to a {@link
     * ProducerRecord}, including partitioning information.
     *
     * @param defaultTopic The default topic to write data to
     * @param serializationSchema A serializable serialization schema for turning user objects into
     *     a kafka-consumable byte[] supporting key/value messages
     * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is
     *     the only required argument.
     * @param semantic Defines semantic that will be used by this producer (see {@link
     *     FlinkKafkaProducer.Semantic}).
     */
    public FlinkKafkaProducer(
            String defaultTopic,
            KafkaSerializationSchema<IN> serializationSchema,
            Properties producerConfig,
            FlinkKafkaProducer.Semantic semantic) {
        this(
                defaultTopic,
                serializationSchema,
                producerConfig,
                semantic,
                DEFAULT_KAFKA_PRODUCERS_POOL_SIZE);
    }

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces its input to the topic. It
     * accepts a {@link KafkaSerializationSchema} and possibly a custom {@link
     * FlinkKafkaPartitioner}.
     *
     * @param defaultTopic The default topic to write data to
     * @param serializationSchema A serializable serialization schema for turning user objects into
     *     a kafka-consumable byte[] supporting key/value messages
     * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is
     *     the only required argument.
     * @param semantic Defines semantic that will be used by this producer (see {@link
     *     FlinkKafkaProducer.Semantic}).
     * @param kafkaProducersPoolSize Overwrite default KafkaProducers pool size (see {@link
     *     FlinkKafkaProducer.Semantic#EXACTLY_ONCE}).
     */
    public FlinkKafkaProducer(
            String defaultTopic,
            KafkaSerializationSchema<IN> serializationSchema,
            Properties producerConfig,
            FlinkKafkaProducer.Semantic semantic,
            int kafkaProducersPoolSize) {
        this(
                defaultTopic,
                null,
                null, /* keyed schema and FlinkKafkaPartitioner */
                serializationSchema,
                producerConfig,
                semantic,
                kafkaProducersPoolSize);
    }

    /**
     * Creates a FlinkKafkaProducer for a given topic. The sink produces its input to the topic. It
     * accepts a {@link KafkaSerializationSchema} and possibly a custom {@link
     * FlinkKafkaPartitioner}.
     *
     * <p>If a partitioner is not provided, written records will be partitioned by the attached key
     * of each record (as determined by {@link KeyedSerializationSchema#serializeKey(Object)}). If
     * written records do not have a key (i.e., {@link
     * KeyedSerializationSchema#serializeKey(Object)} returns {@code null}), they will be
     * distributed to Kafka partitions in a round-robin fashion.
     *
     * @param defaultTopic The default topic to write data to
     * @param keyedSchema A serializable serialization schema for turning user objects into a
     *     kafka-consumable byte[] supporting key/value messages
     * @param customPartitioner A serializable partitioner for assigning messages to Kafka
     *     partitions. If a partitioner is not provided, records will be partitioned by the key of
     *     each record (determined by {@link KeyedSerializationSchema#serializeKey(Object)}). If the
     *     keys are {@code null}, then records will be distributed to Kafka partitions in a
     *     round-robin fashion.
     * @param kafkaSchema A serializable serialization schema for turning user objects into a
     *     kafka-consumable byte[] supporting key/value messages
     * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is
     *     the only required argument.
     * @param semantic Defines semantic that will be used by this producer (see {@link
     *     FlinkKafkaProducer.Semantic}).
     * @param kafkaProducersPoolSize Overwrite default KafkaProducers pool size (see {@link
     *     FlinkKafkaProducer.Semantic#EXACTLY_ONCE}).
     */
    private FlinkKafkaProducer(
            String defaultTopic,
            KeyedSerializationSchema<IN> keyedSchema,
            FlinkKafkaPartitioner<IN> customPartitioner,
            KafkaSerializationSchema<IN> kafkaSchema,
            Properties producerConfig,
            FlinkKafkaProducer.Semantic semantic,
            int kafkaProducersPoolSize) {
        super(
                new FlinkKafkaProducer.TransactionStateSerializer(),
                new FlinkKafkaProducer.ContextStateSerializer());

        this.defaultTopicId = checkNotNull(defaultTopic, "defaultTopic is null");

        if (kafkaSchema != null) {
            this.keyedSchema = null;
            this.kafkaSchema = kafkaSchema;
            this.flinkKafkaPartitioner = null;
            ClosureCleaner.clean(
                    this.kafkaSchema, ExecutionConfig.ClosureCleanerLevel.RECURSIVE, true);

            if (customPartitioner != null) {
                throw new IllegalArgumentException(
                        "Customer partitioner can only be used when"
                                + "using a KeyedSerializationSchema or SerializationSchema.");
            }
        } else if (keyedSchema != null) {
            this.kafkaSchema = null;
            this.keyedSchema = keyedSchema;
            this.flinkKafkaPartitioner = customPartitioner;
            ClosureCleaner.clean(
                    this.flinkKafkaPartitioner,
                    ExecutionConfig.ClosureCleanerLevel.RECURSIVE,
                    true);
            ClosureCleaner.clean(
                    this.keyedSchema, ExecutionConfig.ClosureCleanerLevel.RECURSIVE, true);
        } else {
            throw new IllegalArgumentException(
                    "You must provide either a KafkaSerializationSchema or a"
                            + "KeyedSerializationSchema.");
        }

        this.producerConfig = checkNotNull(producerConfig, "producerConfig is null");
        this.semantic = checkNotNull(semantic, "semantic is null");
        this.kafkaProducersPoolSize = kafkaProducersPoolSize;
        checkState(kafkaProducersPoolSize > 0, "kafkaProducersPoolSize must be non empty");

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

        if (!producerConfig.containsKey(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG)) {
            long timeout = DEFAULT_KAFKA_TRANSACTION_TIMEOUT.toMilliseconds();
            checkState(
                    timeout < Integer.MAX_VALUE && timeout > 0,
                    "timeout does not fit into 32 bit integer");
            this.producerConfig.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, (int) timeout);
            LOG.warn(
                    "Property [{}] not specified. Setting it to {}",
                    ProducerConfig.TRANSACTION_TIMEOUT_CONFIG,
                    DEFAULT_KAFKA_TRANSACTION_TIMEOUT);
        }

        // Enable transactionTimeoutWarnings to avoid silent data loss
        // See KAFKA-6119 (affects versions 0.11.0.0 and 0.11.0.1):
        // The KafkaProducer may not throw an exception if the transaction failed to commit
        if (semantic == FlinkKafkaProducer.Semantic.EXACTLY_ONCE) {
            final Object object =
                    this.producerConfig.get(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG);
            final long transactionTimeout;
            if (object instanceof String && StringUtils.isNumeric((String) object)) {
                transactionTimeout = Long.parseLong((String) object);
            } else if (object instanceof Number) {
                transactionTimeout = ((Number) object).longValue();
            } else {
                throw new IllegalArgumentException(
                        ProducerConfig.TRANSACTION_TIMEOUT_CONFIG
                                + " must be numeric, was "
                                + object);
            }
            super.setTransactionTimeout(transactionTimeout);
            super.enableTransactionTimeoutWarnings(0.8);
        }

        this.topicPartitionsMap = new HashMap<>();
    }

    // ---------------------------------- Properties --------------------------

    /**
     * 如果设置为 true，Flink 会将附加到每条记录的（事件时间）时间戳写入 Kafka。时间戳必须为​​正数，Kafka 才能接受它们。
     *
     * If set to true, Flink will write the (event time) timestamp attached to each record into
     * Kafka. Timestamps must be positive for Kafka to accept them.
     *
     * @param writeTimestampToKafka Flag indicating if Flink's internal timestamps are written to
     *     Kafka.
     */
    public void setWriteTimestampToKafka(boolean writeTimestampToKafka) {
        this.writeTimestampToKafka = writeTimestampToKafka;
        if (kafkaSchema instanceof KafkaSerializationSchemaWrapper) {
            ((KafkaSerializationSchemaWrapper<IN>) kafkaSchema)
                    .setWriteTimestamp(writeTimestampToKafka);
        }
    }

    /**
     * 定义生产者是否应该因错误而失败，或者只记录它们。如果设置为 true，则仅记录异常，如果设置为 false，则最终将抛
     * 出异常并导致流程序失败（并进入恢复）。
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
     * 禁用在作业恢复期间提交可能超时的 Kafka 事务时引发的异常传播。如果 Kafka 事务超时，则提交永远不会成功。因此，
     * 使用此功能可以避免作业的恢复循环。仍将记录异常以通知用户可能已发生数据丢失。
     *
     * <p> 请注意，我们使用 {@link System#currentTimeMillis()} 来跟踪交易的年龄。此外，只有在恢复期间抛出的
     * 异常才会被捕获，即生产者在放弃之前至少会尝试一次事务提交。
     *
     * Disables the propagation of exceptions thrown when committing presumably timed out Kafka
     * transactions during recovery of the job. If a Kafka transaction is timed out, a commit will
     * never be successful. Hence, use this feature to avoid recovery loops of the Job. Exceptions
     * will still be logged to inform the user that data loss might have occurred.
     *
     * <p>Note that we use {@link System#currentTimeMillis()} to track the age of a transaction.
     * Moreover, only exceptions thrown during the recovery are caught, i.e., the producer will
     * attempt at least one commit of the transaction before giving up.
     */
    @Override
    public FlinkKafkaProducer<IN> ignoreFailuresAfterTransactionTimeout() {
        super.ignoreFailuresAfterTransactionTimeout();
        return this;
    }

    // ----------------------------------- Utilities --------------------------

    /** Initializes the connection to Kafka. */
    @Override
    public void open(Configuration configuration) throws Exception {
        if (logFailuresOnly) {
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

        RuntimeContext ctx = getRuntimeContext();

        if (flinkKafkaPartitioner != null) {
            flinkKafkaPartitioner.open(
                    ctx.getIndexOfThisSubtask(), ctx.getNumberOfParallelSubtasks());
        }

        if (kafkaSchema instanceof KafkaContextAware) {
            KafkaContextAware<IN> contextAwareSchema = (KafkaContextAware<IN>) kafkaSchema;
            contextAwareSchema.setParallelInstanceId(ctx.getIndexOfThisSubtask());
            contextAwareSchema.setNumParallelInstances(ctx.getNumberOfParallelSubtasks());
        }

        if (kafkaSchema != null) {
            kafkaSchema.open(() -> ctx.getMetricGroup().addGroup("user"));
        }

        super.open(configuration);
    }

    @Override
    public void invoke(
            FlinkKafkaProducer.KafkaTransactionState transaction, IN next, Context context)
            throws FlinkKafkaException {
        checkErroneous();

        ProducerRecord<byte[], byte[]> record;
        if (keyedSchema != null) {
            byte[] serializedKey = keyedSchema.serializeKey(next);
            byte[] serializedValue = keyedSchema.serializeValue(next);
            String targetTopic = keyedSchema.getTargetTopic(next);
            if (targetTopic == null) {
                targetTopic = defaultTopicId;
            }

            Long timestamp = null;
            if (this.writeTimestampToKafka) {
                timestamp = context.timestamp();
            }

            int[] partitions = topicPartitionsMap.get(targetTopic);
            if (null == partitions) {
                partitions = getPartitionsByTopic(targetTopic, transaction.producer);
                topicPartitionsMap.put(targetTopic, partitions);
            }
            if (flinkKafkaPartitioner != null) {
                record =
                        new ProducerRecord<>(
                                targetTopic,
                                flinkKafkaPartitioner.partition(
                                        next,
                                        serializedKey,
                                        serializedValue,
                                        targetTopic,
                                        partitions),
                                timestamp,
                                serializedKey,
                                serializedValue);
            } else {
                record =
                        new ProducerRecord<>(
                                targetTopic, null, timestamp, serializedKey, serializedValue);
            }
        } else if (kafkaSchema != null) {
            if (kafkaSchema instanceof KafkaContextAware) {
                @SuppressWarnings("unchecked")
                KafkaContextAware<IN> contextAwareSchema = (KafkaContextAware<IN>) kafkaSchema;

                String targetTopic = contextAwareSchema.getTargetTopic(next);
                if (targetTopic == null) {
                    targetTopic = defaultTopicId;
                }
                int[] partitions = topicPartitionsMap.get(targetTopic);

                if (null == partitions) {
                    partitions = getPartitionsByTopic(targetTopic, transaction.producer);
                    topicPartitionsMap.put(targetTopic, partitions);
                }

                contextAwareSchema.setPartitions(partitions);
            }
            record = kafkaSchema.serialize(next, context.timestamp());
        } else {
            throw new RuntimeException(
                    "We have neither KafkaSerializationSchema nor KeyedSerializationSchema, this"
                            + "is a bug.");
        }

        // J: 未确认的记录数 ++
        pendingRecords.incrementAndGet();
        transaction.producer.send(record, callback);
    }

    @Override
    public void close() throws FlinkKafkaException {
        // First close the producer for current transaction.
        // 首先关闭当前事务的生产者。
        try {
            final KafkaTransactionState currentTransaction = currentTransaction();
            if (currentTransaction != null) {
                // to avoid exceptions on aborting transactions with some pending records
                // 避免在中止具有某些待处理记录的事务时出现异常
                flush(currentTransaction);

                // normal abort for AT_LEAST_ONCE and NONE do not clean up resources because of
                // producer reusing, thus
                // we need to close it manually
                // 由于生产者重用，AT_LEAST_ONCE 和 NONE 的正常 abort 不会清理资源，因此我们需要手动关闭它
                switch (semantic) {
                    case EXACTLY_ONCE:
                        break;
                    case AT_LEAST_ONCE:
                    case NONE:
                        currentTransaction.producer.flush();
                        currentTransaction.producer.close(Duration.ofSeconds(0));
                        break;
                }
            }
            super.close();
        } catch (Exception e) {
            asyncException = ExceptionUtils.firstOrSuppressed(e, asyncException);
        } finally {
            // We may have to close producer of the current transaction in case some exception was
            // thrown before
            // the normal close routine finishes.
            // 我们可能不得不关闭当前事务的生产者，以防在正常关闭例程完成之前抛出一些异常。
            if (currentTransaction() != null) {
                try {
                    currentTransaction().producer.close(Duration.ofSeconds(0));
                } catch (Throwable t) {
                    LOG.warn("Error closing producer.", t);
                }
            }
            // Make sure all the producers for pending transactions are closed.
            pendingTransactions()
                    .forEach(
                            transaction -> {
                                try {
                                    transaction.getValue().producer.close(Duration.ofSeconds(0));
                                } catch (Throwable t) {
                                    LOG.warn("Error closing producer.", t);
                                }
                            });
            // make sure we propagate pending errors
            // 确保我们传播未决错误
            checkErroneous();
        }
    }

    // ------------------- Logic for handling checkpoint flushing -------------------------- //

    @Override
    protected FlinkKafkaProducer.KafkaTransactionState beginTransaction()
            throws FlinkKafkaException {
        switch (semantic) {
            case EXACTLY_ONCE:
                // J: 开启 kafka producer 自己的事务
                FlinkKafkaInternalProducer<byte[], byte[]> producer = createTransactionalProducer();
                producer.beginTransaction();
                return new FlinkKafkaProducer.KafkaTransactionState(
                        producer.getTransactionalId(), producer);
            case AT_LEAST_ONCE:
            case NONE:
                // Do not create new producer on each beginTransaction() if it is not necessary
                // 如果没有必要，不要在每个 beginTransaction() 上创建新的生产者
                final FlinkKafkaProducer.KafkaTransactionState currentTransaction =
                        currentTransaction();
                if (currentTransaction != null && currentTransaction.producer != null) {
                    return new FlinkKafkaProducer.KafkaTransactionState(
                            currentTransaction.producer);
                }
                return new FlinkKafkaProducer.KafkaTransactionState(
                        initNonTransactionalProducer(true));
            default:
                throw new UnsupportedOperationException("Not implemented semantic");
        }
    }

    @Override
    protected void preCommit(FlinkKafkaProducer.KafkaTransactionState transaction)
            throws FlinkKafkaException {
        switch (semantic) {
            case EXACTLY_ONCE:
            case AT_LEAST_ONCE:
                flush(transaction);
                break;
            case NONE:
                break;
            default:
                throw new UnsupportedOperationException("Not implemented semantic");
        }
        checkErroneous();
    }

    @Override
    protected void commit(FlinkKafkaProducer.KafkaTransactionState transaction) {
        // J: 是否开启事务，不同语义 ...
        if (transaction.isTransactional()) {
            try {
                transaction.producer.commitTransaction();
            } finally {
                // J: 未开启事务时，回收事务 Producer
                recycleTransactionalProducer(transaction.producer);
            }
        }
    }

    @Override
    protected void recoverAndCommit(FlinkKafkaProducer.KafkaTransactionState transaction) {
        if (transaction.isTransactional()) {
            FlinkKafkaInternalProducer<byte[], byte[]> producer = null;
            try {
                producer = initTransactionalProducer(transaction.transactionalId, false);
                producer.resumeTransaction(transaction.producerId, transaction.epoch);
                producer.commitTransaction();
            } catch (InvalidTxnStateException | ProducerFencedException ex) {
                // That means we have committed this transaction before.
                LOG.warn(
                        "Encountered error {} while recovering transaction {}. "
                                + "Presumably this transaction has been already committed before",
                        ex,
                        transaction);
            } finally {
                if (producer != null) {
                    producer.close(0, TimeUnit.SECONDS);
                }
            }
        }
    }

    @Override
    protected void abort(FlinkKafkaProducer.KafkaTransactionState transaction) {
        if (transaction.isTransactional()) {
            transaction.producer.abortTransaction();
            recycleTransactionalProducer(transaction.producer);
        }
    }

    @Override
    protected void recoverAndAbort(FlinkKafkaProducer.KafkaTransactionState transaction) {
        if (transaction.isTransactional()) {
            FlinkKafkaInternalProducer<byte[], byte[]> producer = null;
            try {
                producer = initTransactionalProducer(transaction.transactionalId, false);
                producer.initTransactions();
            } finally {
                if (producer != null) {
                    producer.close(0, TimeUnit.SECONDS);
                }
            }
        }
    }

    /**
     * <b>注意子类实现者：<b>在覆盖此方法时，请始终调用 {@code super.acknowledgeMes​​sage()} 以保持生产者内部
     * 簿记的不变量。如果没有，一定要知道你在做什么。
     *
     * <b>ATTENTION to subclass implementors:</b> When overriding this method, please always call
     * {@code super.acknowledgeMessage()} to keep the invariants of the internal bookkeeping of the
     * producer. If not, be sure to know what you are doing.
     */
    protected void acknowledgeMessage() {
        pendingRecords.decrementAndGet();
    }

    /**
     * 刷新挂起的记录。
     *
     * Flush pending records.
     *
     * @param transaction
     */
    private void flush(FlinkKafkaProducer.KafkaTransactionState transaction)
            throws FlinkKafkaException {
        if (transaction.producer != null) {
            transaction.producer.flush();
        }
        long pendingRecordsCount = pendingRecords.get();
        if (pendingRecordsCount != 0) {
            throw new IllegalStateException(
                    "Pending record count must be zero at this point: " + pendingRecordsCount);
        }

        // if the flushed requests has errors, we should propagate it also and fail the checkpoint
        checkErroneous();
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        super.snapshotState(context);

        nextTransactionalIdHintState.clear();
        // To avoid duplication only first subtask keeps track of next transactional id hint.
        // Otherwise all of the
        // subtasks would write exactly same information.
        // 为了避免重复，只有第一个子任务会跟踪下一个事务 ID 提示。否则，所有子任务都会写入完全相同的信息
        if (getRuntimeContext().getIndexOfThisSubtask() == 0
                && semantic == FlinkKafkaProducer.Semantic.EXACTLY_ONCE) {
            checkState(
                    nextTransactionalIdHint != null,
                    "nextTransactionalIdHint must be set for EXACTLY_ONCE");
            long nextFreeTransactionalId = nextTransactionalIdHint.nextFreeTransactionalId;

            // If we scaled up, some (unknown) subtask must have created new transactional ids from
            // scratch. In that
            // case we adjust nextFreeTransactionalId by the range of transactionalIds that could be
            // used for this
            // scaling up.
            // 如果我们扩大规模，某些（未知）子任务必须从头开始创建新的事务 ID。在这种情况下，我们根据可用于此扩展的
            // transactionalId 范围调整 nextFreeTransactionalId。
            if (getRuntimeContext().getNumberOfParallelSubtasks()
                    > nextTransactionalIdHint.lastParallelism) {
                nextFreeTransactionalId +=
                        getRuntimeContext().getNumberOfParallelSubtasks() * kafkaProducersPoolSize;
            }

            nextTransactionalIdHintState.add(
                    new FlinkKafkaProducer.NextTransactionalIdHint(
                            getRuntimeContext().getNumberOfParallelSubtasks(),
                            nextFreeTransactionalId));
        }
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        if (semantic != FlinkKafkaProducer.Semantic.NONE
                && !((StreamingRuntimeContext) this.getRuntimeContext()).isCheckpointingEnabled()) {
            LOG.warn(
                    "Using {} semantic, but checkpointing is not enabled. Switching to {} semantic.",
                    semantic,
                    FlinkKafkaProducer.Semantic.NONE);
            semantic = FlinkKafkaProducer.Semantic.NONE;
        }

        nextTransactionalIdHintState =
                context.getOperatorStateStore()
                        .getUnionListState(NEXT_TRANSACTIONAL_ID_HINT_DESCRIPTOR_V2);

        if (context.getOperatorStateStore()
                .getRegisteredStateNames()
                .contains(NEXT_TRANSACTIONAL_ID_HINT_DESCRIPTOR)) {
            migrateNextTransactionalIdHindState(context);
        }

        transactionalIdsGenerator =
                new TransactionalIdsGenerator(
                        getRuntimeContext().getTaskName()
                                + "-"
                                + ((StreamingRuntimeContext) getRuntimeContext())
                                        .getOperatorUniqueID(),
                        getRuntimeContext().getIndexOfThisSubtask(),
                        getRuntimeContext().getNumberOfParallelSubtasks(),
                        kafkaProducersPoolSize,
                        SAFE_SCALE_DOWN_FACTOR);

        if (semantic != FlinkKafkaProducer.Semantic.EXACTLY_ONCE) {
            nextTransactionalIdHint = null;
        } else {
            ArrayList<FlinkKafkaProducer.NextTransactionalIdHint> transactionalIdHints =
                    Lists.newArrayList(nextTransactionalIdHintState.get());
            if (transactionalIdHints.size() > 1) {
                throw new IllegalStateException(
                        "There should be at most one next transactional id hint written by the first subtask");
            } else if (transactionalIdHints.size() == 0) {
                nextTransactionalIdHint = new FlinkKafkaProducer.NextTransactionalIdHint(0, 0);

                // this means that this is either:
                // (1) the first execution of this application
                // (2) previous execution has failed before first checkpoint completed
                //
                // in case of (2) we have to abort all previous transactions
                abortTransactions(transactionalIdsGenerator.generateIdsToAbort());
            } else {
                nextTransactionalIdHint = transactionalIdHints.get(0);
            }
        }

        super.initializeState(context);
    }

    @Override
    protected Optional<FlinkKafkaProducer.KafkaTransactionContext> initializeUserContext() {
        if (semantic != FlinkKafkaProducer.Semantic.EXACTLY_ONCE) {
            return Optional.empty();
        }

        Set<String> transactionalIds = generateNewTransactionalIds();
        resetAvailableTransactionalIdsPool(transactionalIds);
        return Optional.of(new FlinkKafkaProducer.KafkaTransactionContext(transactionalIds));
    }

    private Set<String> generateNewTransactionalIds() {
        checkState(
                nextTransactionalIdHint != null,
                "nextTransactionalIdHint must be present for EXACTLY_ONCE");

        Set<String> transactionalIds =
                transactionalIdsGenerator.generateIdsToUse(
                        nextTransactionalIdHint.nextFreeTransactionalId);
        LOG.info("Generated new transactionalIds {}", transactionalIds);
        return transactionalIds;
    }

    @Override
    protected void finishRecoveringContext(
            Collection<FlinkKafkaProducer.KafkaTransactionState> handledTransactions) {
        cleanUpUserContext(handledTransactions);
        resetAvailableTransactionalIdsPool(getUserContext().get().transactionalIds);
        LOG.info("Recovered transactionalIds {}", getUserContext().get().transactionalIds);
    }

    protected FlinkKafkaInternalProducer<byte[], byte[]> createProducer() {
        return new FlinkKafkaInternalProducer<>(this.producerConfig);
    }

    /**
     * 初始化后，确保来自当前用户上下文的所有先前事务都已完成。
     *
     * After initialization make sure that all previous transactions from the current user context
     * have been completed.
     *
     * @param handledTransactions transactions which were already committed or aborted and do not
     *     need further handling
     */
    private void cleanUpUserContext(
            Collection<FlinkKafkaProducer.KafkaTransactionState> handledTransactions) {
        if (!getUserContext().isPresent()) {
            return;
        }
        HashSet<String> abortTransactions = new HashSet<>(getUserContext().get().transactionalIds);
        handledTransactions.forEach(
                kafkaTransactionState ->
                        abortTransactions.remove(kafkaTransactionState.transactionalId));
        abortTransactions(abortTransactions);
    }

    // 重置可用 Transaction ID 池
    private void resetAvailableTransactionalIdsPool(Collection<String> transactionalIds) {
        availableTransactionalIds.clear();
        availableTransactionalIds.addAll(transactionalIds);
    }

    // ----------------------------------- Utilities --------------------------

    private void abortTransactions(final Set<String> transactionalIds) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        transactionalIds
                .parallelStream()
                .forEach(
                        transactionalId -> {
                            // The parallelStream executes the consumer in a separated thread pool.
                            // Because the consumer(e.g. Kafka) uses the context classloader to
                            // construct some class
                            // we should set the correct classloader for it.
                            try (TemporaryClassLoaderContext ignored =
                                    TemporaryClassLoaderContext.of(classLoader)) {
                                // don't mess with the original configuration or any other
                                // properties of the
                                // original object
                                // -> create an internal kafka producer on our own and do not rely
                                // on
                                //    initTransactionalProducer().
                                final Properties myConfig = new Properties();
                                myConfig.putAll(producerConfig);
                                initTransactionalProducerConfig(myConfig, transactionalId);
                                FlinkKafkaInternalProducer<byte[], byte[]> kafkaProducer = null;
                                try {
                                    kafkaProducer = new FlinkKafkaInternalProducer<>(myConfig);
                                    // it suffices to call initTransactions - this will abort any
                                    // lingering transactions
                                    kafkaProducer.initTransactions();
                                } finally {
                                    if (kafkaProducer != null) {
                                        kafkaProducer.close(Duration.ofSeconds(0));
                                    }
                                }
                            }
                        });
    }

    int getTransactionCoordinatorId() {
        final FlinkKafkaProducer.KafkaTransactionState currentTransaction = currentTransaction();
        if (currentTransaction == null || currentTransaction.producer == null) {
            throw new IllegalArgumentException();
        }
        return currentTransaction.producer.getTransactionCoordinatorId();
    }

    /**
     * 对于每个检查点，我们创建新的 {@link FlinkKafkaInternalProducer} 以便新事务不会与先前检查点期间创建的
     * 事务发生冲突（{@code producer.initTransactions()} 确保我们获得新的 producerId 和 epoch 计数器）。
     *
     * For each checkpoint we create new {@link FlinkKafkaInternalProducer} so that new transactions
     * will not clash with transactions created during previous checkpoints ({@code
     * producer.initTransactions()} assures that we obtain new producerId and epoch counters).
     */
    private FlinkKafkaInternalProducer<byte[], byte[]> createTransactionalProducer()
            throws FlinkKafkaException {
        String transactionalId = availableTransactionalIds.poll();
        if (transactionalId == null) {
            // 正在进行的快照过多。增加 kafka 生产者池的大小或减少并发检查点的数量。
            throw new FlinkKafkaException(
                    FlinkKafkaErrorCode.PRODUCERS_POOL_EMPTY,
                    "Too many ongoing snapshots. Increase kafka producers pool size or decrease number of concurrent checkpoints.");
        }
        FlinkKafkaInternalProducer<byte[], byte[]> producer =
                initTransactionalProducer(transactionalId, true);
        producer.initTransactions();
        return producer;
    }

    private void recycleTransactionalProducer(FlinkKafkaInternalProducer<byte[], byte[]> producer) {
        availableTransactionalIds.add(producer.getTransactionalId());
        producer.flush();
        producer.close(Duration.ofSeconds(0));
    }

    private FlinkKafkaInternalProducer<byte[], byte[]> initTransactionalProducer(
            String transactionalId, boolean registerMetrics) {
        initTransactionalProducerConfig(producerConfig, transactionalId);
        return initProducer(registerMetrics);
    }

    private static void initTransactionalProducerConfig(
            Properties producerConfig, String transactionalId) {
        producerConfig.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId);
    }

    private FlinkKafkaInternalProducer<byte[], byte[]> initNonTransactionalProducer(
            boolean registerMetrics) {
        producerConfig.remove(ProducerConfig.TRANSACTIONAL_ID_CONFIG);
        return initProducer(registerMetrics);
    }

    private FlinkKafkaInternalProducer<byte[], byte[]> initProducer(boolean registerMetrics) {
        FlinkKafkaInternalProducer<byte[], byte[]> producer = createProducer();

        LOG.info(
                "Starting FlinkKafkaInternalProducer ({}/{}) to produce into default topic {}",
                getRuntimeContext().getIndexOfThisSubtask() + 1,
                getRuntimeContext().getNumberOfParallelSubtasks(),
                defaultTopicId);

        // register Kafka metrics to Flink accumulators
        // 将 Kafka 指标注册到 Flink 累加器
        if (registerMetrics
                && !Boolean.parseBoolean(
                        producerConfig.getProperty(KEY_DISABLE_METRICS, "false"))) {
            Map<MetricName, ? extends Metric> metrics = producer.metrics();

            if (metrics == null) {
                // MapR's Kafka implementation returns null here.
                LOG.info("Producer implementation does not support metrics");
            } else {
                final MetricGroup kafkaMetricGroup =
                        getRuntimeContext().getMetricGroup().addGroup("KafkaProducer");
                for (Map.Entry<MetricName, ? extends Metric> entry : metrics.entrySet()) {
                    String name = entry.getKey().name();
                    Metric metric = entry.getValue();

                    KafkaMetricMutableWrapper wrapper = previouslyCreatedMetrics.get(name);
                    if (wrapper != null) {
                        wrapper.setKafkaMetric(metric);
                    } else {
                        // TODO: somehow merge metrics from all active producers?
                        wrapper = new KafkaMetricMutableWrapper(metric);
                        previouslyCreatedMetrics.put(name, wrapper);
                        kafkaMetricGroup.gauge(name, wrapper);
                    }
                }
            }
        }
        return producer;
    }

    protected void checkErroneous() throws FlinkKafkaException {
        Exception e = asyncException;
        if (e != null) {
            // prevent double throwing
            asyncException = null;
            throw new FlinkKafkaException(
                    FlinkKafkaErrorCode.EXTERNAL_ERROR,
                    "Failed to send data to Kafka: " + e.getMessage(),
                    e);
        }
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    // 迁移下一个事务 ID 后状态
    private void migrateNextTransactionalIdHindState(FunctionInitializationContext context)
            throws Exception {
        ListState<NextTransactionalIdHint> oldNextTransactionalIdHintState =
                context.getOperatorStateStore()
                        .getUnionListState(NEXT_TRANSACTIONAL_ID_HINT_DESCRIPTOR);
        nextTransactionalIdHintState =
                context.getOperatorStateStore()
                        .getUnionListState(NEXT_TRANSACTIONAL_ID_HINT_DESCRIPTOR_V2);

        ArrayList<NextTransactionalIdHint> oldTransactionalIdHints =
                Lists.newArrayList(oldNextTransactionalIdHintState.get());
        if (!oldTransactionalIdHints.isEmpty()) {
            nextTransactionalIdHintState.addAll(oldTransactionalIdHints);
            // clear old state
            oldNextTransactionalIdHintState.clear();
        }
    }

    private static Properties getPropertiesFromBrokerList(String brokerList) {
        String[] elements = brokerList.split(",");

        // validate the broker addresses
        for (String broker : elements) {
            NetUtils.getCorrectHostnamePort(broker);
        }

        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        return props;
    }

    protected static int[] getPartitionsByTopic(String topic, Producer<byte[], byte[]> producer) {
        // the fetched list is immutable, so we're creating a mutable copy in order to sort it
        List<PartitionInfo> partitionsList = new ArrayList<>(producer.partitionsFor(topic));

        // sort the partitions by partition id to make sure the fetched partition list is the same
        // across subtasks
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

    /** State for handling transactions. */
    // 处理事务的状态。
    @VisibleForTesting
    @Internal
    public static class KafkaTransactionState {

        // J: 内部 flink kafka 生产者。
        private final transient FlinkKafkaInternalProducer<byte[], byte[]> producer;

        @Nullable final String transactionalId;

        final long producerId;

        final short epoch;

        @VisibleForTesting
        public KafkaTransactionState(
                String transactionalId, FlinkKafkaInternalProducer<byte[], byte[]> producer) {
            this(transactionalId, producer.getProducerId(), producer.getEpoch(), producer);
        }

        @VisibleForTesting
        public KafkaTransactionState(FlinkKafkaInternalProducer<byte[], byte[]> producer) {
            this(null, -1, (short) -1, producer);
        }

        @VisibleForTesting
        public KafkaTransactionState(
                @Nullable String transactionalId,
                long producerId,
                short epoch,
                FlinkKafkaInternalProducer<byte[], byte[]> producer) {
            this.transactionalId = transactionalId;
            this.producerId = producerId;
            this.epoch = epoch;
            this.producer = producer;
        }

        boolean isTransactional() {
            return transactionalId != null;
        }

        public FlinkKafkaInternalProducer<byte[], byte[]> getProducer() {
            return producer;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s [transactionalId=%s, producerId=%s, epoch=%s]",
                    this.getClass().getSimpleName(), transactionalId, producerId, epoch);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            FlinkKafkaProducer.KafkaTransactionState that =
                    (FlinkKafkaProducer.KafkaTransactionState) o;

            if (producerId != that.producerId) {
                return false;
            }
            if (epoch != that.epoch) {
                return false;
            }
            return transactionalId != null
                    ? transactionalId.equals(that.transactionalId)
                    : that.transactionalId == null;
        }

        @Override
        public int hashCode() {
            int result = transactionalId != null ? transactionalId.hashCode() : 0;
            result = 31 * result + (int) (producerId ^ (producerId >>> 32));
            result = 31 * result + (int) epoch;
            return result;
        }
    }

    /**
     * 与 {@link FlinkKafkaProducer} 的此实例关联的上下文。用于跟踪事务 ID 的用户。
     *
     * Context associated to this instance of the {@link FlinkKafkaProducer}. User for keeping track
     * of the transactionalIds.
     */
    @VisibleForTesting
    @Internal
    public static class KafkaTransactionContext {
        final Set<String> transactionalIds;

        @VisibleForTesting
        public KafkaTransactionContext(Set<String> transactionalIds) {
            checkNotNull(transactionalIds);
            this.transactionalIds = transactionalIds;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            FlinkKafkaProducer.KafkaTransactionContext that =
                    (FlinkKafkaProducer.KafkaTransactionContext) o;

            return transactionalIds.equals(that.transactionalIds);
        }

        @Override
        public int hashCode() {
            return transactionalIds.hashCode();
        }
    }

    /**
     * {@link org.apache.flink.api.common.typeutils.TypeSerializer} for {@link
     * FlinkKafkaProducer.KafkaTransactionState}.
     */
    @VisibleForTesting
    @Internal
    public static class TransactionStateSerializer
            extends TypeSerializerSingleton<FlinkKafkaProducer.KafkaTransactionState> {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean isImmutableType() {
            return true;
        }

        @Override
        public FlinkKafkaProducer.KafkaTransactionState createInstance() {
            return null;
        }

        @Override
        public FlinkKafkaProducer.KafkaTransactionState copy(
                FlinkKafkaProducer.KafkaTransactionState from) {
            return from;
        }

        @Override
        public FlinkKafkaProducer.KafkaTransactionState copy(
                FlinkKafkaProducer.KafkaTransactionState from,
                FlinkKafkaProducer.KafkaTransactionState reuse) {
            return from;
        }

        @Override
        public int getLength() {
            return -1;
        }

        @Override
        public void serialize(
                FlinkKafkaProducer.KafkaTransactionState record, DataOutputView target)
                throws IOException {
            if (record.transactionalId == null) {
                target.writeBoolean(false);
            } else {
                target.writeBoolean(true);
                target.writeUTF(record.transactionalId);
            }
            target.writeLong(record.producerId);
            target.writeShort(record.epoch);
        }

        @Override
        public FlinkKafkaProducer.KafkaTransactionState deserialize(DataInputView source)
                throws IOException {
            String transactionalId = null;
            if (source.readBoolean()) {
                transactionalId = source.readUTF();
            }
            long producerId = source.readLong();
            short epoch = source.readShort();
            return new FlinkKafkaProducer.KafkaTransactionState(
                    transactionalId, producerId, epoch, null);
        }

        @Override
        public FlinkKafkaProducer.KafkaTransactionState deserialize(
                FlinkKafkaProducer.KafkaTransactionState reuse, DataInputView source)
                throws IOException {
            return deserialize(source);
        }

        @Override
        public void copy(DataInputView source, DataOutputView target) throws IOException {
            boolean hasTransactionalId = source.readBoolean();
            target.writeBoolean(hasTransactionalId);
            if (hasTransactionalId) {
                target.writeUTF(source.readUTF());
            }
            target.writeLong(source.readLong());
            target.writeShort(source.readShort());
        }

        // -----------------------------------------------------------------------------------

        @Override
        public TypeSerializerSnapshot<FlinkKafkaProducer.KafkaTransactionState>
                snapshotConfiguration() {
            return new TransactionStateSerializerSnapshot();
        }

        /** Serializer configuration snapshot for compatibility and format evolution. */
        // 用于兼容性和格式演变的串行器配置快照。
        @SuppressWarnings("WeakerAccess")
        public static final class TransactionStateSerializerSnapshot
                extends SimpleTypeSerializerSnapshot<FlinkKafkaProducer.KafkaTransactionState> {

            public TransactionStateSerializerSnapshot() {
                super(TransactionStateSerializer::new);
            }
        }
    }

    /**
     * {@link org.apache.flink.api.common.typeutils.TypeSerializer} for {@link
     * FlinkKafkaProducer.KafkaTransactionContext}.
     */
    @VisibleForTesting
    @Internal
    public static class ContextStateSerializer
            extends TypeSerializerSingleton<FlinkKafkaProducer.KafkaTransactionContext> {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean isImmutableType() {
            return true;
        }

        @Override
        public FlinkKafkaProducer.KafkaTransactionContext createInstance() {
            return null;
        }

        @Override
        public FlinkKafkaProducer.KafkaTransactionContext copy(
                FlinkKafkaProducer.KafkaTransactionContext from) {
            return from;
        }

        @Override
        public FlinkKafkaProducer.KafkaTransactionContext copy(
                FlinkKafkaProducer.KafkaTransactionContext from,
                FlinkKafkaProducer.KafkaTransactionContext reuse) {
            return from;
        }

        @Override
        public int getLength() {
            return -1;
        }

        @Override
        public void serialize(
                FlinkKafkaProducer.KafkaTransactionContext record, DataOutputView target)
                throws IOException {
            int numIds = record.transactionalIds.size();
            target.writeInt(numIds);
            for (String id : record.transactionalIds) {
                target.writeUTF(id);
            }
        }

        @Override
        public FlinkKafkaProducer.KafkaTransactionContext deserialize(DataInputView source)
                throws IOException {
            int numIds = source.readInt();
            Set<String> ids = new HashSet<>(numIds);
            for (int i = 0; i < numIds; i++) {
                ids.add(source.readUTF());
            }
            return new FlinkKafkaProducer.KafkaTransactionContext(ids);
        }

        @Override
        public FlinkKafkaProducer.KafkaTransactionContext deserialize(
                FlinkKafkaProducer.KafkaTransactionContext reuse, DataInputView source)
                throws IOException {
            return deserialize(source);
        }

        @Override
        public void copy(DataInputView source, DataOutputView target) throws IOException {
            int numIds = source.readInt();
            target.writeInt(numIds);
            for (int i = 0; i < numIds; i++) {
                target.writeUTF(source.readUTF());
            }
        }

        // -----------------------------------------------------------------------------------

        @Override
        public TypeSerializerSnapshot<KafkaTransactionContext> snapshotConfiguration() {
            return new ContextStateSerializerSnapshot();
        }

        /** Serializer configuration snapshot for compatibility and format evolution. */
        // 用于兼容性和格式演变的串行器配置快照。
        @SuppressWarnings("WeakerAccess")
        public static final class ContextStateSerializerSnapshot
                extends SimpleTypeSerializerSnapshot<KafkaTransactionContext> {

            public ContextStateSerializerSnapshot() {
                super(ContextStateSerializer::new);
            }
        }
    }

    /** Keep information required to deduce next safe to use transactional id. */
    // 保留推断下一个安全使用事务 ID 所需的信息。
    public static class NextTransactionalIdHint {
        public int lastParallelism = 0;
        public long nextFreeTransactionalId = 0;

        public NextTransactionalIdHint() {
            this(0, 0);
        }

        public NextTransactionalIdHint(int parallelism, long nextFreeTransactionalId) {
            this.lastParallelism = parallelism;
            this.nextFreeTransactionalId = nextFreeTransactionalId;
        }

        @Override
        public String toString() {
            return "NextTransactionalIdHint["
                    + "lastParallelism="
                    + lastParallelism
                    + ", nextFreeTransactionalId="
                    + nextFreeTransactionalId
                    + ']';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            NextTransactionalIdHint that = (NextTransactionalIdHint) o;

            if (lastParallelism != that.lastParallelism) {
                return false;
            }
            return nextFreeTransactionalId == that.nextFreeTransactionalId;
        }

        @Override
        public int hashCode() {
            int result = lastParallelism;
            result =
                    31 * result
                            + (int) (nextFreeTransactionalId ^ (nextFreeTransactionalId >>> 32));
            return result;
        }
    }

    /**
     * {@link org.apache.flink.api.common.typeutils.TypeSerializer} for {@link
     * FlinkKafkaProducer.NextTransactionalIdHint}.
     */
    @VisibleForTesting
    @Internal
    public static class NextTransactionalIdHintSerializer
            extends TypeSerializerSingleton<NextTransactionalIdHint> {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean isImmutableType() {
            return true;
        }

        @Override
        public NextTransactionalIdHint createInstance() {
            return new NextTransactionalIdHint();
        }

        @Override
        public NextTransactionalIdHint copy(NextTransactionalIdHint from) {
            return from;
        }

        @Override
        public NextTransactionalIdHint copy(
                NextTransactionalIdHint from, NextTransactionalIdHint reuse) {
            return from;
        }

        @Override
        public int getLength() {
            return Long.BYTES + Integer.BYTES;
        }

        @Override
        public void serialize(NextTransactionalIdHint record, DataOutputView target)
                throws IOException {
            target.writeLong(record.nextFreeTransactionalId);
            target.writeInt(record.lastParallelism);
        }

        @Override
        public NextTransactionalIdHint deserialize(DataInputView source) throws IOException {
            long nextFreeTransactionalId = source.readLong();
            int lastParallelism = source.readInt();
            return new NextTransactionalIdHint(lastParallelism, nextFreeTransactionalId);
        }

        @Override
        public NextTransactionalIdHint deserialize(
                NextTransactionalIdHint reuse, DataInputView source) throws IOException {
            return deserialize(source);
        }

        @Override
        public void copy(DataInputView source, DataOutputView target) throws IOException {
            target.writeLong(source.readLong());
            target.writeInt(source.readInt());
        }

        @Override
        public TypeSerializerSnapshot<NextTransactionalIdHint> snapshotConfiguration() {
            return new NextTransactionalIdHintSerializerSnapshot();
        }

        /** Serializer configuration snapshot for compatibility and format evolution. */
        @SuppressWarnings("WeakerAccess")
        public static final class NextTransactionalIdHintSerializerSnapshot
                extends SimpleTypeSerializerSnapshot<NextTransactionalIdHint> {

            public NextTransactionalIdHintSerializerSnapshot() {
                super(NextTransactionalIdHintSerializer::new);
            }
        }
    }
}
