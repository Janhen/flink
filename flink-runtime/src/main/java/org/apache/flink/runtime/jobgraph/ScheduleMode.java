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

/** The ScheduleMode decides how tasks of an execution graph are started. */
// ScheduleMode决定如何启动执行图中的任务
public enum ScheduleMode {
    /**
     * 从源延迟地计划任务。一旦输入数据准备好，下游任务就会启动
     *
     * Schedule tasks lazily from the sources. Downstream tasks are started once their input data
     * are ready
     */
    LAZY_FROM_SOURCES(true),

    /**
     * 与LAZY_FROM_SOURCES相同，不同之处在于它使用批处理槽请求，支持执行槽数少于请求的作业。但是，
     * 用户需要确保作业不包含任何流水线洗牌(每个流水线区域都可以使用单个槽执行)。
     *
     * Same as LAZY_FROM_SOURCES just with the difference that it uses batch slot requests which
     * support the execution of jobs with fewer slots than requested. However, the user needs to
     * make sure that the job does not contain any pipelined shuffles (every pipelined region can be
     * executed with a single slot).
     */
    LAZY_FROM_SOURCES_WITH_BATCH_SLOT_REQUEST(true),

    /** Schedules all tasks immediately. */
    // 立即安排所有任务
    EAGER(false);

    private final boolean allowLazyDeployment;

    ScheduleMode(boolean allowLazyDeployment) {
        this.allowLazyDeployment = allowLazyDeployment;
    }

    /** Returns whether we are allowed to deploy consumers lazily. */
    public boolean allowLazyDeployment() {
        return allowLazyDeployment;
    }
}
