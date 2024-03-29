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

package org.apache.flink.table.connector.sink.abilities;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.connector.sink.DynamicTableSink;

import java.util.Map;

/**
 * 允许在 {@link DynamicTableSink} 中写入分区数据。
 *
 * <p>Partitions 将存储在外部系统中的数据分割为更小的部分，由一个或多个基于字符串的分区键标识。单个分区表示为
 *    {@code Map <String, String>}，它将每个分区键映射到一个分区值。分区键及其顺序由目录表定义。
 *
 * <p>例如，可以按区域进行分区，也可以在按月分区的区域内进行分区。分区键的顺序(在本例中:先按区域，然后按月份)由编目
 *   表定义。分区列表可以是:
 *
 * <pre>
 *   List(
 *     ['region'='europe', 'month'='2020-01'],
 *     ['region'='europe', 'month'='2020-02'],
 *     ['region'='asia', 'month'='2020-01'],
 *     ['region'='asia', 'month'='2020-02']
 *   )
 * </pre>
 *
 * <p>给出以下分区表:
 *
 * ...
 *
 * <p>如果所有分区键都在 {@code PARTITION} 子句中分配了一个值，则该操作被视为“插入静态分区”。在上面的示例中，
 * 查询结果应写入静态分区 {@code region='europe', month='2020-01'}，规划器将其传递给
 * {@link #applyStaticPartition(Map)}。规划器还能够从查询的文字中派生出静态分区：
 *
 * ...
 *
 * <p>如果只有所有分区键的子集在 {@code PARTITION} 子句中获得静态值或在 {@code SELECT} 子句中具有常量部分，则
 * 该操作被视为“插入动态分区” .在上面的例子中，静态分区部分是{@code region='europe'}，它将被规划器传递给
 * {@link #applyStaticPartition(Map)}。分区键的其余值应在运行时由接收器从每个单独的记录中获取。在上面的两个示例中，
 * {@code month} 字段是动态分区键。
 *
 * <p>如果 {@code PARTITION} 子句不包含静态赋值或完全省略，则分区键的所有值要么从查询的静态部分派生，要么动态获取。
 *
 * Enables to write partitioned data in a {@link DynamicTableSink}.
 *
 * <p>Partitions split the data stored in an external system into smaller portions that are
 * identified by one or more string-based partition keys. A single partition is represented as a
 * {@code Map < String, String >} which maps each partition key to a partition value. Partition keys
 * and their order are defined by the catalog table.
 *
 * <p>For example, data can be partitioned by region and within a region partitioned by month. The
 * order of the partition keys (in the example: first by region then by month) is defined by the
 * catalog table. A list of partitions could be:
 *
 * <pre>
 *   List(
 *     ['region'='europe', 'month'='2020-01'],
 *     ['region'='europe', 'month'='2020-02'],
 *     ['region'='asia', 'month'='2020-01'],
 *     ['region'='asia', 'month'='2020-02']
 *   )
 * </pre>
 *
 * <p>Given the following partitioned table:
 *
 * <pre>{@code
 * CREATE TABLE t (a INT, b STRING, c DOUBLE, region STRING, month STRING) PARTITION BY (region, month);
 * }</pre>
 *
 * <p>We can insert data into <i>static table partitions</i> using the {@code INSERT INTO ...
 * PARTITION} syntax:
 *
 * <pre>{@code
 * INSERT INTO t PARTITION (region='europe', month='2020-01') SELECT a, b, c FROM my_view;
 * }</pre>
 *
 * <p>If all partition keys get a value assigned in the {@code PARTITION} clause, the operation is
 * considered an "insertion into a static partition". In the above example, the query result should
 * be written into the static partition {@code region='europe', month='2020-01'} which will be
 * passed by the planner into {@link #applyStaticPartition(Map)}. The planner is also able to
 * derived static partitions from literals of a query:
 *
 * <pre>{@code
 * INSERT INTO t SELECT a, b, c, 'asia' AS region, '2020-01' AS month FROM my_view;
 * }</pre>
 *
 * <p>Alternatively, we can insert data into <i>dynamic table partitions</i> using the SQL syntax:
 *
 * <pre>{@code
 * INSERT INTO t PARTITION (region='europe') SELECT a, b, c, month FROM another_view;
 * }</pre>
 *
 * <p>If only a subset of all partition keys get a static value assigned in the {@code PARTITION}
 * clause or with a constant part in a {@code SELECT} clause, the operation is considered an
 * "insertion into a dynamic partition". In the above example, the static partition part is {@code
 * region='europe'} which will be passed by the planner into {@link #applyStaticPartition(Map)}. The
 * remaining values for partition keys should be obtained from each individual record by the sink
 * during runtime. In the two examples above, the {@code month} field is the dynamic partition key.
 *
 * <p>If the {@code PARTITION} clause contains no static assignments or is omitted entirely, all
 * values for partition keys are either derived from static parts of the query or obtained
 * dynamically.
 */
@PublicEvolving
public interface SupportsPartitioning {

    /**
     * 提供分区的静态部分
     *
     * <p>单个分区将每个分区键映射到一个分区值。根据用户定义的语句，分区可能不包括所有分区键。
     *
     * <p>参见 {@link SupportsPartitioning} 的文档获取更多信息。
     *
     * Provides the static part of a partition.
     *
     * <p>A single partition maps each partition key to a partition value. Depending on the
     * user-defined statement, the partition might not include all partition keys.
     *
     * <p>See the documentation of {@link SupportsPartitioning} for more information.
     *
     * @param partition user-defined (possibly partial) static partition
     */
    void applyStaticPartition(Map<String, String> partition);

    /**
     * 返回数据在被接收器使用之前是否需要按分区分组。默认情况下，运行时不需要这样做，记录以任意分区顺序到达。
     *
     * Returns whether data needs to be grouped by partition before it is consumed by the sink. By
     * default, this is not required from the runtime and records arrive in arbitrary partition
     * order.
     *
     * <p>If this method returns true, the sink can expect that all records will be grouped by the
     * partition keys before consumed by the sink. In other words: The sink will receive all
     * elements of one partition and then all elements of another partition. Elements of different
     * partitions will not be mixed. For some sinks, this can be used to reduce the number of
     * partition writers and improve writing performance by writing one partition at a time.
     *
     * <p>The given argument indicates whether the current execution mode supports grouping or not.
     * For example, depending on the execution mode a sorting operation might not be available
     * during runtime.
     *
     * @param supportsGrouping whether the current execution mode supports grouping
     * @return whether data need to be grouped by partition before consumed by the sink. If {@code
     *     supportsGrouping} is false, it should never return true, otherwise the planner will fail.
     */
    @SuppressWarnings("unused")
    default boolean requiresPartitionGrouping(boolean supportsGrouping) {
        return false;
    }
}
