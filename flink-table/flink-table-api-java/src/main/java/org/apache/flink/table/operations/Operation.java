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

package org.apache.flink.table.operations;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.delegation.Parser;
import org.apache.flink.table.delegation.Planner;

/**
 * 涵盖所有类型的表操作，如查询(DQL)、修改(DML)、定义(DDL)或控制操作(DCL)。这是{@link Planner#getParser()}
 * 和{@link Parser#parse(String)}的输出。
 *
 * Covers all sort of Table operations such as queries(DQL), modifications(DML), definitions(DDL),
 * or control actions(DCL). This is the output of {@link Planner#getParser()} and {@link
 * Parser#parse(String)}.
 *
 * @see QueryOperation
 * @see ModifyOperation
 */
@PublicEvolving
public interface Operation {
    /**
     * 返回一个字符串，该字符串汇总此操作以便打印到控制台。实现可能会跳过非常特定的属性。
     *
     * Returns a string that summarizes this operation for printing to a console. An implementation
     * might skip very specific properties.
     *
     * @return summary string of this operation for debugging purposes
     */
    String asSummaryString();
}
