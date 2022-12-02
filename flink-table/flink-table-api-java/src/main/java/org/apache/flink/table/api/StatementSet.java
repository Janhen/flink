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
 * {@link StatementSet}接受DML语句或{@link Table}，规划器可以将所有添加的语句和Table一起优化，然后作为一个作业提交。
 *
 * <p>添加的语句和表将在调用' execute '方法时被清除。
 *
 * A {@link StatementSet} accepts DML statements or {@link Table}s, the planner can optimize all
 * added statements and Tables together and then submit as one job.
 *
 * <p>The added statements and Tables will be cleared when calling the `execute` method.
 */
@PublicEvolving
public interface StatementSet {

    /** add insert statement to the set. */
    // 向集合中添加插入语句
    StatementSet addInsertSql(String statement);

    /** add Table with the given sink table name to the set. */
    // 将指定接收器表名的 Table 添加到集合中
    StatementSet addInsert(String targetPath, Table table);

    /** add {@link Table} with the given sink table name to the set. */
    // 将{@link Table}与给定的接收器表名添加到集合中
    StatementSet addInsert(String targetPath, Table table, boolean overwrite);

    /**
     * 返回AST和执行计划，以计算所有语句和Tables的结果
     *
     * returns the AST and the execution plan to compute the result of the all statements and
     * Tables.
     *
     * @param extraDetails The extra explain details which the explain result should include, e.g.
     *     estimated cost, changelog mode for streaming, displaying execution plan in json format
     * @return AST and the execution plan.
     */
    String explain(ExplainDetail... extraDetails);

    /**
     * 批量执行所有语句和表
     *
     * <p>执行此方法时，添加的语句和表将被清除。
     *
     * execute all statements and Tables as a batch.
     *
     * <p>The added statements and Tables will be cleared when executing this method.
     */
    TableResult execute();
}
