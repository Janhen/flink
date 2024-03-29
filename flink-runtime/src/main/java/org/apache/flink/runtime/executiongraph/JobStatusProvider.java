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

import org.apache.flink.api.common.JobStatus;

/** Interface for querying the state of a job and the timestamps of state transitions. */
// 接口功能查询作业的状态和状态转换的时间戳
public interface JobStatusProvider {

    /**
     * Returns the current {@link JobStatus} for this execution graph.
     *
     * @return job status for this execution graph
     */
    JobStatus getState();

    /**
     * 返回给定 {@link JobStatus} 的时间戳
     *
     * Returns the timestamp for the given {@link JobStatus}.
     *
     * @param status status for which the timestamp should be returned
     * @return timestamp for the given job status
     */
    long getStatusTimestamp(JobStatus status);
}
