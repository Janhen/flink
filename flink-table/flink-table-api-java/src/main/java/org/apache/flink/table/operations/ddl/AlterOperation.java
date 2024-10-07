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

package org.apache.flink.table.operations.ddl;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.operations.Operation;

/**
 * 描述DDL语句的{@link Operation}，例如ALTER TABLE或ALTER DATABASE。
 *
 * <p>不同的子操作可以有其特殊的目标名称。例如，alter table操作可能有一个目标表名和一个标志来描述它是否存在。
 *
 * A {@link Operation} that describes the DDL statements, e.g. ALTER TABLE or ALTER DATABASE.
 *
 * <p>Different sub operations can have their special target name. For example, a alter table
 * operation may have a target table name and a flag to describe if is exists.
 */
@Internal
public interface AlterOperation extends Operation {}
