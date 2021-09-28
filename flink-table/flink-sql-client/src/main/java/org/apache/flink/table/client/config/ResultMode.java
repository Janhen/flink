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

package org.apache.flink.table.client.config;

import org.apache.flink.types.RowKind;

/** The mode when display the result of the query in the sql client. */
public enum ResultMode {

    /** Collects results and returns them as table snapshots. */
    // 收集结果并将其作为表快照返回。
    TABLE,

    /** A result that is represented as a changelog consisting of records with {@link RowKind}. */
    // 表示为由{@link RowKind}的记录组成的更改日志的结果。
    CHANGELOG,

    /** Print result in tableau mode. */
    // 打印结果在tableau模式。
    TABLEAU
}
