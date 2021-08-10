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
import org.apache.flink.table.expressions.Expression;

/** A table that has been grouped on a set of grouping keys. */
// 在一组分组键上分组的表。
@PublicEvolving
public interface GroupedTable {

    /**
     * 对分组表执行选择操作。类似于SQL SELECT语句。字段表达式可以包含复杂表达式和聚合。
     *
     * Performs a selection operation on a grouped table. Similar to an SQL SELECT statement. The
     * field expressions can contain complex expressions and aggregations.
     *
     * <p>Example:
     *
     * <pre>{@code
     * tab.groupBy("key").select("key, value.avg + ' The average' as average")
     * }</pre>
     *
     * @deprecated use {@link #select(Expression...)}
     */
    @Deprecated
    Table select(String fields);

    /**
     * Performs a selection operation on a grouped table. Similar to an SQL SELECT statement. The
     * field expressions can contain complex expressions and aggregations.
     *
     * <p>Example:
     *
     * <pre>{@code
     * tab.groupBy($("key")).select($("key"), $("value").avg().plus(" The average").as("average"));
     * }</pre>
     *
     * <p>Scala Example:
     *
     * <pre>{@code
     * tab.groupBy($"key").select($"key", $"value".avg + " The average" as "average")
     * }</pre>
     */
    Table select(Expression... fields);

    /**
     * Performs an aggregate operation with an aggregate function. You have to close the {@link
     * #aggregate(String)} with a select statement. The output will be flattened if the output type
     * is a composite type.
     *
     * <p>Example:
     *
     * <pre>{@code
     * AggregateFunction aggFunc = new MyAggregateFunction();
     * tableEnv.registerFunction("aggFunc", aggFunc);
     * table.groupBy("key")
     *   .aggregate("aggFunc(a, b) as (f0, f1, f2)")
     *   .select("key, f0, f1")
     * }</pre>
     *
     * @deprecated use {@link #aggregate(Expression)}
     */
    @Deprecated
    AggregatedTable aggregate(String aggregateFunction);

    /**
     * 使用聚合函数执行聚合操作。你必须用一个select语句关闭 {@link #aggregate(Expression)}。如果输出类型是
     * 复合类型，则输出将被平展。
     *
     * Performs an aggregate operation with an aggregate function. You have to close the {@link
     * #aggregate(Expression)} with a select statement. The output will be flattened if the output
     * type is a composite type.
     *
     * <p>Example:
     *
     * <pre>{@code
     * AggregateFunction aggFunc = new MyAggregateFunction();
     * tableEnv.registerFunction("aggFunc", aggFunc);
     * tab.groupBy($("key"))
     *   .aggregate(call("aggFunc", $("a"), $("b")).as("f0", "f1", "f2"))
     *   .select($("key"), $("f0"), $("f1"));
     * }</pre>
     *
     * <p>Scala Example:
     *
     * <pre>{@code
     * val aggFunc = new MyAggregateFunction
     * table.groupBy($"key")
     *   .aggregate(aggFunc($"a", $"b") as ("f0", "f1", "f2"))
     *   .select($"key", $"f0", $"f1")
     * }</pre>
     */
    AggregatedTable aggregate(Expression aggregateFunction);

    /**
     * Performs a flatAggregate operation on a grouped table. FlatAggregate takes a
     * TableAggregateFunction which returns multiple rows. Use a selection after flatAggregate.
     *
     * <p>Example:
     *
     * <pre>{@code
     * val tableAggFunc: TableAggregateFunction = new MyTableAggregateFunction
     * tableEnv.registerFunction("tableAggFunc", tableAggFunc);
     * tab.groupBy("key")
     *   .flatAggregate("tableAggFunc(a, b) as (x, y, z)")
     *   .select("key, x, y, z")
     * }</pre>
     *
     * @deprecated use {@link #flatAggregate(Expression)}
     */
    @Deprecated
    FlatAggregateTable flatAggregate(String tableAggFunction);

    /**
     * 对分组表执行 flatAggregate 操作。FlatAggregate 接受一个 TableAggregateFunction，它返回多行。
     * 在 flatAggregate 后面使用选择。
     *
     * Performs a flatAggregate operation on a grouped table. FlatAggregate takes a
     * TableAggregateFunction which returns multiple rows. Use a selection after flatAggregate.
     *
     * <p>Example:
     *
     * <pre>{@code
     * TableAggregateFunction tableAggFunc = new MyTableAggregateFunction();
     * tableEnv.registerFunction("tableAggFunc", tableAggFunc);
     * tab.groupBy($("key"))
     *   .flatAggregate(call("tableAggFunc", $("a"), $("b")).as("x", "y", "z"))
     *   .select($("key"), $("x"), $("y"), $("z"));
     * }</pre>
     *
     * <p>Scala Example:
     *
     * <pre>{@code
     * val tableAggFunc: TableAggregateFunction = new MyTableAggregateFunction
     * tab.groupBy($"key")
     *   .flatAggregate(tableAggFunc($"a", $"b") as ("x", "y", "z"))
     *   .select($"key", $"x", $"y", $"z")
     * }</pre>
     */
    FlatAggregateTable flatAggregate(Expression tableAggFunction);
}
