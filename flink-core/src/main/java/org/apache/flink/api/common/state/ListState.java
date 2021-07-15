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

package org.apache.flink.api.common.state;

import org.apache.flink.annotation.PublicEvolving;

import java.util.List;

/**
 * {@link State}接口用于操作中的分区列表状态。状态由用户函数访问和修改，系统始终作为分布式快照的一部分进行检查点。
 * 状态可以是键列表状态或操作符列表状态。
 *
 * <p>当它是一个关键列表状态时，它被应用在{@code KeyedStream}上的函数访问。
 *
 * <p>键由系统自动提供，因此函数总是看到映射到当前元素键的值。这样，系统就可以一致地处理流和状态分区。
 *
 * <p>当它是一个操作符列表状态时，该列表是一组状态项的集合，这些状态项彼此独立，在操作符并行性发生变化时，可以跨操作符实
 * 例重新分配。
 *
 * {@link State} interface for partitioned list state in Operations. The state is accessed and
 * modified by user functions, and checkpointed consistently by the system as part of the
 * distributed snapshots.
 *
 * <p>The state can be a keyed list state or an operator list state.
 *
 * <p>When it is a keyed list state, it is accessed by functions applied on a {@code KeyedStream}.
 * The key is automatically supplied by the system, so the function always sees the value mapped to
 * the key of the current element. That way, the system can handle stream and state partitioning
 * consistently together.
 *
 * <p>When it is an operator list state, the list is a collection of state items that are
 * independent from each other and eligible for redistribution across operator instances in case of
 * changed operator parallelism.
 *
 * @param <T> Type of values that this list state keeps.
 */
@PublicEvolving
public interface ListState<T> extends MergingState<T, Iterable<T>> {

    /**
     * Updates the operator state accessible by {@link #get()} by updating existing values to to the
     * given list of values. The next time {@link #get()} is called (for the same state partition)
     * the returned state will represent the updated list.
     *
     * <p>If null or an empty list is passed in, the state value will be null.
     *
     * @param values The new values for the state.
     * @throws Exception The method may forward exception thrown internally (by I/O or functions).
     */
    void update(List<T> values) throws Exception;

    /**
     * Updates the operator state accessible by {@link #get()} by adding the given values to
     * existing list of values. The next time {@link #get()} is called (for the same state
     * partition) the returned state will represent the updated list.
     *
     * <p>If null or an empty list is passed in, the state value remains unchanged.
     *
     * @param values The new values to be added to the state.
     * @throws Exception The method may forward exception thrown internally (by I/O or functions).
     */
    void addAll(List<T> values) throws Exception;
}
