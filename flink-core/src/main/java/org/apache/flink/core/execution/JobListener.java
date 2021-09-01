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
 * 一个 listener，当特定的作业状态发生改变时，它会得到通知，首先应该由执行环境的 {@code #registerJobListener}
 * 注册。
 *
 * <p>强烈建议不要在回调内部执行任何阻塞操作。如果阻塞了线程，那么环境执行方法的调用者就可能被阻塞。
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
     * 作业提交的回调。当 {@code execute()} 或 {@code executeAsync()} 被调用时，会调用这个函数。
     *
     * 传递的参数中有一个是 null，分别表示失败或成功。
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
     * 作业执行完成时的回调，成功或不成功。只有在调用执行环境的 {@code execute()} 而不是 {@code executeAsync()}
     * 方法时，它才会被回调。
     *
     * <p>传递的参数中有一个是 null，分别表示失败或成功。
     *
     * Callback on job execution finished, successfully or unsuccessfully. It is only called back
     * when you call {@code execute()} instead of {@code executeAsync()} methods of execution
     * environments.
     *
     * <p>Exactly one of the passed parameters is null, respectively for failure or success.
     */
    void onJobExecuted(
            @Nullable JobExecutionResult jobExecutionResult, @Nullable Throwable throwable);
}
