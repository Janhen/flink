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

package org.apache.flink.api.common;

import org.apache.flink.annotation.PublicEvolving;

/** Possible states of a job once it has been accepted by the dispatcher. */
// 作业被调度程序接受后的可能状态
@PublicEvolving
public enum JobStatus {
    /**
     * 作业已被 Dispatcher 接收，正在等待作业管理器接收领导力并被创建
     *
     * The job has been received by the Dispatcher, and is waiting for the job manager to receive
     * leadership and to be created.
     */
    INITIALIZING(TerminalState.NON_TERMINAL),

    /** Job is newly created, no task has started to run. */
    // Job 是新创建的，没有任务开始运行
    CREATED(TerminalState.NON_TERMINAL),

    /** Some tasks are scheduled or running, some may be pending, some may be finished. */
    RUNNING(TerminalState.NON_TERMINAL),

    /** The job has failed and is currently waiting for the cleanup to complete. */
    // 作业已失败，目前正在等待清理工作完成
    FAILING(TerminalState.NON_TERMINAL),

    /** The job has failed with a non-recoverable task failure. */
    FAILED(TerminalState.GLOBALLY),

    /** Job is being cancelled. */
    CANCELLING(TerminalState.NON_TERMINAL),

    /** Job has been cancelled. */
    CANCELED(TerminalState.GLOBALLY),

    /** All of the job's tasks have successfully finished. */
    FINISHED(TerminalState.GLOBALLY),

    /** The job is currently undergoing a reset and total restart. */
    // 该作业目前正在进行复位和全面重启
    RESTARTING(TerminalState.NON_TERMINAL),

    /**
     * 作业已挂起，这意味着它已停止，但未从潜在的HA作业存储中删除
     *
     * The job has been suspended which means that it has been stopped but not been removed from a
     * potential HA job store.
     */
    SUSPENDED(TerminalState.LOCALLY),

    /** The job is currently reconciling and waits for task execution report to recover state. */
    // 作业当前正在协调，等待任务执行报告恢复状态
    RECONCILING(TerminalState.NON_TERMINAL);

    // --------------------------------------------------------------------------------------------

    private enum TerminalState {
        NON_TERMINAL,
        LOCALLY,
        GLOBALLY
    }

    private final TerminalState terminalState;

    JobStatus(TerminalState terminalState) {
        this.terminalState = terminalState;
    }

    /**
     * 检查此状态是否为<i>global terminal<i>。全局终端作业已完成，不能再失败，也不能由另一个备用主节点重新启动或恢复。
     *
     * <p>当达到全局终端状态时，作业的所有恢复数据将从高可用性服务中删除。
     *
     * Checks whether this state is <i>globally terminal</i>. A globally terminal job is complete
     * and cannot fail any more and will not be restarted or recovered by another standby master
     * node.
     *
     * <p>When a globally terminal state has been reached, all recovery data for the job is dropped
     * from the high-availability services.
     *
     * @return True, if this job status is globally terminal, false otherwise.
     */
    public boolean isGloballyTerminalState() {
        return terminalState == TerminalState.GLOBALLY;
    }

    /**
     * 检查此状态是否为<i>本地 terminal<i>。本地 terminal 是指正在执行的JobManager中的作业执行图的状态。如果
     * 执行图是本地 terminal 的， JobManager 将不会继续执行或恢复作业。
     *
     * <p>{@link #SUSPENDED}是本地 terminal 而不是全局 terminal 的唯一状态，通常在正在执行的 JobManager
     *   失去其leader状态时输入该状态。
     *
     * Checks whether this state is <i>locally terminal</i>. Locally terminal refers to the state of
     * a job's execution graph within an executing JobManager. If the execution graph is locally
     * terminal, the JobManager will not continue executing or recovering the job.
     *
     * <p>The only state that is locally terminal, but not globally terminal is {@link #SUSPENDED},
     * which is typically entered when the executing JobManager looses its leader status.
     *
     * @return True, if this job status is terminal, false otherwise.
     */
    public boolean isTerminalState() {
        return terminalState != TerminalState.NON_TERMINAL;
    }
}
