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

package org.apache.flink.runtime.checkpoint;

import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.FlinkRuntimeException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

/** The checkpoint failure manager which centralized manage checkpoint failure processing logic. */
// 集中管理检查点故障处理逻辑的检查点故障管理器。
public class CheckpointFailureManager {

    // 无限可容忍故障数
    public static final int UNLIMITED_TOLERABLE_FAILURE_NUMBER = Integer.MAX_VALUE;
    // 超过检查点可容忍故障阈值。
    public static final String EXCEEDED_CHECKPOINT_TOLERABLE_FAILURE_MESSAGE =
            "Exceeded checkpoint tolerable failure threshold.";

    private final int tolerableCpFailureNumber;
    private final FailJobCallback failureCallback;
    private final AtomicInteger continuousFailureCounter;
    private final Set<Long> countedCheckpointIds;

    public CheckpointFailureManager(int tolerableCpFailureNumber, FailJobCallback failureCallback) {
        checkArgument(
                tolerableCpFailureNumber >= 0,
                "The tolerable checkpoint failure number is illegal, "
                        + "it must be greater than or equal to 0 .");
        this.tolerableCpFailureNumber = tolerableCpFailureNumber;
        this.continuousFailureCounter = new AtomicInteger(0);
        this.failureCallback = checkNotNull(failureCallback);
        this.countedCheckpointIds = ConcurrentHashMap.newKeySet();
    }

    /**
     * 使用处理程序回调处理作业级检查点异常。
     *
     * Handle job level checkpoint exception with a handler callback.
     *
     * @param exception the checkpoint exception.
     * @param checkpointId the failed checkpoint id used to count the continuous failure number
     *     based on checkpoint id sequence. In trigger phase, we may not get the checkpoint id when
     *     the failure happens before the checkpoint id generation. In this case, it will be
     *     specified a negative latest generated checkpoint id as a special flag.
     *     失败的检查点id，用于根据检查点id序列计算连续失败的次数。在触发阶段，当故障在生成检查点id之前发生时，我们可
     *     能无法获得检查点id。在这种情况下，它将被指定一个负的最新生成的检查点id作为一个特殊标志。
     */
    public void handleJobLevelCheckpointException(
            CheckpointException exception, long checkpointId) {
        checkFailureCounter(exception, checkpointId);
        if (continuousFailureCounter.get() > tolerableCpFailureNumber) {
            clearCount();
            failureCallback.failJob(
                    new FlinkRuntimeException("Exceeded checkpoint tolerable failure threshold."));
        }
    }

    /**
     * 使用处理程序回调处理任务级检查点异常。
     *
     * Handle task level checkpoint exception with a handler callback.
     *
     * @param exception the checkpoint exception.
     * @param checkpointId the failed checkpoint id used to count the continuous failure number
     *     based on checkpoint id sequence. In trigger phase, we may not get the checkpoint id when
     *     the failure happens before the checkpoint id generation. In this case, it will be
     *     specified a negative latest generated checkpoint id as a special flag.
     * @param executionAttemptID the execution attempt id, as a safe guard.
     */
    public void handleTaskLevelCheckpointException(
            CheckpointException exception,
            long checkpointId,
            ExecutionAttemptID executionAttemptID) {
        checkFailureCounter(exception, checkpointId);
        if (continuousFailureCounter.get() > tolerableCpFailureNumber) {
            clearCount();
            // 超过检查点可容忍失败阈值
            failureCallback.failJobDueToTaskFailure(
                    new FlinkRuntimeException("Exceeded checkpoint tolerable failure threshold."),
                    executionAttemptID);
        }
    }

    public void checkFailureCounter(CheckpointException exception, long checkpointId) {
        if (tolerableCpFailureNumber == UNLIMITED_TOLERABLE_FAILURE_NUMBER) {
            return;
        }

        CheckpointFailureReason reason = exception.getCheckpointFailureReason();
        switch (reason) {
            case PERIODIC_SCHEDULER_SHUTDOWN:
            case TOO_MANY_CONCURRENT_CHECKPOINTS:
            case TOO_MANY_CHECKPOINT_REQUESTS:
            case MINIMUM_TIME_BETWEEN_CHECKPOINTS:
            case NOT_ALL_REQUIRED_TASKS_RUNNING:
            case CHECKPOINT_SUBSUMED:
            case CHECKPOINT_COORDINATOR_SUSPEND:
            case CHECKPOINT_COORDINATOR_SHUTDOWN:
            case JOB_FAILURE:
            case JOB_FAILOVER_REGION:
                // for compatibility purposes with user job behavior
            case CHECKPOINT_DECLINED_TASK_NOT_READY:
            case CHECKPOINT_DECLINED_TASK_CLOSING:
            case CHECKPOINT_DECLINED_TASK_NOT_CHECKPOINTING:
            case CHECKPOINT_DECLINED_ALIGNMENT_LIMIT_EXCEEDED:
            case CHECKPOINT_DECLINED_ON_CANCELLATION_BARRIER:
            case CHECKPOINT_DECLINED_SUBSUMED:
            case CHECKPOINT_DECLINED_INPUT_END_OF_STREAM:

            case EXCEPTION:
            case TASK_FAILURE:
            case TASK_CHECKPOINT_FAILURE:
            case UNKNOWN_TASK_CHECKPOINT_NOTIFICATION_FAILURE:
            case TRIGGER_CHECKPOINT_FAILURE:
            case FINALIZE_CHECKPOINT_FAILURE:
                // ignore
                break;

            case CHECKPOINT_ASYNC_EXCEPTION:
            case CHECKPOINT_DECLINED:
            case CHECKPOINT_EXPIRED:
                // we should make sure one checkpoint only be counted once
                if (countedCheckpointIds.add(checkpointId)) {
                    continuousFailureCounter.incrementAndGet();
                }

                break;

            default:
                throw new FlinkRuntimeException(
                        "Unknown checkpoint failure reason : " + reason.name());
        }
    }

    /**
     * 成功处理检查点。
     *
     * Handle checkpoint success.
     *
     * @param checkpointId the failed checkpoint id used to count the continuous failure number
     *     based on checkpoint id sequence.
     */
    public void handleCheckpointSuccess(@SuppressWarnings("unused") long checkpointId) {
        clearCount();
    }

    private void clearCount() {
        continuousFailureCounter.set(0);
        countedCheckpointIds.clear();
    }

    /**
     * 如果正在进行的同步保存点被丢弃，则使整个作业图失败。
     *
     * <p>如果检查点在检查点协调器上被取消，即在同步保存点屏障被发送到任务之前，那么我们不会取消作业，因为我们不会有死锁
     * 的风险。
     *
     * Fails the whole job graph in case an in-progress synchronous savepoint is discarded.
     *
     * <p>If the checkpoint was cancelled at the checkpoint coordinator, i.e. before the synchronous
     * savepoint barrier was sent to the tasks, then we do not cancel the job as we do not risk
     * having a deadlock.
     *
     * @param cause The reason why the job is cancelled.
     */
    void handleSynchronousSavepointFailure(final Throwable cause) {
        if (!isPreFlightFailure(cause)) {
            failureCallback.failJob(cause);
        }
    }

    private static boolean isPreFlightFailure(final Throwable cause) {
        return ExceptionUtils.findThrowable(cause, CheckpointException.class)
                .map(CheckpointException::getCheckpointFailureReason)
                .map(CheckpointFailureReason::isPreFlight)
                .orElse(false);
    }

    /** A callback interface about how to fail a job. */
    // 关于如何使作业失败的回调接口
    public interface FailJobCallback {

        /**
         * 无法完成整个 job graph
         *
         * Fails the whole job graph.
         *
         * @param cause The reason why the synchronous savepoint fails.
         */
        void failJob(final Throwable cause);

        /**
         * 由于任务失败而导致整个 job graph 失败。
         *
         * Fails the whole job graph due to task failure.
         *
         * @param cause The reason why the job is cancelled.
         * @param failingTask The id of the failing task attempt to prevent failing the job multiple
         *     times.
         */
        void failJobDueToTaskFailure(final Throwable cause, final ExecutionAttemptID failingTask);
    }
}
