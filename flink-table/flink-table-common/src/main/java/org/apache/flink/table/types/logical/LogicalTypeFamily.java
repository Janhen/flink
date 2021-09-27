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

package org.apache.flink.table.types.logical;

import org.apache.flink.annotation.PublicEvolving;

/**
 * 用于将{@link LogicalTypeRoot}集群为类别的逻辑类型族的枚举。
 *
 * <p>枚举在命名和完整性方面非常接近SQL标准。然而，它只是反映了正在发展的标准的一个子集，并包含了一些扩展(由
 *   {@code EXTENSION}表示)。
 *
 * An enumeration of logical type families for clustering {@link LogicalTypeRoot}s into categories.
 *
 * <p>The enumeration is very close to the SQL standard in terms of naming and completeness.
 * However, it reflects just a subset of the evolving standard and contains some extensions
 * (indicated by {@code EXTENSION}).
 */
@PublicEvolving
public enum LogicalTypeFamily {
    // 预定义的
    PREDEFINED,

    // 构造的
    CONSTRUCTED,

    // 用户定义的
    USER_DEFINED,

    // 字符串
    CHARACTER_STRING,

    // 二进制字符串
    BINARY_STRING,

    // 数字
    NUMERIC,

    // 整形
    INTEGER_NUMERIC,

    // 精确的数字 (DECIMAL?)
    EXACT_NUMERIC,

    // 模糊的数字 (FLOAT?)
    APPROXIMATE_NUMERIC,

    // 日期时间
    DATETIME,

    // 时间
    TIME,

    // 时间戳
    TIMESTAMP,

    // 时间间隔
    INTERVAL,

    // 集合
    COLLECTION,

    // 扩展
    EXTENSION
}
