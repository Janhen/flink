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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 对SQL表达式的调用。
 *
 * <p>给定的字符串在规划过程中被解析并转换为 {@link Expression}。在运行时只计算翻译后的表达式
 *
 * <p>注意:实际上，这个类属于{@code flink-table-api-java}模块，然而，由于这个表达式在定义可持久化的计算列和水印
 *   策略时对目录至关重要，我们将它保留在{@code flink-table-common}中，以保持目录的依赖性较低。
 *
 * A call to a SQL expression.
 *
 * <p>The given string is parsed and translated into an {@link Expression} during planning. Only the
 * translated expression is evaluated during runtime.
 *
 * <p>Note: Actually, this class belongs into the {@code flink-table-api-java} module, however,
 * since this expression is crucial for catalogs when defining persistable computed columns and
 * watermark strategies, we keep it in {@code flink-table-common} to keep the dependencies of
 * catalogs low.
 */
@PublicEvolving
public final class SqlCallExpression implements Expression {

    // indicates that this is an unresolved expression consistent with unresolved data types
    private static final String FORMAT = "[%s]";

    private final String sqlExpression;

    public SqlCallExpression(String sqlExpression) {
        this.sqlExpression = sqlExpression;
    }

    public String getSqlExpression() {
        return sqlExpression;
    }

    @Override
    public String asSummaryString() {
        return String.format(FORMAT, sqlExpression);
    }

    @Override
    public List<Expression> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SqlCallExpression that = (SqlCallExpression) o;
        return sqlExpression.equals(that.sqlExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlExpression);
    }

    @Override
    public String toString() {
        return asSummaryString();
    }
}
