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

package org.apache.flink.runtime.scheduler.strategy;

/** State of a {@link SchedulingResultPartition}. */
public enum ResultPartitionState {

    /** Partition is just created or is just reset. */
    // 分区刚刚创建或刚刚重置
    CREATED,

    /**
     * 分区可以使用了。对于流水线分区，这表示它产生了数据。对于阻塞分区，这表示父结果中的所有结果分区已经完成。
     *
     * Partition is ready for consuming. For pipelined partition, this indicates it has data
     * produced. For blocking partition, this indicates all result partitions in its parent result
     * have finished.
     */
    CONSUMABLE
}
