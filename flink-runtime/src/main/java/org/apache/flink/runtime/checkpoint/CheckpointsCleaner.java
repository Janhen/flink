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

package org.apache.flink.runtime.checkpoint;

import org.apache.flink.util.function.RunnableWithException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;

import java.io.Serializable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 委托类负责检查点清理和计数尚未清理的检查点数量。
 *
 * Delegate class responsible for checkpoints cleaning and counting the number of checkpoints yet to
 * clean.
 */
@ThreadSafe
public class CheckpointsCleaner implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(CheckpointsCleaner.class);
    private static final long serialVersionUID = 2545865801947537790L;

    private final AtomicInteger numberOfCheckpointsToClean;

    public CheckpointsCleaner() {
        this.numberOfCheckpointsToClean = new AtomicInteger(0);
    }

    int getNumberOfCheckpointsToClean() {
        return numberOfCheckpointsToClean.get();
    }

    public void cleanCheckpoint(
            Checkpoint checkpoint,
            boolean shouldDiscard,
            Runnable postCleanAction,
            Executor executor) {
        cleanup(
                checkpoint,
                () -> {
                    if (shouldDiscard) {
                        checkpoint.discard();
                    }
                },
                postCleanAction,
                executor);
    }

    public void cleanCheckpointOnFailedStoring(
            CompletedCheckpoint completedCheckpoint, Executor executor) {
        cleanup(
                completedCheckpoint,
                completedCheckpoint::discardOnFailedStoring,
                () -> {},
                executor);
    }

    private void cleanup(
            Checkpoint checkpoint,
            RunnableWithException cleanupAction,
            Runnable postCleanupAction,
            Executor executor) {
        numberOfCheckpointsToClean.incrementAndGet();
        executor.execute(
                () -> {
                    try {
                        cleanupAction.run();
                    } catch (Exception e) {
                        LOG.warn(
                                "Could not properly discard completed checkpoint {}.",
                                checkpoint.getCheckpointID(),
                                e);
                    } finally {
                        numberOfCheckpointsToClean.decrementAndGet();
                        postCleanupAction.run();
                    }
                });
    }
}
