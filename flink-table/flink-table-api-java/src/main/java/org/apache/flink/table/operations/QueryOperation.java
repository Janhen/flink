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
import org.apache.flink.table.api.Table;
import org.apache.flink.table.catalog.ResolvedSchema;

import java.util.List;

/**
 * 表示面向用户的 {@link Table} API背 后的操作结构的基类。
 *
 * <p>它表示一个操作，该操作可以是关系查询的一个节点。它有一个模式，可以用来验证应用在这个模式之上的
 *   {@link QueryOperation}。
 *
 * Base class for representing an operation structure behind a user-facing {@link Table} API.
 *
 * <p>It represents an operation that can be a node of a relational query. It has a schema, that can
 * be used to validate a {@link QueryOperation} applied on top of this one.
 */
@Internal
public interface QueryOperation extends Operation {

    /** Resolved schema of this operation. */
    // 此操作的已解析模式。
    ResolvedSchema getResolvedSchema();

    /**
     * 返回完全序列化此实例的字符串。序列化的字符串可以用来存储查询，例如
     * {@link org.apache.flink.table.catalog。Catalog}作为视图。
     *
     * Returns a string that fully serializes this instance. The serialized string can be used for
     * storing the query in e.g. a {@link org.apache.flink.table.catalog.Catalog} as a view.
     *
     * @return detailed string for persisting in a catalog
     * @see Operation#asSummaryString()
     */
    default String asSerializableString() {
        throw new UnsupportedOperationException(
                "QueryOperations are not string serializable for now.");
    }

    List<QueryOperation> getChildren();

    default <T> T accept(QueryOperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
