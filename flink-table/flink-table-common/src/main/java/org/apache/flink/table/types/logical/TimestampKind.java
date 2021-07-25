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

import org.apache.flink.annotation.Internal;

/**
 * 用于将时间属性元数据附加到带有或不带有时区的时间戳的内部时间戳类型。
 *
 * Internal timestamp kind for attaching time attribute metadata to timestamps with or without a
 * time zone.
 */
@Internal
public enum TimestampKind {
    // J: 常规的
    REGULAR,

    ROWTIME,

    // J: 处理时间?
    PROCTIME
}
