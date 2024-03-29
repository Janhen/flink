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

import org.apache.flink.annotation.Public;

/** The result of submitting a job to a JobManager. */
// 将作业提交到 JobManager 的结果。
@Public
public class JobSubmissionResult {

    private final JobID jobID;

    public JobSubmissionResult(JobID jobID) {
        this.jobID = jobID;
    }

    /**
     * Returns the JobID assigned to the job by the Flink runtime.
     *
     * @return jobID, or null if the job has been executed on a runtime without JobIDs or if the
     *     execution failed.
     */
    public JobID getJobID() {
        return jobID;
    }

    /**
     * 检查此 JobSubmissionResult 是否也是 JobExecutionResult。请参阅 {@code getJobExecutionResult}
     * 以检索 JobExecutionResult。
     *
     * Checks if this JobSubmissionResult is also a JobExecutionResult. See {@code
     * getJobExecutionResult} to retrieve the JobExecutionResult.
     *
     * @return True if this is a JobExecutionResult, false otherwise
     */
    public boolean isJobExecutionResult() {
        return false;
    }

    /**
     * 如果可用，则返回 JobExecutionResult。
     *
     * Returns the JobExecutionResult if available.
     *
     * @return The JobExecutionResult
     * @throws ClassCastException if this is not a JobExecutionResult
     */
    public JobExecutionResult getJobExecutionResult() {
        throw new ClassCastException("This JobSubmissionResult is not a JobExecutionResult.");
    }
}
