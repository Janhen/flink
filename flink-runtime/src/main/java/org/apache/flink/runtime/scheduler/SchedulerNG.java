/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.flink.runtime.scheduler;

import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.queryablestate.KvStateID;
import org.apache.flink.runtime.accumulators.AccumulatorSnapshot;
import org.apache.flink.runtime.checkpoint.CheckpointMetrics;
import org.apache.flink.runtime.checkpoint.TaskStateSnapshot;
import org.apache.flink.runtime.concurrent.ComponentMainThreadExecutor;
import org.apache.flink.runtime.execution.ExecutionState;
import org.apache.flink.runtime.executiongraph.ArchivedExecutionGraph;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.runtime.executiongraph.JobStatusListener;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.jobmanager.PartitionProducerDisposedException;
import org.apache.flink.runtime.jobmaster.SerializedInputSplit;
import org.apache.flink.runtime.messages.FlinkJobNotFoundException;
import org.apache.flink.runtime.messages.checkpoint.DeclineCheckpoint;
import org.apache.flink.runtime.messages.webmonitor.JobDetails;
import org.apache.flink.runtime.operators.coordination.CoordinationRequest;
import org.apache.flink.runtime.operators.coordination.CoordinationResponse;
import org.apache.flink.runtime.operators.coordination.OperatorCoordinator;
import org.apache.flink.runtime.operators.coordination.OperatorEvent;
import org.apache.flink.runtime.query.KvStateLocation;
import org.apache.flink.runtime.query.UnknownKvStateLocation;
import org.apache.flink.runtime.rest.handler.legacy.backpressure.OperatorBackPressureStats;
import org.apache.flink.runtime.state.KeyGroupRange;
import org.apache.flink.runtime.taskmanager.TaskExecutionState;
import org.apache.flink.util.FlinkException;

import javax.annotation.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 调度 Flink 作业的接口。
 *
 * <p> 实例通过 {@link SchedulerNGFactory} 创建，并在实例化时接收一个 {@link JobGraph}。
 *
 * <p> 实现可以期望方法不会被并发调用。事实上，所有的调用都将起源于 {@link ComponentMainThreadExecutor} 中的一个线程，
 * 该线程将通过 {@link #setMainThreadExecutor(ComponentMainThreadExecutor)} 传递。
 *
 * Interface for scheduling Flink jobs.
 *
 * <p>Instances are created via {@link SchedulerNGFactory}, and receive a {@link JobGraph} when
 * instantiated.
 *
 * <p>Implementations can expect that methods will not be invoked concurrently. In fact, all
 * invocations will originate from a thread in the {@link ComponentMainThreadExecutor}, which will
 * be passed via {@link #setMainThreadExecutor(ComponentMainThreadExecutor)}.
 */
public interface SchedulerNG {

    // J: 主线程执行器
    void setMainThreadExecutor(ComponentMainThreadExecutor mainThreadExecutor);

    // J: 注册 Job 状态监听器
    void registerJobStatusListener(JobStatusListener jobStatusListener);

    void startScheduling();

    void suspend(Throwable cause);

    void cancel();

    CompletableFuture<Void> getTerminationFuture();

    // J: 处理调度中的全局失败
    void handleGlobalFailure(Throwable cause);

    // J: 更新 Task 级别的状态
    boolean updateTaskExecutionState(TaskExecutionState taskExecutionState);

    // J: batch 处理时候的 Split?
    SerializedInputSplit requestNextInputSplit(
            JobVertexID vertexID, ExecutionAttemptID executionAttempt) throws IOException;

    ExecutionState requestPartitionState(
            IntermediateDataSetID intermediateResultId, ResultPartitionID resultPartitionId)
            throws PartitionProducerDisposedException;

    void scheduleOrUpdateConsumers(ResultPartitionID partitionID);

    ArchivedExecutionGraph requestJob();

    // J: 获取 Job 的状态
    JobStatus requestJobStatus();

    // J: 获取 Job 的详情
    JobDetails requestJobDetails();

    // ------------------------------------------------------------------------------------
    // Methods below do not belong to Scheduler but are included due to historical reasons
    // ------------------------------------------------------------------------------------

    // J: 获取 Kv 状态未知
    KvStateLocation requestKvStateLocation(JobID jobId, String registrationName)
            throws UnknownKvStateLocation, FlinkJobNotFoundException;

    void notifyKvStateRegistered(
            JobID jobId,
            JobVertexID jobVertexId,
            KeyGroupRange keyGroupRange,
            String registrationName,
            KvStateID kvStateId,
            InetSocketAddress kvStateServerAddress)
            throws FlinkJobNotFoundException;

    void notifyKvStateUnregistered(
            JobID jobId,
            JobVertexID jobVertexId,
            KeyGroupRange keyGroupRange,
            String registrationName)
            throws FlinkJobNotFoundException;

    // ------------------------------------------------------------------------

    // J: 调度中累加器更新
    void updateAccumulators(AccumulatorSnapshot accumulatorSnapshot);

    // ------------------------------------------------------------------------

    // J: 获取算子的背压统计信息
    Optional<OperatorBackPressureStats> requestOperatorBackPressureStats(JobVertexID jobVertexId)
            throws FlinkException;

    // ------------------------------------------------------------------------

    // J: 触发保存点
    CompletableFuture<String> triggerSavepoint(@Nullable String targetDirectory, boolean cancelJob);

    void acknowledgeCheckpoint(
            JobID jobID,
            ExecutionAttemptID executionAttemptID,
            long checkpointId,
            CheckpointMetrics checkpointMetrics,
            TaskStateSnapshot checkpointState);

    // J: 拒绝检查点
    void declineCheckpoint(DeclineCheckpoint decline);

    CompletableFuture<String> stopWithSavepoint(
            String targetDirectory, boolean advanceToEndOfEventTime);

    // ------------------------------------------------------------------------
    //  Operator Coordinator related methods
    //
    //  These are necessary as long as the Operator Coordinators are part of the
    //  scheduler. There are good reasons to pull them out of the Scheduler and
    //  make them directly a part of the JobMaster. However, we would need to
    //  rework the complete CheckpointCoordinator initialization before we can
    //  do that, because the CheckpointCoordinator is initialized (and restores
    //  savepoint) in the scheduler constructor, which requires the coordinators
    //  to be there as well.
    // ------------------------------------------------------------------------

    /**
     * 将给定的 OperatorEvent 传递给具有给定 {@link OperatorID} 的 {@link OperatorCoordinator}。
     *
     * Delivers the given OperatorEvent to the {@link OperatorCoordinator} with the given {@link
     * OperatorID}.
     *
     * <p>Failure semantics: If the task manager sends an event for a non-running task or a
     * non-existing operator coordinator, then respond with an exception to the call. If task and
     * coordinator exist, then we assume that the call from the TaskManager was valid, and any
     * bubbling exception needs to cause a job failure
     *
     * @throws FlinkException Thrown, if the task is not running or no operator/coordinator exists
     *     for the given ID.
     */
    void deliverOperatorEventToCoordinator(
            ExecutionAttemptID taskExecution, OperatorID operator, OperatorEvent evt)
            throws FlinkException;

    /**
     * Delivers a coordination request to the {@link OperatorCoordinator} with the given {@link
     * OperatorID} and returns the coordinator's response.
     *
     * @return A future containing the response.
     * @throws FlinkException Thrown, if the task is not running, or no operator/coordinator exists
     *     for the given ID, or the coordinator cannot handle client events.
     */
    CompletableFuture<CoordinationResponse> deliverCoordinationRequestToCoordinator(
            OperatorID operator, CoordinationRequest request) throws FlinkException;
}
