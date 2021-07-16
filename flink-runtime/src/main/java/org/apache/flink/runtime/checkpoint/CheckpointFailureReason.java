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

package org.apache.flink.runtime.checkpoint;

/** Various reasons why a checkpoint was failure. */
// 检查点失败的各种原因
public enum CheckpointFailureReason {
    // 定期检查点调度程序被关闭
    PERIODIC_SCHEDULER_SHUTDOWN(true, "Periodic checkpoint scheduler is shut down."),

    // 超过了并发检查点的最大数目
    TOO_MANY_CONCURRENT_CHECKPOINTS(
            true, "The maximum number of concurrent checkpoints is exceeded"),

    // 队列检查点请求的最大数目超过
    TOO_MANY_CHECKPOINT_REQUESTS(true, "The maximum number of queued checkpoint requests exceeded"),

    // 检查点之间的最短时间仍未确定。"检查点将在最短时间后触发。"
    MINIMUM_TIME_BETWEEN_CHECKPOINTS(
            true,
            "The minimum time between checkpoints is still pending. "
                    + "Checkpoint will be triggered after the minimum time."),

    // 目前并不是所有必需的任务都在运行。
    NOT_ALL_REQUIRED_TASKS_RUNNING(true, "Not all required tasks are currently running."),

    // 触发检查点时发生异常。
    EXCEPTION(true, "An Exception occurred while triggering the checkpoint."),

    // 异步 Task 检查点失败。
    CHECKPOINT_ASYNC_EXCEPTION(false, "Asynchronous task checkpoint failed."),

    // !检查点在完成之前过期
    CHECKPOINT_EXPIRED(false, "Checkpoint expired before completing."),

    // 检查点已被包含
    CHECKPOINT_SUBSUMED(false, "Checkpoint has been subsumed."),

    // 检查点是拒绝。
    CHECKPOINT_DECLINED(false, "Checkpoint was declined."),

    // 检查点被拒绝(Task 没有准备好)
    CHECKPOINT_DECLINED_TASK_NOT_READY(false, "Checkpoint was declined (tasks not ready)"),

    // 检查点被拒绝(task 的 operators 部分关闭)"
    CHECKPOINT_DECLINED_TASK_CLOSING(
            false, "Checkpoint was declined (task's operators partially closed)"),

    // Task 不支持检查点
    CHECKPOINT_DECLINED_TASK_NOT_CHECKPOINTING(false, "Task does not support checkpointing"),

    // 检查点被取消，因为从新的检查点接收到 barrier
    CHECKPOINT_DECLINED_SUBSUMED(
            false, "Checkpoint was canceled because a barrier from newer checkpoint was received."),

    // Task 从其输入之一接收取消
    CHECKPOINT_DECLINED_ON_CANCELLATION_BARRIER(
            false, "Task received cancellation from one of its inputs"),

    // 检查点对齐阶段需要缓冲比配置的最大字节更多的字节
    CHECKPOINT_DECLINED_ALIGNMENT_LIMIT_EXCEEDED(
            false,
            "The checkpoint alignment phase needed to buffer more than the configured maximum bytes"),

    // 由于一个输入流已完成，检查点被拒绝
    CHECKPOINT_DECLINED_INPUT_END_OF_STREAM(
            false, "Checkpoint was declined because one input stream is finished"),

    // CheckpointCoordinator关闭。
    CHECKPOINT_COORDINATOR_SHUTDOWN(false, "CheckpointCoordinator shutdown."),

    // 检查点协调器挂起。
    CHECKPOINT_COORDINATOR_SUSPEND(false, "Checkpoint Coordinator is suspending."),

    // job 失败了
    JOB_FAILURE(false, "The job has failed."),

    // FailoverRegion重启
    JOB_FAILOVER_REGION(false, "FailoverRegion is restarting."),

    // task 失败了
    TASK_FAILURE(false, "Task has failed."),

    // 任务本地检查点失败。
    TASK_CHECKPOINT_FAILURE(false, "Task local checkpoint failure."),

    // 要通知检查点的未知任务
    UNKNOWN_TASK_CHECKPOINT_NOTIFICATION_FAILURE(
            false, "Unknown task for the checkpoint to notify."),

    // 未能完成检查点。
    FINALIZE_CHECKPOINT_FAILURE(false, "Failure to finalize checkpoint."),

    // 触发检查点失败。
    TRIGGER_CHECKPOINT_FAILURE(false, "Trigger checkpoint failure.");

    // ------------------------------------------------------------------------

    private final boolean preFlight;
    private final String message;

    CheckpointFailureReason(boolean isPreFlight, String message) {
        this.preFlight = isPreFlight;
        this.message = message;
    }

    public String message() {
        return message;
    }

    /**
     * @return true if this value indicates a failure reason happening before a checkpoint is passed
     *     to a job's tasks.
     */
    public boolean isPreFlight() {
        return preFlight;
    }
}
