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
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.util.Preconditions;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * 一个版本无关的Kafka {@link DynamicTableSink}。
 *
 * 版本特定的Kafka
 * 消费者需要扩展这个类并重写{@link #createKafkaProducer(String, Properties, SerializationSchema, Optional)}}。
 *
 * A version-agnostic Kafka {@link DynamicTableSink}.
 *
 * <p>The version-specific Kafka consumers need to extend this class and override {@link
 * #createKafkaProducer(String, Properties, SerializationSchema, Optional)}}.
 */
@Internal
public abstract class KafkaDynamicSinkBase implements DynamicTableSink {

    /** Consumed data type of the table. */
    protected final DataType consumedDataType;

    /** The Kafka topic to write to. */
    protected final String topic;

    /** Properties for the Kafka producer. */
    protected final Properties properties;

    /** Sink format for encoding records to Kafka. */
    protected final EncodingFormat<SerializationSchema<RowData>> encodingFormat;

    /** Partitioner to select Kafka partition for each item. */
    // Partitioner选择Kafka分区为每个 item
    protected final Optional<FlinkKafkaPartitioner<RowData>> partitioner;

    protected KafkaDynamicSinkBase(
            DataType consumedDataType,
            String topic,
            Properties properties,
            Optional<FlinkKafkaPartitioner<RowData>> partitioner,
            EncodingFormat<SerializationSchema<RowData>> encodingFormat) {
        this.consumedDataType =
                Preconditions.checkNotNull(
                        consumedDataType, "Consumed data type must not be null.");
        this.topic = Preconditions.checkNotNull(topic, "Topic must not be null.");
        this.properties = Preconditions.checkNotNull(properties, "Properties must not be null.");
        this.partitioner = Preconditions.checkNotNull(partitioner, "Partitioner must not be null.");
        this.encodingFormat =
                Preconditions.checkNotNull(encodingFormat, "Encoding format must not be null.");
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        return this.encodingFormat.getChangelogMode();
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        SerializationSchema<RowData> serializationSchema =
                this.encodingFormat.createRuntimeEncoder(context, this.consumedDataType);

        final SinkFunction<RowData> kafkaProducer =
                createKafkaProducer(this.topic, properties, serializationSchema, this.partitioner);

        return SinkFunctionProvider.of(kafkaProducer);
    }

    /**
     * 返回特定于版本的Kafka生成器。
     *
     * Returns the version-specific Kafka producer.
     *
     * @param topic Kafka topic to produce to.
     * @param properties Properties for the Kafka producer.
     * @param serializationSchema Serialization schema to use to create Kafka records.
     * @param partitioner Partitioner to select Kafka partition.
     * @return The version-specific Kafka producer
     */
    protected abstract SinkFunction<RowData> createKafkaProducer(
            String topic,
            Properties properties,
            SerializationSchema<RowData> serializationSchema,
            Optional<FlinkKafkaPartitioner<RowData>> partitioner);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final KafkaDynamicSinkBase that = (KafkaDynamicSinkBase) o;
        return Objects.equals(consumedDataType, that.consumedDataType)
                && Objects.equals(topic, that.topic)
                && Objects.equals(properties, that.properties)
                && Objects.equals(encodingFormat, that.encodingFormat)
                && Objects.equals(partitioner, that.partitioner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumedDataType, topic, properties, encodingFormat, partitioner);
    }
}
