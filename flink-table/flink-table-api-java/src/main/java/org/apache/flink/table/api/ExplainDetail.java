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

package org.apache.flink.table.api;

import org.apache.flink.annotation.PublicEvolving;

/** ExplainDetail defines the types of details for explain result. */
// ExplainDetail 定义 explain 结果的详细信息类型。
@PublicEvolving
public enum ExplainDetail {
    /**
     * 优化器估计物理 rel 节点上的成本信息。例如 TableSourceScan(…，累积成本=
     * {1.0E8行，1.0E8 cpu, 2.4E9 io, 0.0网络，0.0内存}
     *
     * The cost information on physical rel node estimated by optimizer. e.g. TableSourceScan(...,
     * cumulative cost = {1.0E8 rows, 1.0E8 cpu, 2.4E9 io, 0.0 network, 0.0 memory}
     */
    ESTIMATED_COST,

    /**
     * 由物理 rel 节点产生的变更日志模式。例如 GroupAggregate(…changelogMode =[我UA D])
     *
     * The changelog mode produced by a physical rel node. e.g. GroupAggregate(...,
     * changelogMode=[I,UA,D])
     */
    CHANGELOG_MODE
}
