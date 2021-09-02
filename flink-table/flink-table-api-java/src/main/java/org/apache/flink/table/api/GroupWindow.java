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

package org.apache.flink.table.api;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.expressions.ApiExpressionUtils;
import org.apache.flink.table.expressions.Expression;

/**
 * 组窗口规范。
 *
 * <p>Group 窗口基于时间或行计数间隔对行进行分组，因此本质上是一种特殊类型的 groupBy。就像 groupBy 一样，组窗口
 *   允许计算元素组的聚合。
 *
 * <p>无限流表只能按时间或行间隔分组。因此需要窗口分组来在流表上应用聚合。
 *
 * <p>对于有限批处理表，组窗口为基于时间的 groupBy 提供了快捷方式。
 *
 * A group window specification.
 *
 * <p>Group windows group rows based on time or row-count intervals and is therefore essentially a
 * special type of groupBy. Just like groupBy, group windows allow to compute aggregates on groups
 * of elements.
 *
 * <p>Infinite streaming tables can only be grouped into time or row intervals. Hence window
 * grouping is required to apply aggregations on streaming tables.
 *
 * <p>For finite batch tables, group windows provide shortcuts for time-based groupBy.
 */
@PublicEvolving
public abstract class GroupWindow {

    /** Alias name for the group window. */
    // 组窗口的别名。
    private final Expression alias;

    private final Expression timeField;

    GroupWindow(Expression alias, Expression timeField) {
        this.alias = ApiExpressionUtils.unwrapFromApi(alias);
        this.timeField = ApiExpressionUtils.unwrapFromApi(timeField);
    }

    public Expression getAlias() {
        return alias;
    }

    public Expression getTimeField() {
        return timeField;
    }
}
