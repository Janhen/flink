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

/**
 * {@link StatementSet}接受 DML 语句或 {@link Table}，planner 可以同时优化所有添加的语句和表，然后作为一个
 * 作业提交。
 *
 * <p>当调用 'execute' 方法时，添加的语句和表将被清除。
 *
 * A {@link StatementSet} accepts DML statements or {@link Table}s, the planner can optimize all
 * added statements and Tables together and then submit as one job.
 *
 * <p>The added statements and Tables will be cleared when calling the `execute` method.
 */
@PublicEvolving
public interface StatementSet {

    /** add insert statement to the set. */
    // 向集合中添加插入语句。
    StatementSet addInsertSql(String statement);

    /** add Table with the given sink table name to the set. */
    // 将具有给定接收器表名的 Table 添加到 set 中。
    StatementSet addInsert(String targetPath, Table table);

    /** add {@link Table} with the given sink table name to the set. */
    // 将带有给定接收器表名的 {@link Table} 添加到 set 中。
    StatementSet addInsert(String targetPath, Table table, boolean overwrite);

    /**
     * 返回 AST 和执行计划，用于计算所有语句和表的结果。
     *
     * returns the AST and the execution plan to compute the result of the all statements and
     * Tables.
     *
     * @param extraDetails The extra explain details which the explain result should include, e.g.
     *     estimated cost, changelog mode for streaming
     * @return AST and the execution plan.
     */
    String explain(ExplainDetail... extraDetails);

    /**
     * 批量执行所有语句和表。
     *
     * <p>执行此方法时，将清除添加的语句和表。
     *
     * execute all statements and Tables as a batch.
     *
     * <p>The added statements and Tables will be cleared when executing this method.
     */
    TableResult execute();
}
