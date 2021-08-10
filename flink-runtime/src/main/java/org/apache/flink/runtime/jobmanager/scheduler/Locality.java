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

package org.apache.flink.runtime.jobmanager.scheduler;

// J: Job 的本地性?
public enum Locality {

    /** No constraint existed on the task placement. */
    // 任务的位置不存在约束。
    UNCONSTRAINED,

    /** The task was scheduled into the same TaskManager as requested */
    // 任务按照请求被调度到同一个 TaskManager 中
    LOCAL,

    /** The task was scheduled onto the same host as requested */
    // 任务被调度到请求的同一主机上
    HOST_LOCAL,

    /** The task was scheduled to a destination not included in its locality preferences. */
    // 任务被调度到不包含在其位置首选项中的目的地。
    NON_LOCAL,

    // 没有提供位置信息，不知道位置是否被 respected
    /** No locality information was provided, it is unknown if the locality was respected */
    UNKNOWN
}
