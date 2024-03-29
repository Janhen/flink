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

package org.apache.flink.table.data.util;

import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;

/** Utilities for {@link RowData}. */
public final class RowDataUtil {

    /**
     * 如果消息是 {@link RowKind#INSERT} 或 {@link RowKind#UPDATE_AFTER}，则返回 true，这是一个聚合的累积操作。
     *
     * Returns true if the message is either {@link RowKind#INSERT} or {@link RowKind#UPDATE_AFTER},
     * which refers to an accumulate operation of aggregation.
     */
    public static boolean isAccumulateMsg(RowData row) {
        RowKind kind = row.getRowKind();
        return kind == RowKind.INSERT || kind == RowKind.UPDATE_AFTER;
    }

    /**
     * 如果消息是{@link RowKind#DELETE}或{@link RowKind#UPDATE_BEFORE}，则返回true，该消息指向聚合的收回操作。
     *
     * Returns true if the message is either {@link RowKind#DELETE} or {@link
     * RowKind#UPDATE_BEFORE}, which refers to a retract operation of aggregation.
     */
    public static boolean isRetractMsg(RowData row) {
        RowKind kind = row.getRowKind();
        return kind == RowKind.UPDATE_BEFORE || kind == RowKind.DELETE;
    }
}
