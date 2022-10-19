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

package org.apache.flink.configuration;

import static org.apache.flink.configuration.ConfigOptions.key;

/** Configuration parameters for join/sort algorithms. */
// 连接排序算法的配置参数
public class AlgorithmOptions {

    // 在混合哈希连接实现中激活布隆过滤器的标志
    //   在散列连接需要溢出到磁盘的情况下（数据集大于内存的保留部分），这些布隆过滤器可以大大减少溢出记录的数量，代价是一些 CPU 周期。
    public static final ConfigOption<Boolean> HASH_JOIN_BLOOM_FILTERS =
            key("taskmanager.runtime.hashjoin-bloom-filters")
                    .defaultValue(false)
                    .withDescription(
                            "Flag to activate/deactivate bloom filters in the hybrid hash join implementation."
                                    + " In cases where the hash join needs to spill to disk (datasets larger than the reserved fraction of"
                                    + " memory), these bloom filters can greatly reduce the number of spilled records, at the cost some"
                                    + " CPU cycles.");

    // 外部合并连接的最大扇入和溢出哈希表的扇出
    public static final ConfigOption<Integer> SPILLING_MAX_FAN =
            key("taskmanager.runtime.max-fan")
                    .defaultValue(128)
                    .withDescription(
                            "The maximal fan-in for external merge joins and fan-out for spilling hash tables. Limits"
                                    + " the number of file handles per operator, but may cause intermediate merging/partitioning, if set too"
                                    + " small.");

    // 当这部分内存预算已满时，排序操作开始溢出。
    public static final ConfigOption<Float> SORT_SPILLING_THRESHOLD =
            key("taskmanager.runtime.sort-spilling-threshold")
                    .defaultValue(0.8f)
                    .withDescription(
                            "A sort operation starts spilling when this fraction of its memory budget is full.");

    // 溢出时是否使用 LargeRecordHandler。
    //   如果一条记录不适合排序缓冲区。记录将溢出到磁盘上，仅使用键继续排序。合并时将随后读取记录本身。
    public static final ConfigOption<Boolean> USE_LARGE_RECORDS_HANDLER =
            key("taskmanager.runtime.large-record-handler")
                    .defaultValue(false)
                    .withDescription(
                            "Whether to use the LargeRecordHandler when spilling. If a record will not fit into the sorting"
                                    + " buffer. The record will be spilled on disk and the sorting will continue with only the key."
                                    + " The record itself will be read afterwards when merging.");
}
