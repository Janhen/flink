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

package org.apache.flink.streaming.util.serialization;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import static org.apache.flink.api.java.typeutils.TypeExtractor.getForClass;

/**
 * 反序列化 JSON 字符串到 ObjectNode 的模式。
 *
 * <p>关键字段可以通过调用objectNode.get("Key").get(&lt;name>).as(&lt;type>)来访问
 *
 * <p>Value字段可以通过调用objectNode.get("Value").get(&lt;name>).as(&lt;type>)来访问
 *
 * J: 结果为 jackson 的 {@code ObjectNode}
 *
 * DeserializationSchema that deserializes a JSON String into an ObjectNode.
 *
 * <p>Key fields can be accessed by calling objectNode.get("key").get(&lt;name>).as(&lt;type>)
 *
 * <p>Value fields can be accessed by calling objectNode.get("value").get(&lt;name>).as(&lt;type>)
 *
 * <p>Metadata fields can be accessed by calling
 * objectNode.get("metadata").get(&lt;name>).as(&lt;type>) and include the "offset" (long), "topic"
 * (String) and "partition" (int).
 */
@PublicEvolving
public class JSONKeyValueDeserializationSchema implements KafkaDeserializationSchema<ObjectNode> {

    private static final long serialVersionUID = 1509391548173891955L;

    // J: 是否将元数据包含进结果
    private final boolean includeMetadata;
    // J: Jackson ...
    private ObjectMapper mapper;

    public JSONKeyValueDeserializationSchema(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    @Override
    public ObjectNode deserialize(ConsumerRecord<byte[], byte[]> record) throws Exception {
        if (mapper == null) {
            mapper = new ObjectMapper();
        }
        ObjectNode node = mapper.createObjectNode();
        if (record.key() != null) {
            // 放入 kafka 消息的 key
            node.set("key", mapper.readValue(record.key(), JsonNode.class));
        }
        if (record.value() != null) {
            // 放入 kafka 消息的 value
            node.set("value", mapper.readValue(record.value(), JsonNode.class));
        }
        if (includeMetadata) {
            // 放入 kafka 消息的元数据信息到 metadata 下
            node.putObject("metadata")
                    .put("offset", record.offset())
                    .put("topic", record.topic())
                    .put("partition", record.partition());
        }
        return node;
    }

    @Override
    public boolean isEndOfStream(ObjectNode nextElement) {
        return false;
    }

    @Override
    public TypeInformation<ObjectNode> getProducedType() {
        return getForClass(ObjectNode.class);
    }
}
