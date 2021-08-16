/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.state.heap;

import org.apache.flink.annotation.Internal;

/**
 * 可以由{@link HeapPriorityQueue} 管理的对象的接口。这样的对象一次只能包含在一个 {@link HeapPriorityQueue} 中。
 *
 * Interface for objects that can be managed by a {@link HeapPriorityQueue}. Such an object can only
 * be contained in at most one {@link HeapPriorityQueue} at a time.
 */
@Internal
public interface HeapPriorityQueueElement {

    /**
     * The index that indicates that a {@link HeapPriorityQueueElement} object is not contained in
     * and managed by any {@link HeapPriorityQueue}. We do not strictly enforce that internal
     * indexes must be reset to this value when elements are removed from a {@link
     * HeapPriorityQueue}.
     */
    int NOT_CONTAINED = Integer.MIN_VALUE;

    /**
     * 返回该对象在 {@link HeapPriorityQueue} 的内部数组中的当前索引。
     *
     * Returns the current index of this object in the internal array of {@link HeapPriorityQueue}.
     */
    int getInternalIndex();

    /**
     * 设置该对象在 {@link HeapPriorityQueue} 中的当前索引，并且只能由所属的 {@link HeapPriorityQueue} 调用。
     *
     * Sets the current index of this object in the {@link HeapPriorityQueue} and should only be
     * called by the owning {@link HeapPriorityQueue}.
     *
     * @param newIndex the new index in the timer heap.
     */
    void setInternalIndex(int newIndex);
}
