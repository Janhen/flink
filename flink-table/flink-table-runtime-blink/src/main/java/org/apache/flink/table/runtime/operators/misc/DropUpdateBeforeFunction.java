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

package org.apache.flink.table.runtime.operators.misc;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;

/**
 * 函数仅删除具有 {@link RowKind#UPDATE_BEFORE} 变更日志种类的行。这通常用作不需要 {@link RowKind#UPDATE_BEFORE}
 * 消息的下游运营商的优化，但上游运营商不能自行删除它。
 *
 * A function drops only rows with {@link RowKind#UPDATE_BEFORE} changelog kind. This is usually
 * used as an optimization for the downstream operators that doesn't need the {@link
 * RowKind#UPDATE_BEFORE} messages, but the upstream operator can't drop it by itself.
 */
public class DropUpdateBeforeFunction implements FilterFunction<RowData> {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean filter(RowData value) {
        return !RowKind.UPDATE_BEFORE.equals(value.getRowKind());
    }
}
