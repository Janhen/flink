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

import java.util.Arrays;
import java.util.List;

/**
 * HBase 的验证器。支持更多特性，例如批读写，async api(从 hbase 版本 2.0.0 支持)，LookupFunction 缓存。
 *
 * The validator for HBase. More features to be supported, e.g., batch read/write, async api(support
 * from hbase version 2.0.0), Caching for LookupFunction.
 */
@Internal
public abstract class AbstractHBaseValidator extends ConnectorDescriptorValidator {

    public static final String CONNECTOR_TYPE_VALUE_HBASE = "hbase";
    public static final String CONNECTOR_TABLE_NAME = "connector.table-name";
    public static final String CONNECTOR_ZK_QUORUM = "connector.zookeeper.quorum";
    public static final String CONNECTOR_ZK_NODE_PARENT = "connector.zookeeper.znode.parent";
    public static final String CONNECTOR_WRITE_BUFFER_FLUSH_MAX_SIZE =
            "connector.write.buffer-flush.max-size";
    public static final String CONNECTOR_WRITE_BUFFER_FLUSH_MAX_ROWS =
            "connector.write.buffer-flush.max-rows";
    public static final String CONNECTOR_WRITE_BUFFER_FLUSH_INTERVAL =
            "connector.write.buffer-flush.interval";
    public static final String CONNECTOR_PROPERTIES = "connector.properties";

    @Override
    public void validate(DescriptorProperties properties) {
        super.validate(properties);
        properties.validateValue(CONNECTOR_TYPE, CONNECTOR_TYPE_VALUE_HBASE, false);
        properties.validateString(CONNECTOR_TABLE_NAME, false, 1);
        properties.validateString(CONNECTOR_ZK_QUORUM, validateZkQuorum(), 1);
        properties.validateString(CONNECTOR_ZK_NODE_PARENT, true, 1);
        validateSinkProperties(properties);
        validateVersion(properties);
    }

    protected abstract boolean validateZkQuorum();

    private void validateSinkProperties(DescriptorProperties properties) {
        properties.validateMemorySize(
                CONNECTOR_WRITE_BUFFER_FLUSH_MAX_SIZE,
                true,
                1024 * 1024); // only allow MB precision
        properties.validateInt(CONNECTOR_WRITE_BUFFER_FLUSH_MAX_ROWS, true, 1);
        properties.validateDuration(CONNECTOR_WRITE_BUFFER_FLUSH_INTERVAL, true, 1);
    }

    private void validateVersion(DescriptorProperties properties) {
        final List<String> versions = Arrays.asList(getConnectorVersion());
        properties.validateEnumValues(CONNECTOR_VERSION, zkQuorumIsOptional(), versions);
    }

    protected abstract String getConnectorVersion();

    protected abstract boolean zkQuorumIsOptional();
}
