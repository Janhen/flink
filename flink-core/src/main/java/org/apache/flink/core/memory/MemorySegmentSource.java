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

package org.apache.flink.core.memory;

import org.apache.flink.annotation.Internal;

/** Interface describing entities that can provide memory segments. */
// 描述可以提供内存段的实体的接口
@Internal
public interface MemorySegmentSource {

    /**
     * 获取下一个内存段。如果没有更多可用段，则返回 null
     *
     * Gets the next memory segment. If no more segments are available, it returns null.
     *
     * @return The next memory segment, or null, if none is available.
     */
    MemorySegment nextSegment();
}
