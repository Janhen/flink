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

package org.apache.flink.table.descriptors;

import org.apache.flink.annotation.Internal;

/** Validator for {@link ConnectorDescriptor}. */
// {@link ConnectorDescriptor} 的验证器。
@Internal
public abstract class ConnectorDescriptorValidator implements DescriptorValidator {

    /** Prefix for connector-related properties. */
    // 连接器相关属性的前缀。
    public static final String CONNECTOR = "connector";

    /** Key for describing the type of the connector. Usually used for factory discovery. */
    // 用于描述连接器类型的键。通常用于工厂发现。
    public static final String CONNECTOR_TYPE = "connector.type";

    /**
     * 用于描述属性版本的键。当属性格式改变时，此属性可用于向后兼容。
     *
     * Key for describing the property version. This property can be used for backwards
     * compatibility in case the property format changes.
     */
    public static final String CONNECTOR_PROPERTY_VERSION = "connector.property-version";

    /**
     * 用于描述连接器版本的键。这个属性可以用于不同的连接器版本(例如 Kafka 0.10 或 Kafka 0.11)。
     *
     * Key for describing the version of the connector. This property can be used for different
     * connector versions (e.g. Kafka 0.10 or Kafka 0.11).
     */
    public static final String CONNECTOR_VERSION = "connector.version";

    @Override
    public void validate(DescriptorProperties properties) {
        properties.validateString(CONNECTOR_TYPE, false, 1);
        properties.validateInt(CONNECTOR_PROPERTY_VERSION, true, 0);
    }
}
