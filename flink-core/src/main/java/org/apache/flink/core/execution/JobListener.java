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

package org.apache.flink.core.execution;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.JobExecutionResult;

import javax.annotation.Nullable;

/**
 * 一个监听器，它会在特定的作业状态改变时收到通知，应该首先通过执行环境的{@code #registerJobListener}注册。
 *
 * <p>强烈建议不要在回调函数中执行任何阻塞操作。如果阻塞线程，则环境执行方法的调用程序可能会阻塞。
 *
 * A listener that is notified on specific job status changed, which should be firstly registered by
 * {@code #registerJobListener} of execution environments.
 *
 * <p>It is highly recommended NOT to perform any blocking operation inside the callbacks. If you
 * block the thread the invoker of environment execute methods is possibly blocked.
 */
@PublicEvolving
public interface JobListener {

    /**
     * 作业提交的回调。这在调用 {@code execute()} 或 {@code executeAsync()} 时被调用。
     *
     * <p>传入的参数中恰好有一个为null，分别代表失败或成功。
     *
     * Callback on job submission. This is called when {@code execute()} or {@code executeAsync()}
     * is called.
     *
     * <p>Exactly one of the passed parameters is null, respectively for failure or success.
     *
     * @param jobClient a {@link JobClient} for the submitted Flink job
     * @param throwable the cause if submission failed
     */
    void onJobSubmitted(@Nullable JobClient jobClient, @Nullable Throwable throwable);

    /**
     * Callback on job execution finished, successfully or unsuccessfully. It is only called back
     * when you call {@code execute()} instead of {@code executeAsync()} methods of execution
     * environments.
     *
     * <p>Exactly one of the passed parameters is null, respectively for failure or success.
     */
    void onJobExecuted(
            @Nullable JobExecutionResult jobExecutionResult, @Nullable Throwable throwable);
}
