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

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.api.common.accumulators.Accumulator;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.JobException;
import org.apache.flink.runtime.accumulators.AccumulatorSnapshot;
import org.apache.flink.runtime.checkpoint.CheckpointCoordinator;
import org.apache.flink.runtime.checkpoint.CheckpointIDCounter;
import org.apache.flink.runtime.checkpoint.CheckpointStatsTracker;
import org.apache.flink.runtime.checkpoint.CheckpointsCleaner;
import org.apache.flink.runtime.checkpoint.CompletedCheckpointStore;
import org.apache.flink.runtime.checkpoint.MasterTriggerRestoreHook;
import org.apache.flink.runtime.concurrent.ComponentMainThreadExecutor;
import org.apache.flink.runtime.executiongraph.failover.flip1.ResultPartitionAvailabilityChecker;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;
import org.apache.flink.runtime.jobgraph.JobVertex;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.jobgraph.tasks.CheckpointCoordinatorConfiguration;
import org.apache.flink.runtime.query.KvStateLocationRegistry;
import org.apache.flink.runtime.scheduler.InternalFailuresListener;
import org.apache.flink.runtime.scheduler.strategy.SchedulingTopology;
import org.apache.flink.runtime.state.CheckpointStorage;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.util.OptionalFailure;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 执行图是协调数据流的分布式执行的中心数据结构。保存每个并行任务、每个中间流的表示，以及它们之间的通信。
 *
 * 执行图由以下结构组成:
 *
 * The execution graph is the central data structure that coordinates the distributed execution of a
 * data flow. It keeps representations of each parallel task, each intermediate stream, and the
 * communication between them.
 *
 * <p>The execution graph consists of the following constructs:
 *
 *   <li>{@link ExecutionJobVertex}表示执行期间JobGraph中的一个顶点(通常是“map”或“join”这样的操作)。它保存所有
 *     并行子任务的聚合状态。ExecutionJobVertex在图中由{@link JobVertexID}标识，它从JobGraph对应的JobVertex中获取。
 *
 *   <li>{@link ExecutionVertex}表示一个并行子任务。对于每个ExecutionJobVertex, execution顶点的数量与并行度
 *     一样多。ExecutionVertex由ExecutionJobVertex和并行子任务的索引标识>
 *
 *   <li>{@link Execution} 是执行 ExecutionVertex 的一次尝试。ExecutionVertex
 *     可能会有多次执行，以防出现故障，或者在后续操作请求某些数据时不再可用，因此需要重新计算这些数据。执行总是由
 *     {@link ExecutionAttemptID} 标识。JobManager 和 TaskManager
 *     之间关于任务部署和任务状态更新的所有消息总是使用 ExecutionAttemptID 来寻址消息接收者。
 *
 * <ul>
 *   <li>The {@link ExecutionJobVertex} represents one vertex from the JobGraph (usually one
 *       operation like "map" or "join") during execution. It holds the aggregated state of all
 *       parallel subtasks. The ExecutionJobVertex is identified inside the graph by the {@link
 *       JobVertexID}, which it takes from the JobGraph's corresponding JobVertex.
 *   <li>The {@link ExecutionVertex} represents one parallel subtask. For each ExecutionJobVertex,
 *       there are as many ExecutionVertices as the parallelism. The ExecutionVertex is identified
 *       by the ExecutionJobVertex and the index of the parallel subtask
 *   <li>The {@link Execution} is one attempt to execute a ExecutionVertex. There may be multiple
 *       Executions for the ExecutionVertex, in case of a failure, or in the case where some data
 *       needs to be recomputed because it is no longer available when requested by later
 *       operations. An Execution is always identified by an {@link ExecutionAttemptID}. All
 *       messages between the JobManager and the TaskManager about deployment of tasks and updates
 *       in the task status always use the ExecutionAttemptID to address the message receiver.
 * </ul>
 */
public interface ExecutionGraph extends AccessExecutionGraph {

    void start(@Nonnull ComponentMainThreadExecutor jobMasterMainThreadExecutor);

    SchedulingTopology getSchedulingTopology();

    void enableCheckpointing(
            // J: 检查点协调器配置
            CheckpointCoordinatorConfiguration chkConfig,
            // J: Master 触发存储钩子
            List<MasterTriggerRestoreHook<?>> masterHooks,
            CheckpointIDCounter checkpointIDCounter,
            CompletedCheckpointStore checkpointStore,
            // J: 检查点状态后端
            StateBackend checkpointStateBackend,
            // J: 检查点存储
            CheckpointStorage checkpointStorage,
            // J: 检查点追踪器
            CheckpointStatsTracker statsTracker,
            // J: 检查点清理器
            CheckpointsCleaner checkpointsCleaner);

    @Nullable
    CheckpointCoordinator getCheckpointCoordinator();

    // J: Kv 状态位置注册
    KvStateLocationRegistry getKvStateLocationRegistry();

    void setJsonPlan(String jsonPlan);

    Configuration getJobConfiguration();

    Throwable getFailureCause();

    @Override
    Iterable<ExecutionJobVertex> getVerticesTopologically();

    @Override
    Iterable<ExecutionVertex> getAllExecutionVertices();

    @Override
    ExecutionJobVertex getJobVertex(JobVertexID id);

    @Override
    Map<JobVertexID, ExecutionJobVertex> getAllVertices();

    /**
     * 获取重新启动的次数，包括完全重新启动和细粒度重新启动。如果恢复当前处于挂起状态，则该恢复包括在计数中。
     *
     * Gets the number of restarts, including full restarts and fine grained restarts. If a recovery
     * is currently pending, this recovery is included in the count.
     *
     * @return The number of restarts so far
     */
    long getNumberOfRestarts();

    int getTotalNumberOfVertices();

    Map<IntermediateDataSetID, IntermediateResult> getAllIntermediateResults();

    /**
     * 合并以前在 execution 中执行的任务的所有累加器结果。
     *
     * Merges all accumulator results from the tasks previously executed in the Executions.
     *
     * @return The accumulator map
     */
    Map<String, OptionalFailure<Accumulator<?, ?>>> aggregateUserAccumulators();

    /**
     * 在作业运行时更新累加器。最终的累加器结果通过 UpdateTaskExecutionState 消息传输。
     *
     * Updates the accumulators during the runtime of a job. Final accumulator results are
     * transferred through the UpdateTaskExecutionState message.
     *
     * @param accumulatorSnapshot The serialized flink and user-defined accumulators
     */
    void updateAccumulators(AccumulatorSnapshot accumulatorSnapshot);

    void setInternalTaskFailuresListener(InternalFailuresListener internalTaskFailuresListener);

    void attachJobGraph(List<JobVertex> topologiallySorted) throws JobException;

    void transitionToRunning();

    void cancel();

    /**
     * 挂起当前ExecutionGraph。
     *
     * Suspends the current ExecutionGraph.
     *
     * <p>The JobStatus will be directly set to {@link JobStatus#SUSPENDED} iff the current state is
     * not a terminal state. All ExecutionJobVertices will be canceled and the onTerminalState() is
     * executed.
     *
     * <p>The {@link JobStatus#SUSPENDED} state is a local terminal state which stops the execution
     * of the job but does not remove the job from the HA job store so that it can be recovered by
     * another JobManager.
     *
     * @param suspensionCause Cause of the suspension
     */
    void suspend(Throwable suspensionCause);

    void failJob(Throwable cause, long timestamp);

    /**
     * Returns the termination future of this {@link ExecutionGraph}. The termination future is
     * completed with the terminal {@link JobStatus} once the ExecutionGraph reaches this terminal
     * state and all {@link Execution} have been terminated.
     *
     * @return Termination future of this {@link ExecutionGraph}.
     */
    CompletableFuture<JobStatus> getTerminationFuture();

    @VisibleForTesting
    JobStatus waitUntilTerminal() throws InterruptedException;

    boolean transitionState(JobStatus current, JobStatus newState);

    void incrementRestarts();

    void initFailureCause(Throwable t, long timestamp);

    /**
     * 更新一个 ExecutionVertex 行尝试的状态。如果新的状态为 "FINISHED"，这也更新了累加器。
     *
     * Updates the state of one of the ExecutionVertex's Execution attempts. If the new status if
     * "FINISHED", this also updates the accumulators.
     *
     * @param state The state update.
     * @return True, if the task update was properly applied, false, if the execution attempt was
     *     not found.
     */
    boolean updateState(TaskExecutionStateTransition state);

    /**
     * Mark the data of a result partition to be available. Note that only PIPELINED partitions are
     * accepted because it is for the case that a TM side PIPELINED result partition has data
     * produced and notifies JM.
     *
     * @param partitionId specifying the result partition whose data have become available
     */
    void notifyPartitionDataAvailable(ResultPartitionID partitionId);

    Map<ExecutionAttemptID, Execution> getRegisteredExecutions();

    void registerJobStatusListener(JobStatusListener listener);

    ResultPartitionAvailabilityChecker getResultPartitionAvailabilityChecker();

    int getNumFinishedVertices();

    @Nonnull
    ComponentMainThreadExecutor getJobMasterMainThreadExecutor();
}
