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

package org.apache.flink.runtime.client;

import org.apache.flink.api.common.JobID;

/** An exception indicating that the job has failed in the INITIALIZING job status. */
// 一个异常，指示作业在初始化作业状态中失败。
public class JobInitializationException extends JobExecutionException {

    private static final long serialVersionUID = 2818087325120827526L;

    public JobInitializationException(JobID jobID, String msg, Throwable cause) {
        super(jobID, msg, cause);
    }
}
