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
 * 通用接口的各种表达式。
 *
 * <p>表达式表示用于生成计算结果的逻辑树。每个表达式由零个、一个或多个子表达式组成。表达式可以是文字值、函数调用或
 *   字段引用。
 *
 * <p>表达式是API的一部分。它们可能在API堆栈中被转换多次，直到它们完全{@link ResolvedExpression}s。值类型和
 *   输出类型表示为{@link DataType}的实例。
 *
 * General interface for all kinds of expressions.
 *
 * <p>Expressions represent a logical tree for producing a computation result. Every expression
 * consists of zero, one, or more subexpressions. Expressions might be literal values, function
 * calls, or field references.
 *
 * <p>Expressions are part of the API. They might be transformed multiple times within the API stack
 * until they are fully {@link ResolvedExpression}s. Value types and output types are expressed as
 * instances of {@link DataType}.
 */
@PublicEvolving
public interface Expression {

    /**
     * Returns a string that summarizes this expression for printing to a console. An implementation
     * might skip very specific properties.
     *
     * @return summary string of this expression for debugging purposes
     */
    String asSummaryString();

    List<Expression> getChildren();

    <R> R accept(ExpressionVisitor<R> visitor);
}
