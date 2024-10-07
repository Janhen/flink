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

package org.apache.flink.table.delegation;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.catalog.UnresolvedIdentifier;
import org.apache.flink.table.expressions.ResolvedExpression;
import org.apache.flink.table.operations.Operation;
import org.apache.flink.table.operations.QueryOperation;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;

import javax.annotation.Nullable;

import java.util.List;

/** Provides methods for parsing SQL objects from a SQL string. */
// 提供从SQL字符串解析SQL对象的方法。
@Internal
public interface Parser {

    /**
     * 用于解析表示为String的SQL查询的入口点。
     *
     * <p><b>注意:<b>如果创建的{@link Operation}是{@link QueryOperation}，它必须以
     * {@link Planner#translate(List)}方法能够理解的形式存在。
     *
     * <p>生成的操作树应该已经被验证过了。
     *
     * Entry point for parsing SQL queries expressed as a String.
     *
     * <p><b>Note:</b>If the created {@link Operation} is a {@link QueryOperation} it must be in a
     * form that will be understood by the {@link Planner#translate(List)} method.
     *
     * <p>The produced Operation trees should already be validated.
     *
     * @param statement the SQL statement to evaluate
     * @return parsed queries as trees of relational {@link Operation}s
     * @throws org.apache.flink.table.api.SqlParserException when failed to parse the statement
     */
    List<Operation> parse(String statement);

    /**
     * 用于解析表示为String的SQL标识符的入口点。
     *
     * Entry point for parsing SQL identifiers expressed as a String.
     *
     * @param identifier the SQL identifier to parse
     * @return parsed identifier
     * @throws org.apache.flink.table.api.SqlParserException when failed to parse the identifier
     */
    UnresolvedIdentifier parseIdentifier(String identifier);

    /**
     * 用于解析表示为String的SQL表达式的入口点。
     *
     * Entry point for parsing SQL expressions expressed as a String.
     *
     * @param sqlExpression the SQL expression to parse
     * @param inputRowType the fields available in the SQL expression
     * @param outputType expected top-level output type if available
     * @return resolved expression
     * @throws org.apache.flink.table.api.SqlParserException when failed to parse the sql expression
     */
    ResolvedExpression parseSqlExpression(
            String sqlExpression, RowType inputRowType, @Nullable LogicalType outputType);

    /**
     * 返回给定语句在给定游标位置的完成提示。完成不区分大小写。
     *
     * Returns completion hints for the given statement at the given cursor position. The completion
     * happens case insensitively.
     *
     * @param statement Partial or slightly incorrect SQL statement
     * @param position cursor position
     * @return completion hints that fit at the current cursor position
     */
    String[] getCompletionHints(String statement, int position);
}
