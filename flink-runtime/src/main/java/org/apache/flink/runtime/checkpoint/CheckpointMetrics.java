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

package org.apache.flink.runtime.checkpoint;

import java.io.Serializable;
import java.util.Objects;

import static org.apache.flink.util.Preconditions.checkArgument;

/** A collection of simple metrics, around the triggering of a checkpoint. */
/** 围绕检查点触发的一组简单 metrics。 */
public class CheckpointMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The duration (in nanoseconds) that the stream alignment for the checkpoint took. */
    /** 流对齐检查点所花费的时间(以纳秒为单位)。 */
    private long alignmentDurationNanos;

    /** The duration (in milliseconds) of the synchronous part of the operator checkpoint. */
    // operator 检查点同步部分的持续时间(以毫秒为单位)。
    private long syncDurationMillis;

    /** The duration (in milliseconds) of the asynchronous part of the operator checkpoint. */
    // operator 检查点异步部分的持续时间(以毫秒为单位)。
    private long asyncDurationMillis;

    // 检查点开始延迟
    private long checkpointStartDelayNanos;

    public CheckpointMetrics() {
        this(-1L, -1L, -1L, -1L);
    }

    public CheckpointMetrics(
            long bytesBufferedInAlignment,
            long alignmentDurationNanos,
            long syncDurationMillis,
            long asyncDurationMillis) {

        // these may be "-1", in case the values are unknown or not set
        checkArgument(syncDurationMillis >= -1);
        checkArgument(asyncDurationMillis >= -1);
        checkArgument(bytesBufferedInAlignment >= -1);
        checkArgument(alignmentDurationNanos >= -1);

        this.alignmentDurationNanos = alignmentDurationNanos;
        this.syncDurationMillis = syncDurationMillis;
        this.asyncDurationMillis = asyncDurationMillis;
    }

    public long getAlignmentDurationNanos() {
        return alignmentDurationNanos;
    }

    public CheckpointMetrics setAlignmentDurationNanos(long alignmentDurationNanos) {
        this.alignmentDurationNanos = alignmentDurationNanos;
        return this;
    }

    public long getSyncDurationMillis() {
        return syncDurationMillis;
    }

    public CheckpointMetrics setSyncDurationMillis(long syncDurationMillis) {
        this.syncDurationMillis = syncDurationMillis;
        return this;
    }

    public long getAsyncDurationMillis() {
        return asyncDurationMillis;
    }

    public CheckpointMetrics setAsyncDurationMillis(long asyncDurationMillis) {
        this.asyncDurationMillis = asyncDurationMillis;
        return this;
    }

    public CheckpointMetrics setCheckpointStartDelayNanos(long checkpointStartDelayNanos) {
        this.checkpointStartDelayNanos = checkpointStartDelayNanos;
        return this;
    }

    public long getCheckpointStartDelayNanos() {
        return checkpointStartDelayNanos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CheckpointMetrics that = (CheckpointMetrics) o;

        return alignmentDurationNanos == that.alignmentDurationNanos
                && syncDurationMillis == that.syncDurationMillis
                && asyncDurationMillis == that.asyncDurationMillis
                && checkpointStartDelayNanos == that.checkpointStartDelayNanos;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                alignmentDurationNanos,
                syncDurationMillis,
                asyncDurationMillis,
                checkpointStartDelayNanos);
    }

    @Override
    public String toString() {
        return "CheckpointMetrics{"
                + ", alignmentDurationNanos="
                + alignmentDurationNanos
                + ", syncDurationMillis="
                + syncDurationMillis
                + ", asyncDurationMillis="
                + asyncDurationMillis
                + ", checkpointStartDelayNanos="
                + checkpointStartDelayNanos
                + '}';
    }
}
