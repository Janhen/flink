/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.api.common;

import org.apache.flink.annotation.PublicEvolving;

/**
 * DataStream程序的运行时执行模式。除此之外，它还控制任务调度、网络转移行为和时间语义。一些操作还将根据配置的执行模式
 * 更改它们的记录发射行为。
 *
 * Runtime execution mode of DataStream programs. Among other things, this controls task scheduling,
 * network shuffle behavior, and time semantics. Some operations will also change their record
 * emission behaviour based on the configured execution mode.
 *
 * @see <a
 *     href="https://cwiki.apache.org/confluence/display/FLINK/FLIP-134%3A+Batch+execution+for+the+DataStream+API">
 *     https://cwiki.apache.org/confluence/display/FLINK/FLIP-134%3A+Batch+execution+for+the+DataStream+API</a>
 */
@PublicEvolving
public enum RuntimeExecutionMode {

    /**
     * 流水线将使用流语义执行。所有任务将在执行开始之前部署，检查点将被启用，处理时间和事件时间都将得到完全支持。
     *
     * The Pipeline will be executed with Streaming Semantics. All tasks will be deployed before
     * execution starts, checkpoints will be enabled, and both processing and event time will be
     * fully supported.
     */
    STREAMING,

    /**
     * 流水线将使用批处理语义执行。任务会根据所属的调度区域逐步调度，区域之间的切换会阻塞，水印假设是“完美的”，即没有
     * 延迟的数据，执行过程中假设处理时间不提前。
     *
     * The Pipeline will be executed with Batch Semantics. Tasks will be scheduled gradually based
     * on the scheduling region they belong, shuffles between regions will be blocking, watermarks
     * are assumed to be "perfect" i.e. no late data, and processing time is assumed to not advance
     * during execution.
     */
    BATCH,

    /**
     * 如果所有的源都有边界，Flink将设置执行模式为{@link RuntimeExecutionMode#BATCH};如果至少有一个源没有边界，
     * Flink将设置为{@link RuntimeExecutionMode#STREAMING}。
     *
     * Flink will set the execution mode to {@link RuntimeExecutionMode#BATCH} if all sources are
     * bounded, or {@link RuntimeExecutionMode#STREAMING} if there is at least one source which is
     * unbounded.
     */
    AUTOMATIC
}
