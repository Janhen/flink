/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.client;

import org.apache.flink.api.common.JobID;

/**
 * 异常，如果提交的作业正在执行，则在作业提交时返回。
 *
 * Exception which is returned upon job submission if the submitted job is currently being executed.
 */
public class DuplicateJobSubmissionException extends JobSubmissionException {

    public DuplicateJobSubmissionException(JobID jobID) {
        super(jobID, "Job has already been submitted.");
    }
}
