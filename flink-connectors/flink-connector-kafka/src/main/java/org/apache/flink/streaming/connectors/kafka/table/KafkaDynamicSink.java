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

package org.apache.flink.streaming.connectors.kafka.table;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.streaming.connectors.kafka.table.DynamicKafkaSerializationSchema.MetadataConverter;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.connector.sink.abilities.SupportsWritingMetadata;
import org.apache.flink.table.data.ArrayData;
import org.apache.flink.table.data.MapData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.utils.DataTypeUtils;

import org.apache.kafka.common.header.Header;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import static org.apache.flink.util.Preconditions.checkNotNull;

/** A version-agnostic Kafka {@link DynamicTableSink}. */
@Internal
public class KafkaDynamicSink implements DynamicTableSink, SupportsWritingMetadata {

    // --------------------------------------------------------------------------------------------
    // Mutable attributes
    // --------------------------------------------------------------------------------------------

    /** Metadata that is appended at the end of a physical sink row. */
    // 附加在物理接收器行的末尾的元数据
    protected List<String> metadataKeys;

    // --------------------------------------------------------------------------------------------
    // Format attributes
    // --------------------------------------------------------------------------------------------

    /** Data type of consumed data type. */
    protected DataType consumedDataType;

    /** Data type to configure the formats. */
    // 配置格式的数据类型
    protected final DataType physicalDataType;

    /** Optional format for encoding keys to Kafka. */
    protected final @Nullable EncodingFormat<SerializationSchema<RowData>> keyEncodingFormat;

    /** Format for encoding values to Kafka. */
    protected final EncodingFormat<SerializationSchema<RowData>> valueEncodingFormat;

    /** Indices that determine the key fields and the source position in the consumed row. */
    // 确定关键字段和所消费行的源位置的索引
    protected final int[] keyProjection;

    /** Indices that determine the value fields and the source position in the consumed row. */
    protected final int[] valueProjection;

    /** Prefix that needs to be removed from fields when constructing the physical data type. */
    // 在构造物理数据类型时需要从字段中删除的前缀
    protected final @Nullable String keyPrefix;

    // --------------------------------------------------------------------------------------------
    // Kafka-specific attributes
    // --------------------------------------------------------------------------------------------

    /** The Kafka topic to write to. */
    protected final String topic;

    /** Properties for the Kafka producer. */
    protected final Properties properties;

    /** Partitioner to select Kafka partition for each item. */
    protected final @Nullable FlinkKafkaPartitioner<RowData> partitioner;

    /** Sink commit semantic. */
    protected final KafkaSinkSemantic semantic;

    /**
     * 标志来确定接收模式。在upsert模式下，接收器将删除更新前消息转换为 tombstone 消息。
     *
     * Flag to determine sink mode. In upsert mode sink transforms the delete/update-before message
     * to tombstone message.
     */
    protected final boolean upsertMode;

    /** Sink buffer flush config which only supported in upsert mode now. */
    protected final SinkBufferFlushMode flushMode;

    /** Parallelism of the physical Kafka producer. * */
    protected final @Nullable Integer parallelism;

    public KafkaDynamicSink(
            DataType consumedDataType,
            DataType physicalDataType,
            @Nullable EncodingFormat<SerializationSchema<RowData>> keyEncodingFormat,
            EncodingFormat<SerializationSchema<RowData>> valueEncodingFormat,
            int[] keyProjection,
            int[] valueProjection,
            @Nullable String keyPrefix,
            String topic,
            Properties properties,
            @Nullable FlinkKafkaPartitioner<RowData> partitioner,
            KafkaSinkSemantic semantic,
            boolean upsertMode,
            SinkBufferFlushMode flushMode,
            @Nullable Integer parallelism) {
        // Format attributes
        this.consumedDataType =
                checkNotNull(consumedDataType, "Consumed data type must not be null.");
        this.physicalDataType =
                checkNotNull(physicalDataType, "Physical data type must not be null.");
        this.keyEncodingFormat = keyEncodingFormat;
        this.valueEncodingFormat =
                checkNotNull(valueEncodingFormat, "Value encoding format must not be null.");
        this.keyProjection = checkNotNull(keyProjection, "Key projection must not be null.");
        this.valueProjection = checkNotNull(valueProjection, "Value projection must not be null.");
        this.keyPrefix = keyPrefix;
        // Mutable attributes
        this.metadataKeys = Collections.emptyList();
        // Kafka-specific attributes
        this.topic = checkNotNull(topic, "Topic must not be null.");
        this.properties = checkNotNull(properties, "Properties must not be null.");
        this.partitioner = partitioner;
        this.semantic = checkNotNull(semantic, "Semantic must not be null.");
        this.upsertMode = upsertMode;
        this.flushMode = checkNotNull(flushMode);
        if (flushMode.isEnabled() && !upsertMode) {
            throw new IllegalArgumentException(
                    "Sink buffer flush is only supported in upsert-kafka.");
        }
        this.parallelism = parallelism;
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        return valueEncodingFormat.getChangelogMode();
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        final SerializationSchema<RowData> keySerialization =
                createSerialization(context, keyEncodingFormat, keyProjection, keyPrefix);

        final SerializationSchema<RowData> valueSerialization =
                createSerialization(context, valueEncodingFormat, valueProjection, null);

        final FlinkKafkaProducer<RowData> kafkaProducer =
                createKafkaProducer(keySerialization, valueSerialization);

        // J: 刷写模式开启且为 upsert 模式的处理
        if (flushMode.isEnabled() && upsertMode) {
            BufferedUpsertSinkFunction buffedSinkFunction =
                    new BufferedUpsertSinkFunction(
                            kafkaProducer,
                            physicalDataType,
                            keyProjection,
                            context.createTypeInformation(consumedDataType),
                            flushMode);
            // J: 提供运行时的 provider
            return SinkFunctionProvider.of(buffedSinkFunction, parallelism);
        } else {
            return SinkFunctionProvider.of(kafkaProducer, parallelism);
        }
    }

    @Override
    public Map<String, DataType> listWritableMetadata() {
        final Map<String, DataType> metadataMap = new LinkedHashMap<>();
        Stream.of(WritableMetadata.values())
                .forEachOrdered(m -> metadataMap.put(m.key, m.dataType));
        return metadataMap;
    }

    @Override
    public void applyWritableMetadata(List<String> metadataKeys, DataType consumedDataType) {
        this.metadataKeys = metadataKeys;
        this.consumedDataType = consumedDataType;
    }

    @Override
    public DynamicTableSink copy() {
        final KafkaDynamicSink copy =
                new KafkaDynamicSink(
                        consumedDataType,
                        physicalDataType,
                        keyEncodingFormat,
                        valueEncodingFormat,
                        keyProjection,
                        valueProjection,
                        keyPrefix,
                        topic,
                        properties,
                        partitioner,
                        semantic,
                        upsertMode,
                        flushMode,
                        parallelism);
        copy.metadataKeys = metadataKeys;
        return copy;
    }

    @Override
    public String asSummaryString() {
        return "Kafka table sink";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final KafkaDynamicSink that = (KafkaDynamicSink) o;
        return Objects.equals(metadataKeys, that.metadataKeys)
                && Objects.equals(consumedDataType, that.consumedDataType)
                && Objects.equals(physicalDataType, that.physicalDataType)
                && Objects.equals(keyEncodingFormat, that.keyEncodingFormat)
                && Objects.equals(valueEncodingFormat, that.valueEncodingFormat)
                && Arrays.equals(keyProjection, that.keyProjection)
                && Arrays.equals(valueProjection, that.valueProjection)
                && Objects.equals(keyPrefix, that.keyPrefix)
                && Objects.equals(topic, that.topic)
                && Objects.equals(properties, that.properties)
                && Objects.equals(partitioner, that.partitioner)
                && Objects.equals(semantic, that.semantic)
                && Objects.equals(upsertMode, that.upsertMode)
                && Objects.equals(flushMode, that.flushMode)
                && Objects.equals(parallelism, that.parallelism);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                metadataKeys,
                consumedDataType,
                physicalDataType,
                keyEncodingFormat,
                valueEncodingFormat,
                keyProjection,
                valueProjection,
                keyPrefix,
                topic,
                properties,
                partitioner,
                semantic,
                upsertMode,
                flushMode,
                parallelism);
    }

    // --------------------------------------------------------------------------------------------

    protected FlinkKafkaProducer<RowData> createKafkaProducer(
            SerializationSchema<RowData> keySerialization,
            SerializationSchema<RowData> valueSerialization) {
        final List<LogicalType> physicalChildren = physicalDataType.getLogicalType().getChildren();

        final RowData.FieldGetter[] keyFieldGetters =
                Arrays.stream(keyProjection)
                        .mapToObj(
                                targetField ->
                                        RowData.createFieldGetter(
                                                physicalChildren.get(targetField), targetField))
                        .toArray(RowData.FieldGetter[]::new);

        final RowData.FieldGetter[] valueFieldGetters =
                Arrays.stream(valueProjection)
                        .mapToObj(
                                targetField ->
                                        RowData.createFieldGetter(
                                                physicalChildren.get(targetField), targetField))
                        .toArray(RowData.FieldGetter[]::new);

        // determine the positions of metadata in the consumed row
        final int[] metadataPositions =
                Stream.of(WritableMetadata.values())
                        .mapToInt(
                                m -> {
                                    final int pos = metadataKeys.indexOf(m.key);
                                    if (pos < 0) {
                                        return -1;
                                    }
                                    return physicalChildren.size() + pos;
                                })
                        .toArray();

        // check if metadata is used at all
        final boolean hasMetadata = metadataKeys.size() > 0;

        final DynamicKafkaSerializationSchema kafkaSerializer =
                new DynamicKafkaSerializationSchema(
                        topic,
                        partitioner,
                        keySerialization,
                        valueSerialization,
                        keyFieldGetters,
                        valueFieldGetters,
                        hasMetadata,
                        metadataPositions,
                        upsertMode);

        return new FlinkKafkaProducer<>(
                topic,
                kafkaSerializer,
                properties,
                FlinkKafkaProducer.Semantic.valueOf(semantic.toString()),
                FlinkKafkaProducer.DEFAULT_KAFKA_PRODUCERS_POOL_SIZE);
    }

    private @Nullable SerializationSchema<RowData> createSerialization(
            DynamicTableSink.Context context,
            @Nullable EncodingFormat<SerializationSchema<RowData>> format,
            int[] projection,
            @Nullable String prefix) {
        if (format == null) {
            return null;
        }
        DataType physicalFormatDataType =
                DataTypeUtils.projectRow(this.physicalDataType, projection);
        if (prefix != null) {
            physicalFormatDataType = DataTypeUtils.stripRowPrefix(physicalFormatDataType, prefix);
        }
        return format.createRuntimeEncoder(context, physicalFormatDataType);
    }

    // --------------------------------------------------------------------------------------------
    // Metadata handling
    // --------------------------------------------------------------------------------------------

    enum WritableMetadata {
        // J: message 的 header
        HEADERS(
                "headers",
                // key and value of the map are nullable to make handling easier in queries
                DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.BYTES().nullable())
                        .nullable(),
                new MetadataConverter() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object read(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        // J: Map as two array handle
                        final MapData map = row.getMap(pos);
                        final ArrayData keyArray = map.keyArray();
                        final ArrayData valueArray = map.valueArray();
                        final List<Header> headers = new ArrayList<>();
                        for (int i = 0; i < keyArray.size(); i++) {
                            if (!keyArray.isNullAt(i) && !valueArray.isNullAt(i)) {
                                final String key = keyArray.getString(i).toString();
                                final byte[] value = valueArray.getBinary(i);
                                headers.add(new KafkaHeader(key, value));
                            }
                        }
                        return headers;
                    }
                }),

        // J: timestamp 多种..
        TIMESTAMP(
                "timestamp",
                DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).nullable(),
                new MetadataConverter() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object read(RowData row, int pos) {
                        if (row.isNullAt(pos)) {
                            return null;
                        }
                        return row.getTimestamp(pos, 3).getMillisecond();
                    }
                });

        final String key;

        final DataType dataType;

        final MetadataConverter converter;

        WritableMetadata(String key, DataType dataType, MetadataConverter converter) {
            this.key = key;
            this.dataType = dataType;
            this.converter = converter;
        }
    }

    // --------------------------------------------------------------------------------------------

    private static class KafkaHeader implements Header {

        private final String key;

        private final byte[] value;

        KafkaHeader(String key, byte[] value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public byte[] value() {
            return value;
        }
    }
}
