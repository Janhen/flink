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

package org.apache.flink.table.operations;

import org.apache.flink.annotation.Internal;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.expressions.Expression;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 描述从{@link DataStream}读取数据的关系操作。
 *
 * J: 将 DataStream 转换成一个 Table 的过程中，需要生成表结构，此类用于描述。。。
 *
 * <p><p>此操作可以只公开部分字段，或者更改底层{@link DataStream}的
 *   {@link org.apache.flink.api.common.typeutils.CompositeType}中可用字段的顺序。
 *   {@link JavaDataStreamQueryOperation#getFieldIndices()}描述了{@link TableSchema}字段与
 *   {@link org.apache.flink.api.common.typeutils.CompositeType}字段之间的映射。
 *
 * Describes a relational operation that reads from a {@link DataStream}.
 *
 * <p>This operation may expose only part, or change the order of the fields available in a {@link
 * org.apache.flink.api.common.typeutils.CompositeType} of the underlying {@link DataStream}. The
 * {@link JavaDataStreamQueryOperation#getFieldIndices()} describes the mapping between fields of
 * the {@link TableSchema} to the {@link org.apache.flink.api.common.typeutils.CompositeType}.
 */
@Internal
public class JavaDataStreamQueryOperation<E> implements QueryOperation {

    /**
     * The table identifier registered under the environment. The identifier might be null when the
     * it is from {@link StreamTableEnvironment#fromDataStream(DataStream, Expression...)}. But the
     * identifier should be not null if is from {@link
     * StreamTableEnvironment#createTemporaryView(String, DataStream)} with a registered name.
     */
    @Nullable private final ObjectIdentifier identifier;

    // 原始的 dataStream
    private final DataStream<E> dataStream;
    // 字段索引映射关系
    private final int[] fieldIndices;
    // 表结构
    private final ResolvedSchema resolvedSchema;

    public JavaDataStreamQueryOperation(
            DataStream<E> dataStream, int[] fieldIndices, ResolvedSchema resolvedSchema) {
        this(null, dataStream, fieldIndices, resolvedSchema);
    }

    public JavaDataStreamQueryOperation(
            ObjectIdentifier identifier,
            DataStream<E> dataStream,
            int[] fieldIndices,
            ResolvedSchema resolvedSchema) {
        this.identifier = identifier;
        this.dataStream = dataStream;
        this.resolvedSchema = resolvedSchema;
        this.fieldIndices = fieldIndices;
    }

    public DataStream<E> getDataStream() {
        return dataStream;
    }

    public Optional<ObjectIdentifier> getIdentifier() {
        return Optional.ofNullable(identifier);
    }

    public int[] getFieldIndices() {
        return fieldIndices;
    }

    @Override
    public ResolvedSchema getResolvedSchema() {
        return resolvedSchema;
    }

    @Override
    public String asSummaryString() {
        Map<String, Object> args = new LinkedHashMap<>();
        if (identifier != null) {
            args.put("id", identifier.asSummaryString());
        } else {
            args.put("id", dataStream.getId());
        }
        args.put("fields", resolvedSchema.getColumnNames());

        return OperationUtils.formatWithChildren(
                "DataStream", args, getChildren(), Operation::asSummaryString);
    }

    @Override
    public List<QueryOperation> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <T> T accept(QueryOperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
