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

package org.apache.flink.runtime.execution;

/**
 * 任务在执行期间可能处于的所有状态的枚举。任务通常在状态{@code CREATED}中启动，并根据下图切换状态:
 *
 * <p>如果作业管理器故障转移，可以从{@code CREATED}状态进入{@code RECONCILING}状态，并且{@code RECONCILING}
 * 状态可以切换到任何现有的任务状态。
 *
 * An enumeration of all states that a task can be in during its execution. Tasks usually start in
 * the state {@code CREATED} and switch states according to this diagram:
 *
 * <pre>{@code
 *  CREATED  -> SCHEDULED -> DEPLOYING -> RUNNING -> FINISHED
 *     |            |            |          |
 *     |            |            |   +------+
 *     |            |            V   V
 *     |            |         CANCELLING -----+----> CANCELED
 *     |            |                         |
 *     |            +-------------------------+
 *     |
 *     |                                   ... -> FAILED
 *     V
 * RECONCILING  -> RUNNING | FINISHED | CANCELED | FAILED
 *
 * }</pre>
 *
 * <p>It is possible to enter the {@code RECONCILING} state from {@code CREATED} state if job
 * manager fail over, and the {@code RECONCILING} state can switch into any existing task state.
 *
 * <p>It is possible to enter the {@code FAILED} state from any other state.
 *
 * <p>The states {@code FINISHED}, {@code CANCELED}, and {@code FAILED} are considered terminal
 * states.
 */
public enum ExecutionState {
    CREATED,

    SCHEDULED,

    DEPLOYING,

    RUNNING,

    /**
     * 这个状态标志着“成功完成”。只有当程序到达“输入的末端”时才能到达。当使用有界输入(文件的修复集、有界查询等)或停止
     * 程序(不是取消!)时，可以达到“输入结束”，这使输入看起来像是在特定的点结束了。
     *
     * This state marks "successfully completed". It can only be reached when a program reaches the
     * "end of its input". The "end of input" can be reached when consuming a bounded input (fix set
     * of files, bounded query, etc) or when stopping a program (not cancelling!) which make the
     * input look like it reached its end at a specific point.
     */
    FINISHED,

    CANCELING,

    CANCELED,

    FAILED,

    RECONCILING;

    public boolean isTerminal() {
        return this == FINISHED || this == CANCELED || this == FAILED;
    }
}
