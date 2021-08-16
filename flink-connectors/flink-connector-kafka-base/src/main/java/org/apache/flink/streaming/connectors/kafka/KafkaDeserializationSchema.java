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
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.util.Collector;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.Serializable;

/**
 * 反序列化模式描述了如何将 Kafka ConsumerRecords 转换为数据类型(Java/Scala 对象)，并由 Flink 处理。
 *
 * The deserialization schema describes how to turn the Kafka ConsumerRecords into data types
 * (Java/Scala objects) that are processed by Flink.
 *
 * @param <T> The type created by the keyed deserialization schema.
 */
@PublicEvolving
public interface KafkaDeserializationSchema<T> extends Serializable, ResultTypeQueryable<T> {

    /**
     * 模式的初始化方法。它在实际工作方法{@link #deserialize}之前被调用，因此适合一次性设置工作。
     *
     * <p>提供的{@link DeserializationSchema.InitializationContext}可以用于访问其他特性，例如注册用户指标。
     *
     * Initialization method for the schema. It is called before the actual working methods {@link
     * #deserialize} and thus suitable for one time setup work.
     *
     * <p>The provided {@link DeserializationSchema.InitializationContext} can be used to access
     * additional features such as e.g. registering user metrics.
     *
     * @param context Contextual information that can be used during initialization.
     */
    default void open(DeserializationSchema.InitializationContext context) throws Exception {}

    /**
     * 方法来确定元素是否发出流结束的信号。如果返回 true，则不会触发元素。
     *
     * Method to decide whether the element signals the end of the stream. If true is returned the
     * element won't be emitted.
     *
     * @param nextElement The element to test for the end-of-stream signal.
     * @return True, if the element signals end of stream, false otherwise.
     */
    boolean isEndOfStream(T nextElement);

    /**
     * Deserializes the Kafka record.
     *
     * @param record Kafka record to be deserialized.
     * @return The deserialized message as an object (null if the message cannot be deserialized).
     */
    T deserialize(ConsumerRecord<byte[], byte[]> record) throws Exception;

    /**
     * 反序列化 Kafka 记录。
     *
     * <p>可以通过 {@link Collector} 输出多条记录。注意，生成的记录的数量和大小应该相对较小。根据源实现的不同，
     * 记录可以缓冲在内存中，或者收集记录可能会延迟发出检查点屏障。
     *
     * Deserializes the Kafka record.
     *
     * <p>Can output multiple records through the {@link Collector}. Note that number and size of
     * the produced records should be relatively small. Depending on the source implementation
     * records can be buffered in memory or collecting records might delay emitting checkpoint
     * barrier.
     *
     * @param message The message, as a byte array.
     * @param out The collector to put the resulting messages.
     */
    default void deserialize(ConsumerRecord<byte[], byte[]> message, Collector<T> out)
            throws Exception {
        T deserialized = deserialize(message);
        if (deserialized != null) {
            out.collect(deserialized);
        }
    }
}
