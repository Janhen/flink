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

import java.io.IOException;
import java.util.Collection;

/** The provider used for requesting and releasing batch of memory segments. */
// 用于请求和释放批量内存段的提供程序
public interface MemorySegmentProvider {
    Collection<MemorySegment> requestMemorySegments(int numberOfSegmentsToRequest)
            throws IOException;

    void recycleMemorySegments(Collection<MemorySegment> segments) throws IOException;
}
