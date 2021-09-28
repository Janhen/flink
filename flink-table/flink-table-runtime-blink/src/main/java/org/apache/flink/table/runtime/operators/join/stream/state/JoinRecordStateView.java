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

package org.apache.flink.table.runtime.operators.join.stream.state;

import org.apache.flink.table.data.RowData;

/**
 * 一个{@link JoinRecordStateView}是一个连接状态的视图。它封装了连接状态并提供了一些面向输入记录的api。连接状态
 * 用于存储输入记录。连接状态的结构取决于{@link JoinInputSideSpec}。
 *
 * <p>例如:当{@link JoinInputSideSpec}是JoinKeyContainsUniqueKey时，我们将使用
 *   {@link org.apache.flink.api.common.state.ValueState}来存储记录，这样性能更好。
 *
 * A {@link JoinRecordStateView} is a view to the join state. It encapsulates the join state and
 * provides some APIs facing the input records. The join state is used to store input records. The
 * structure of the join state is vary depending on the {@link JoinInputSideSpec}.
 *
 * <p>For example: when the {@link JoinInputSideSpec} is JoinKeyContainsUniqueKey, we will use
 * {@link org.apache.flink.api.common.state.ValueState} to store records which has better
 * performance.
 */
public interface JoinRecordStateView {

    /** Add a new record to the state view. */
    // 向状态视图添加一条新记录。
    void addRecord(RowData record) throws Exception;

    /** Retract the record from the state view. */
    // 从 state 视图中撤回记录。
    void retractRecord(RowData record) throws Exception;

    /** Gets all the records under the current context (i.e. join key). */
    // 获取当前上下文下的所有记录(即连接键)
    Iterable<RowData> getRecords() throws Exception;
}
