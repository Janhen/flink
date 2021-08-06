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
package org.apache.flink.runtime.executiongraph;

import org.apache.flink.runtime.execution.ExecutionState;
import org.apache.flink.runtime.taskmanager.TaskManagerLocation;

import javax.annotation.Nullable;

/** Common interface for the runtime {@link ExecutionVertex} and {@link ArchivedExecutionVertex}. */
public interface AccessExecutionVertex {
    /**
     * 返回这个执行顶点的名称，格式为 “myTask(2/7)”。
     *
     * Returns the name of this execution vertex in the format "myTask (2/7)".
     *
     * @return name of this execution vertex
     */
    String getTaskNameWithSubtaskIndex();

    /**
     * Returns the subtask index of this execution vertex.
     *
     * @return subtask index of this execution vertex.
     */
    int getParallelSubtaskIndex();

    /**
     * Returns the current execution for this execution vertex.
     *
     * @return current execution
     */
    AccessExecution getCurrentExecutionAttempt();

    /**
     * Returns the current {@link ExecutionState} for this execution vertex.
     *
     * @return execution state for this execution vertex
     */
    ExecutionState getExecutionState();

    /**
     * 返回给定的 {@link ExecutionState} 的时间戳。
     *
     * Returns the timestamp for the given {@link ExecutionState}.
     *
     * @param state state for which the timestamp should be returned
     * @return timestamp for the given state
     */
    long getStateTimestamp(ExecutionState state);

    /**
     * 返回导致作业失败的异常。这是第一个不可恢复并触发作业失败的根异常。
     *
     * Returns the exception that caused the job to fail. This is the first root exception that was
     * not recoverable and triggered job failure.
     *
     * @return failure exception as a string, or {@code "(null)"}
     */
    String getFailureCauseAsString();

    /**
     * 返回这个执行顶点的 {@link TaskManagerLocation}。
     *
     * Returns the {@link TaskManagerLocation} for this execution vertex.
     *
     * @return taskmanager location for this execution vertex.
     */
    TaskManagerLocation getCurrentAssignedResourceLocation();

    /**
     * 返回给定尝试数的执行。
     *
     * Returns the execution for the given attempt number.
     *
     * @param attemptNumber attempt number of execution to be returned
     * @return execution for the given attempt number
     */
    @Nullable
    AccessExecution getPriorExecutionAttempt(int attemptNumber);
}
