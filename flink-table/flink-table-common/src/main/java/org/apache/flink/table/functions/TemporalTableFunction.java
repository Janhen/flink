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

package org.apache.flink.table.functions;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.types.Row;

/**
 * 表示某个历史表上的时态表函数的类。 {@link TemporalTableFunction} 也是 {@link TableFunction} 的一个实例。
 *
 * <p>目前 {@link TemporalTableFunction} 仅在流媒体中受支持。
 *
 * Class representing temporal table function over some history table. A {@link
 * TemporalTableFunction} is also an instance of {@link TableFunction}.
 *
 * <p>Currently {@link TemporalTableFunction}s are only supported in streaming.
 */
@PublicEvolving
public abstract class TemporalTableFunction extends TableFunction<Row> {}
