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

package org.apache.flink.streaming.api.operators.async.queue;

import org.apache.flink.annotation.Internal;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.operators.TimestampedCollector;
import org.apache.flink.streaming.api.operators.async.AsyncWaitOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamElement;

import java.util.List;
import java.util.Optional;

/** Interface for stream element queues for the {@link AsyncWaitOperator}. */
// {@link AsyncWaitOperator} 的流元素队列接口。
@Internal
public interface StreamElementQueue<OUT> {

    /**
     * 尝试将给定元素放入队列。如果队列有剩余容量，则此操作成功，如果队列已满，则此操作失败。
     *
     * <p>此方法返回插入元素的句柄，允许设置计算结果。
     *
     * Tries to put the given element in the queue. This operation succeeds if the queue has
     * capacity left and fails if the queue is full.
     *
     * <p>This method returns a handle to the inserted element that allows to set the result of the
     * computation.
     *
     * @param streamElement the element to be inserted.
     * @return A handle to the element if successful or {@link Optional#empty()} otherwise.
     */
    Optional<ResultFuture<OUT>> tryPut(StreamElement streamElement);

    /**
     * 从这个队列的头部发出一个已完成的元素到给定的输出中。
     *
     * <p>如果没有完成任何元素，则不会发出任何元素（在进入任何临界区之前检查 {@link #hasCompletedElements()}）。
     *
     * Emits one completed element from the head of this queue into the given output.
     *
     * <p>Will not emit any element if no element has been completed (check {@link
     * #hasCompletedElements()} before entering any critical section).
     *
     * @param output the output into which to emit
     */
    void emitCompletedElement(TimestampedCollector<OUT> output);

    /**
     * 检查是否至少有一个完整的头部元素。
     *
     * Checks if there is at least one completed head element.
     *
     * @return True if there is a completed head element.
     */
    boolean hasCompletedElements();

    /**
     * 返回当前包含在此队列中的 {@link StreamElement} 集合以进行检查点。
     *
     * <p>这包括所有未发出、已完成和未完成的元素。
     *
     * Returns the collection of {@link StreamElement} currently contained in this queue for
     * checkpointing.
     *
     * <p>This includes all non-emitted, completed and non-completed elements.
     *
     * @return List of currently contained {@link StreamElement}.
     */
    List<StreamElement> values();

    /**
     * 如果队列为空，则为真；否则为假。
     *
     * True if the queue is empty; otherwise false.
     *
     * @return True if the queue is empty; otherwise false.
     */
    boolean isEmpty();

    /**
     * 返回队列的大小。
     *
     * Return the size of the queue.
     *
     * @return The number of elements contained in this queue.
     */
    int size();
}
