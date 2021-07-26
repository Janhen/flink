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
 * 枚举有效的SQL兼容模式。
 *
 * <p>在大多数情况下，内置的兼容性模式应该足够了。对于一些特性，例如“INSERT INTO T PARTITION(a='xxx')…”语法，如果
 * 需要，你可能需要切换到 Hive 方言。
 *
 * <p>将来我们可能会引入其他 SQL 方言。
 *
 * Enumeration of valid SQL compatibility modes.
 *
 * <p>In most of the cases, the built-in compatibility mode should be sufficient. For some features,
 * i.e. the "INSERT INTO T PARTITION(a='xxx') ..." grammar, you may need to switch to the Hive
 * dialect if required.
 *
 * <p>We may introduce other SQL dialects in the future.
 */
@PublicEvolving
public enum SqlDialect {

    /** Flink's default SQL behavior. */
    DEFAULT,

    /**
     * SQL dialect that allows some Apache Hive specific grammar.
     *
     * <p>Note: We might never support all of the Hive grammar. See the documentation for supported
     * features.
     */
    HIVE
}
