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

package org.apache.flink.runtime.jobgraph;

/** Enum to distinguish JobGraphs between batch and streaming, currently used by the scheduler. */
// 枚举，以区分批处理和流处理之间的JobGraphs，目前由调度器使用
public enum JobType {
    /** Batch jobs are finite jobs, potentially consisting of multiple pipelined regions. */
    BATCH,
    /**
     * 流作业是无限的作业，由一个大的流水线区域组成，不被任何阻塞的数据交换隔开
     *
     * Streaming jobs are infinite jobs, consisting of one large pipelined region, not separated by
     * any blocking data exchanges.
     */
    STREAMING
}
