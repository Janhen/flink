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

package org.apache.flink.api.common.io;

import org.apache.flink.annotation.Public;

/**
 * 此接口充当无法拆分的输入的输入格式的标记。具有非并行输入格式的数据源始终以 1 的并行度执行。
 *
 * This interface acts as a marker for input formats for inputs which cannot be split. Data sources
 * with a non-parallel input formats are always executed with a parallelism of one.
 *
 * @see InputFormat
 */
@Public
public interface NonParallelInput {}
