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
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.runtime.state.metainfo.StateMetaInfoSnapshot;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import java.io.IOException;

/**
 * 应该按键组进行写分区的状态快照的通用接口。所有的快照都应该在使用后释放。该接口概述了异步快照的生命周期，通常如下所示。
 * 在检查点的同步部分中，为某个状态生成一个{@link StateSnapshot}实例，并捕获此时的状态。然后，在检查点的异步部分，用
 * 户调用{@link #getKeyGroupWriter()}以确保快照被划分为键组。对于已经分区的状态，这可以是NOP。调用者可以使用返回的
 * {@link StateKeyGroupWriter}按键组写入状态。作为最后一步，当状态完全写入时，用户调用{@link #release()}。
 *
 * General interface for state snapshots that should be written partitioned by key-groups. All
 * snapshots should be released after usage. This interface outlines the asynchronous snapshot
 * life-cycle, which typically looks as follows. In the synchronous part of a checkpoint, an
 * instance of {@link StateSnapshot} is produced for a state and captures the state at this point in
 * time. Then, in the asynchronous part of the checkpoint, the user calls {@link
 * #getKeyGroupWriter()} to ensure that the snapshot is partitioned into key-groups. For state that
 * is already partitioned, this can be a NOP. The returned {@link StateKeyGroupWriter} can be used
 * by the caller to write the state by key-group. As a last step, when the state is completely
 * written, the user calls {@link #release()}.
 */
@Internal
public interface StateSnapshot {

    /**
     * This method returns {@link StateKeyGroupWriter} and should be called in the asynchronous part
     * of the snapshot.
     */
    @Nonnull
    StateKeyGroupWriter getKeyGroupWriter();

    /** Returns a snapshot of the state's meta data. */
    @Nonnull
    StateMetaInfoSnapshot getMetaInfoSnapshot();

    /**
     * Release the snapshot. All snapshots should be released when they are no longer used because
     * some implementation can only release resources after a release. Produced {@link
     * StateKeyGroupWriter} should no longer be used after calling this method.
     */
    void release();

    /** Interface for writing a snapshot that is partitioned into key-groups. */
    interface StateKeyGroupWriter {
        /**
         * Writes the data for the specified key-group to the output. You must call {@link
         * #getKeyGroupWriter()} once before first calling this method.
         *
         * @param dov the output.
         * @param keyGroupId the key-group to write.
         * @throws IOException on write-related problems.
         */
        void writeStateInKeyGroup(@Nonnull DataOutputView dov, @Nonnegative int keyGroupId)
                throws IOException;
    }
}
