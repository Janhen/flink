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

package org.apache.flink.runtime.io.network.partition;

/** Type of a result partition. */
// 结果分区类型
public enum ResultPartitionType {

    /**
     * 阻塞分区代表阻塞数据交换，数据流首先完全产生，然后被消费。这是一个只适用于有界流的选项，可以在有界流运行时和
     * 恢复中使用。
     *
     * <p>阻塞分区可以被多次并发使用。
     *
     * <p>分区在被使用后不会自动释放(比如{@link #PIPELINED}分区)，只有当它确定不再需要该分区时，才会通过调度器释放。
     *
     * Blocking partitions represent blocking data exchanges, where the data stream is first fully
     * produced and then consumed. This is an option that is only applicable to bounded streams and
     * can be used in bounded stream runtime and recovery.
     *
     * <p>Blocking partitions can be consumed multiple times and concurrently.
     *
     * <p>The partition is not automatically released after being consumed (like for example the
     * {@link #PIPELINED} partitions), but only released through the scheduler, when it determines
     * that the partition is no longer needed.
     */
    BLOCKING(false, false, false, false, true),

    /**
     * BLOCKING_PERSISTENT partitions are similar to {@link #BLOCKING} partitions, but have a
     * user-specified life cycle.
     *
     * <p>BLOCKING_PERSISTENT partitions are dropped upon explicit API calls to the JobManager or
     * ResourceManager, rather than by the scheduler.
     *
     * <p>Otherwise, the partition may only be dropped by safety-nets during failure handling
     * scenarios, like when the TaskManager exits or when the TaskManager looses connection to
     * JobManager / ResourceManager for too long.
     */
    BLOCKING_PERSISTENT(false, false, false, true, true),

    /**
     * 一种流水线的流数据交换。这既适用于有界流，也适用于无界流。
     *
     * A pipelined streaming data exchange. This is applicable to both bounded and unbounded
     * streams.
     *
     * <p>Pipelined results can be consumed only once by a single consumer and are automatically
     * disposed when the stream has been consumed.
     *
     * <p>This result partition type may keep an arbitrary amount of data in-flight, in contrast to
     * the {@link #PIPELINED_BOUNDED} variant.
     */
    PIPELINED(true, true, false, false, false),

    /**
     * 带有受限(本地)缓冲池的管道分区。
     *
     * Pipelined partitions with a bounded (local) buffer pool.
     *
     * <p>For streaming jobs, a fixed limit on the buffer pool size should help avoid that too much
     * data is being buffered and checkpoint barriers are delayed. In contrast to limiting the
     * overall network buffer pool size, this, however, still allows to be flexible with regards to
     * the total number of partitions by selecting an appropriately big network buffer pool size.
     *
     * <p>For batch jobs, it will be best to keep this unlimited ({@link #PIPELINED}) since there
     * are no checkpoint barriers.
     */
    PIPELINED_BOUNDED(true, true, true, false, false),

    /**
     * 带有有界(本地)缓冲池的流水线分区，以支持下游任务在近似本地恢复中重新连接后继续消耗数据。
     *
     * Pipelined partitions with a bounded (local) buffer pool to support downstream task to
     * continue consuming data after reconnection in Approximate Local-Recovery.
     *
     * <p>Pipelined results can be consumed only once by a single consumer at one time. {@link
     * #PIPELINED_APPROXIMATE} is different from {@link #PIPELINED} and {@link #PIPELINED_BOUNDED}
     * in that {@link #PIPELINED_APPROXIMATE} partition can be reconnected after down stream task
     * fails.
     */
    PIPELINED_APPROXIMATE(true, true, true, false, true);

    /** Can the partition be consumed while being produced? */
    private final boolean isPipelined;

    /** Does the partition produce back pressure when not consumed? */
    // 分区不使用时是否产生背压?
    private final boolean hasBackPressure;

    /** Does this partition use a limited number of (network) buffers? */
    // 这个分区使用有限数量的(网络)缓冲区吗?
    private final boolean isBounded;

    /** This partition will not be released after consuming if 'isPersistent' is true. */
    // 如果'isPersistent'为真，这个分区在使用后不会被释放。
    private final boolean isPersistent;

    /**
     * 分区可以重新连接吗?
     *
     * Can the partition be reconnected.
     *
     * <p>Attention: this attribute is introduced temporally for
     * ResultPartitionType.PIPELINED_APPROXIMATE It will be removed afterwards: TODO: 1. Approximate
     * local recovery has its won failover strategy to restart the failed set of tasks instead of
     * restarting downstream of failed tasks depending on {@code
     * RestartPipelinedRegionFailoverStrategy} 2. FLINK-19895: Unify the life cycle of
     * ResultPartitionType Pipelined Family
     */
    private final boolean isReconnectable;

    /** Specifies the behaviour of an intermediate result partition at runtime. */
    ResultPartitionType(
            boolean isPipelined,
            boolean hasBackPressure,
            boolean isBounded,
            boolean isPersistent,
            boolean isReconnectable) {
        this.isPipelined = isPipelined;
        this.hasBackPressure = hasBackPressure;
        this.isBounded = isBounded;
        this.isPersistent = isPersistent;
        this.isReconnectable = isReconnectable;
    }

    public boolean hasBackPressure() {
        return hasBackPressure;
    }

    public boolean isBlocking() {
        return !isPipelined;
    }

    public boolean isPipelined() {
        return isPipelined;
    }

    public boolean isReconnectable() {
        return isReconnectable;
    }

    /**
     * Whether this partition uses a limited number of (network) buffers or not.
     *
     * @return <tt>true</tt> if the number of buffers should be bound to some limit
     */
    public boolean isBounded() {
        return isBounded;
    }

    public boolean isPersistent() {
        return isPersistent;
    }
}
