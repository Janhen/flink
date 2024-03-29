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

package org.apache.flink.table.utils;

import org.apache.flink.annotation.Internal;

/** Utilities for table sources and sinks. */
// 用于表源和接收器的实用程序。
@Internal
public final class TableConnectorUtils {

    private TableConnectorUtils() {
        // do not instantiate
    }

    /** Returns the table connector name used for logging and web UI. */
    // 返回用于日志和 web UI 的表连接器名称。
    public static String generateRuntimeName(Class<?> clazz, String[] fields) {
        String className = clazz.getSimpleName();
        if (null == fields) {
            return className + "(*)";
        } else {
            return className + "(" + String.join(", ", fields) + ")";
        }
    }
}
