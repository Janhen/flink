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

/**
 * 所有由{@link StateBackend}和任务中的其他组件拍摄的快照的基础。
 *
 * <p>每个快照都由一个{@link StateObject}的集合组成，其中一些可能被其他检查点引用。当句柄被
 * {@link org.apache.flink.runtime.checkpoint.CheckpointCoordinator}接收时，共享状态将在给定的
 * {@link SharedStateRegistry}处注册，并且当检查点被丢弃时将被丢弃。
 *
 * <p>The {@link SharedStateRegistry}负责丢弃已注册的共享状态。在通过
 * {@link #registerSharedStates(SharedStateRegistry)}第一次注册之前，新创建的共享状态仍然由这个句柄拥有，并被
 * 认为是私有状态，直到它第一次注册。注册将所有权转移到{@link SharedStateRegistry}。复合状态句柄应该只删除
 * {@link StateObject#discardState()}方法中的所有私有状态，{@link SharedStateRegistry}负责在共享状态注册后删
 * 除它们。
 *
 * Base of all snapshots that are taken by {@link StateBackend}s and some other components in tasks.
 *
 * <p>Each snapshot is composed of a collection of {@link StateObject}s some of which may be
 * referenced by other checkpoints. The shared states will be registered at the given {@link
 * SharedStateRegistry} when the handle is received by the {@link
 * org.apache.flink.runtime.checkpoint.CheckpointCoordinator} and will be discarded when the
 * checkpoint is discarded.
 *
 * <p>The {@link SharedStateRegistry} is responsible for the discarding of registered shared states.
 * Before their first registration through {@link #registerSharedStates(SharedStateRegistry)}, newly
 * created shared state is still owned by this handle and considered as private state until it is
 * registered for the first time. Registration transfers ownership to the {@link
 * SharedStateRegistry}. The composite state handle should only delete all private states in the
 * {@link StateObject#discardState()} method, the {@link SharedStateRegistry} is responsible for
 * deleting shared states after they were registered.
 */
public interface CompositeStateHandle extends StateObject {

    /**
     * 在给定的{@link SharedStateRegistry}中注册新创建的和已经引用的共享状态。当检查点成功完成或从失败中恢复时，将
     * 调用此方法。
     *
     * <p>在此完成后，新创建的共享状态被认为已发布，不再属于此句柄。这意味着它不再作为调用{@link #discardState()}的
     * 一部分被删除。相反，{@link #discardState()}将触发从注册表取消注册。
     *
     * Register both newly created and already referenced shared states in the given {@link
     * SharedStateRegistry}. This method is called when the checkpoint successfully completes or is
     * recovered from failures.
     *
     * <p>After this is completed, newly created shared state is considered as published is no
     * longer owned by this handle. This means that it should no longer be deleted as part of calls
     * to {@link #discardState()}. Instead, {@link #discardState()} will trigger an unregistration
     * from the registry.
     *
     * @param stateRegistry The registry where shared states are registered.
     */
    void registerSharedStates(SharedStateRegistry stateRegistry);
}
