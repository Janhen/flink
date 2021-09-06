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

package org.apache.flink.table.expressions;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.types.DataType;

import java.util.List;

/**
 * 已完全解决和验证的表达式。
 *
 * <p>与 {@link Expression} 相比，已解析表达式不再包含未解析子表达式，并为计算结果提供输出数据类型。
 *
 * <p>该类的实例描述了一个完全参数化的、不可变的表达式，可以序列化和持久化。
 *
 * <p>解析表达式是API到规划器的输出，并从规划器推入接口，例如，用于谓词下推。
 *
 * Expression that has been fully resolved and validated.
 *
 * <p>Compared to {@link Expression}, resolved expressions do not contain unresolved subexpressions
 * anymore and provide an output data type for the computation result.
 *
 * <p>Instances of this class describe a fully parameterized, immutable expression that can be
 * serialized and persisted.
 *
 * <p>Resolved expression are the output of the API to the planner and are pushed from the planner
 * into interfaces, for example, for predicate push-down.
 */
@PublicEvolving
public interface ResolvedExpression extends Expression {

    /**
     * 返回完全序列化此实例的字符串。序列化字符串可用于存储查询，例如，
     * {@link org.apache.flink.table.catalog。Catalog} 作为视图。
     *
     * Returns a string that fully serializes this instance. The serialized string can be used for
     * storing the query in, for example, a {@link org.apache.flink.table.catalog.Catalog} as a
     * view.
     *
     * @return detailed string for persisting in a catalog
     */
    default String asSerializableString() {
        throw new UnsupportedOperationException("Expressions are not string serializable for now.");
    }

    /** Returns the data type of the computation result. */
    DataType getOutputDataType();

    List<ResolvedExpression> getResolvedChildren();
}
