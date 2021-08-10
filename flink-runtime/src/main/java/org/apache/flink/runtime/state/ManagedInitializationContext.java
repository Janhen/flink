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

package org.apache.flink.runtime.state;

import org.apache.flink.api.common.state.KeyedStateStore;
import org.apache.flink.api.common.state.OperatorStateStore;

/**
 * 该接口提供了一个上下文，操作符可以通过注册到托管状态(即由状态后端管理的状态)来进行初始化。
 *
 * <p>操作符状态对所有操作符可用，而 keyyed 状态只对 keyBy 后的操作符可用。
 *
 * <p>为了初始化，如果状态是空的(new 操作符)，或者是从以前执行这个操作符恢复的，上下文会发出信号。
 *
 * This interface provides a context in which operators can initialize by registering to managed
 * state (i.e. state that is managed by state backends).
 *
 * <p>Operator state is available to all operators, while keyed state is only available for
 * operators after keyBy.
 *
 * <p>For the purpose of initialization, the context signals if the state is empty (new operator) or
 * was restored from a previous execution of this operator.
 */
public interface ManagedInitializationContext {

    /**
     * 如果状态已从上一次执行的快照恢复，则返回 true。对于无状态任务，它总是返回 false。
     *
     * Returns true, if state was restored from the snapshot of a previous execution. This returns
     * always false for stateless tasks.
     */
    boolean isRestored();

    /** Returns an interface that allows for registering operator state with the backend. */
    // 返回一个接口，该接口允许向后端注册操作符状态。
    OperatorStateStore getOperatorStateStore();

    /** Returns an interface that allows for registering keyed state with the backend. */
    // 返回一个接口，允许在后端注册键控状态。
    KeyedStateStore getKeyedStateStore();
}
