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

package org.apache.flink.table.connector.format;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.factories.DynamicTableFactory;

/**
 * 连接器格式的基本接口。
 *
 * <p>根据外部系统的类型，连接器可能支持不同的行读取和写入编码。该接口是构建实际运行时实现之前的中间表示。
 *
 * <p>可以从两个维度区分格式：
 *
 *   <li>应用格式的上下文（{@link DynamicTableSource} 或 {@link DynamicTableSink}）。
 *   <li>所需的运行时实现接口（例如 {@link DeserializationSchema} 或一些批量接口）。
 *
 * <p>{@link DynamicTableFactory} 可以搜索连接器接受的格式。
 *
 * Base interface for connector formats.
 *
 * <p>Depending on the kind of external system, a connector might support different encodings for
 * reading and writing rows. This interface is an intermediate representation before constructing
 * actual runtime implementation.
 *
 * <p>Formats can be distinguished along two dimensions:
 *
 * <ul>
 *   <li>Context in which the format is applied ({@link DynamicTableSource} or {@link
 *       DynamicTableSink}).
 *   <li>Runtime implementation interface that is required (e.g. {@link DeserializationSchema} or
 *       some bulk interface).
 * </ul>
 *
 * <p>A {@link DynamicTableFactory} can search for a format that it is accepted by the connector.
 *
 * @see DecodingFormat
 * @see EncodingFormat
 */
@PublicEvolving
public interface Format {

    /**
     * 返回连接器(或计划器)在运行时预期的更改集。
     *
     * Returns the set of changes that a connector (and transitively the planner) can expect during
     * runtime.
     */
    ChangelogMode getChangelogMode();
}
