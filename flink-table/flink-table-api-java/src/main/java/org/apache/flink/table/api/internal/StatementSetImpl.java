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

package org.apache.flink.table.api.internal;

import org.apache.flink.annotation.Experimental;
import org.apache.flink.annotation.Internal;
import org.apache.flink.table.api.ExplainDetail;
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.catalog.UnresolvedIdentifier;
import org.apache.flink.table.operations.CatalogSinkModifyOperation;
import org.apache.flink.table.operations.ModifyOperation;
import org.apache.flink.table.operations.Operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Implementation for {@link StatementSet}. */
@Internal
class StatementSetImpl implements StatementSet {
    private final TableEnvironmentInternal tableEnvironment;
    // J: insert ... 操作(DML)
    private final List<ModifyOperation> operations = new ArrayList<>();

    protected StatementSetImpl(TableEnvironmentInternal tableEnvironment) {
        this.tableEnvironment = tableEnvironment;
    }

    @Override
    public StatementSet addInsertSql(String statement) {
        List<Operation> operations = tableEnvironment.getParser().parse(statement);

        if (operations.size() != 1) {
            throw new TableException("Only single statement is supported.");
        }

        Operation operation = operations.get(0);
        if (operation instanceof ModifyOperation) {
            this.operations.add((ModifyOperation) operation);
        } else {
            throw new TableException("Only insert statement is supported now.");
        }
        return this;
    }

    @Override
    public StatementSet addInsert(String targetPath, Table table) {
        return addInsert(targetPath, table, false);
    }

    @Override
    public StatementSet addInsert(String targetPath, Table table, boolean overwrite) {
        UnresolvedIdentifier unresolvedIdentifier =
                tableEnvironment.getParser().parseIdentifier(targetPath);
        ObjectIdentifier objectIdentifier =
                tableEnvironment.getCatalogManager().qualifyIdentifier(unresolvedIdentifier);

        operations.add(
                new CatalogSinkModifyOperation(
                        objectIdentifier,
                        table.getQueryOperation(),
                        Collections.emptyMap(),
                        overwrite,
                        Collections.emptyMap()));

        return this;
    }

    @Override
    public String explain(ExplainDetail... extraDetails) {
        List<Operation> operationList =
                operations.stream().map(o -> (Operation) o).collect(Collectors.toList());
        return tableEnvironment.explainInternal(operationList, extraDetails);
    }

    @Override
    public TableResult execute() {
        try {
            return tableEnvironment.executeInternal(operations);
        } finally {
            operations.clear();
        }
    }

    /**
     *
     * 作为批处理获得所有语句和Tables的json计划。
     *
     * <p>json计划是为语句和表优化的ExecNode计划的字符串json表示。ExecNode计划可以序列化为json计划，json计划可以
     *   反序列化为ExecNode计划。
     *
     * <p>在执行此方法时，不会清除添加的语句和表。
     *
     * <p>注意:只有Blink规划器支持此方法。
     *
     * <p><b>NOTES<b>:这是一个实验性的特性。
     *
     * Get the json plan of the all statements and Tables as a batch.
     *
     * <p>The json plan is the string json representation of an optimized ExecNode plan for the
     * statements and Tables. An ExecNode plan can be serialized to json plan, and a json plan can
     * be deserialized to an ExecNode plan.
     *
     * <p>The added statements and Tables will NOT be cleared when executing this method.
     *
     * <p>NOTES: Only the Blink planner supports this method.
     *
     * <p><b>NOTES</b>: This is an experimental feature now.
     *
     * @return the string json representation of an optimized ExecNode plan for the statements and
     *     Tables.
     */
    @Experimental
    public String getJsonPlan() {
        return tableEnvironment.getJsonPlan(operations);
    }
}
