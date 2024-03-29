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

import javax.annotation.Nullable;

import java.io.Serializable;

import static org.apache.flink.util.Preconditions.checkNotNull;

/** A snapshot of the checkpoint stats. */
public class CheckpointStatsSnapshot implements Serializable {

    private static final long serialVersionUID = 8914278419087217964L;

    /** Snapshot of the checkpoint counts. */
    // 检查点计数的快照。
    private final CheckpointStatsCounts counts;

    /** Snapshot of the completed checkpoints summary stats. */
    // 完成检查点汇总统计的快照。
    private final CompletedCheckpointStatsSummary summary;

    /** Snapshot of the checkpoint history. */
    // 检查点历史的快照。
    private final CheckpointStatsHistory history;

    /** The latest restored checkpoint operation. */
    // 最近恢复的检查点操作。
    @Nullable private final RestoredCheckpointStats latestRestoredCheckpoint;

    public static CheckpointStatsSnapshot empty() {
        return new CheckpointStatsSnapshot(
                new CheckpointStatsCounts(),
                new CompletedCheckpointStatsSummary(),
                new CheckpointStatsHistory(0),
                null);
    }

    /**
     * Creates a stats snapshot.
     *
     * @param counts Snapshot of the checkpoint counts.
     * @param summary Snapshot of the completed checkpoints summary stats.
     * @param history Snapshot of the checkpoint history.
     * @param latestRestoredCheckpoint The latest restored checkpoint operation.
     */
    CheckpointStatsSnapshot(
            CheckpointStatsCounts counts,
            CompletedCheckpointStatsSummary summary,
            CheckpointStatsHistory history,
            @Nullable RestoredCheckpointStats latestRestoredCheckpoint) {

        this.counts = checkNotNull(counts);
        this.summary = checkNotNull(summary);
        this.history = checkNotNull(history);
        this.latestRestoredCheckpoint = latestRestoredCheckpoint;
    }

    /**
     * Returns the snapshotted checkpoint counts.
     *
     * @return Snapshotted checkpoint counts.
     */
    public CheckpointStatsCounts getCounts() {
        return counts;
    }

    /**
     * Returns the snapshotted completed checkpoint summary stats.
     *
     * @return Snapshotted completed checkpoint summary stats.
     */
    public CompletedCheckpointStatsSummary getSummaryStats() {
        return summary;
    }

    /**
     * Returns the snapshotted checkpoint history.
     *
     * @return Snapshotted checkpoint history.
     */
    public CheckpointStatsHistory getHistory() {
        return history;
    }

    /**
     * Returns the latest restored checkpoint.
     *
     * @return Latest restored checkpoint or <code>null</code>.
     */
    @Nullable
    public RestoredCheckpointStats getLatestRestoredCheckpoint() {
        return latestRestoredCheckpoint;
    }
}
