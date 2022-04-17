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

package org.apache.flink.formats.common;

import org.apache.flink.annotation.Internal;

/** Timestamp format Enums. */
// 时间戳格式枚举。
@Internal
public enum TimestampFormat {
    /**
     * 指定TIMESTAMPTIMESTAMP_WITH_LOCAL_ZONE格式的选项。它将解析“yyyy-MM-dd HH:mm:ss”格式的TIMESTAMP。
     * 格式为"yyyy-MM-dd HH:mm:ss. s{precision}"， TIMESTAMP_WITH_LOCAL_TIMEZONE格式为
     * "yyyy-MM-dd HH:mm:ss. "。s{precision}'Z'"，并以相同的格式输出。
     *
     * Options to specify TIMESTAMP/TIMESTAMP_WITH_LOCAL_ZONE format. It will parse TIMESTAMP in
     * "yyyy-MM-dd HH:mm:ss.s{precision}" format, TIMESTAMP_WITH_LOCAL_TIMEZONE in "yyyy-MM-dd
     * HH:mm:ss.s{precision}'Z'" and output in the same format.
     */
    SQL,

    /**
     * 指定TIMESTAMPTIMESTAMP_WITH_LOCAL_ZONE格式的选项。它将解析“yyyy-MM-ddTHH:mm:ss”格式的TIMESTAMP。
     * 格式为“yyyy-MM-ddTHH:mm:ss. s{precision}”。s{precision}'Z'"，并以相同的格式输出。
     *
     * Options to specify TIMESTAMP/TIMESTAMP_WITH_LOCAL_ZONE format. It will parse TIMESTAMP in
     * "yyyy-MM-ddTHH:mm:ss.s{precision}" format, TIMESTAMP_WITH_LOCAL_TIMEZONE in
     * "yyyy-MM-ddTHH:mm:ss.s{precision}'Z'" and output in the same format.
     */
    ISO_8601
}
