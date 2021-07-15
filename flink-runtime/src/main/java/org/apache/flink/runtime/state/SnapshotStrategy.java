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

import org.apache.flink.annotation.Internal;
import org.apache.flink.runtime.checkpoint.CheckpointOptions;

import javax.annotation.Nonnull;

import java.util.concurrent.RunnableFuture;

/**
 * 用于状态后端中不同快照方法的接口。理想情况下，实现类应该是无状态的，或者至少是线程安全的，也就是说，这是一个功能性接口，
 * 可以被多个检查点并行调用
 *
 * Interface for different snapshot approaches in state backends. Implementing classes should
 * ideally be stateless or at least threadsafe, i.e. this is a functional interface and is can be
 * called in parallel by multiple checkpoints.
 *
 * @param <S> type of the returned state object that represents the result of the snapshot
 *     operation.
 */
@Internal
public interface SnapshotStrategy<S extends StateObject> {

    /**
     * 该操作将快照写入由给定{@link CheckpointStreamFactory}提供的流中，并返回一个@{@link RunnableFuture}，
     * 该操作提供了快照的状态句柄。操作是同步执行还是异步执行取决于实现。在后一种情况下，在获取句柄之前必须先执行返回的
     * Runnable。
     *
     * Operation that writes a snapshot into a stream that is provided by the given {@link
     * CheckpointStreamFactory} and returns a @{@link RunnableFuture} that gives a state handle to
     * the snapshot. It is up to the implementation if the operation is performed synchronous or
     * asynchronous. In the later case, the returned Runnable must be executed first before
     * obtaining the handle.
     *
     * @param checkpointId The ID of the checkpoint.
     * @param timestamp The timestamp of the checkpoint.
     * @param streamFactory The factory that we can use for writing our state to streams.
     * @param checkpointOptions Options for how to perform this checkpoint.
     * @return A runnable future that will yield a {@link StateObject}.
     */
    @Nonnull
    RunnableFuture<S> snapshot(
            long checkpointId,
            long timestamp,
            @Nonnull CheckpointStreamFactory streamFactory,
            @Nonnull CheckpointOptions checkpointOptions)
            throws Exception;
}
