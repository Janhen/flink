/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.flink.runtime.scheduler;

/**
 * 处理全局故障的接口。在调度器上下文中，我们区分本地和全局故障。全局故障是发生在调度器上下文中(在JobManager进程中)
 * 的故障，而本地故障是发生在执行任务的“本地”故障。
 *
 * An interface for handling global failures. In context of a scheduler we distinguish between local
 * and global failures. Global failure is the one that happens in context of the scheduler (in the
 * JobManager process) and local failure is one that is "local" to an executing task.
 */
@FunctionalInterface
public interface GlobalFailureHandler {

    /**
     * Handles a global failure.
     *
     * @param cause A cause that describes the global failure.
     */
    void handleGlobalFailure(Throwable cause);
}
