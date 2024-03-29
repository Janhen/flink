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

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.dag.Pipeline;
import org.apache.flink.configuration.Configuration;

import java.util.concurrent.CompletableFuture;

/** The entity responsible for executing a {@link Pipeline}, i.e. a user job. */
// 负责执行 {@link Pipeline} 的实体，例如一个用户作业
@Internal
public interface PipelineExecutor {

    /**
     * 根据提供的配置执行一个 {@link Pipeline}，并返回一个 {@link JobClient}，允许与正在执行的作业进行交互，
     * 例如取消它或取一个保存点。
     *
     * <p><b>注意:<b>调用者负责管理返回的 {@link JobClient} 的生命周期。这意味着，
     * 例如 {@code close()} 应该在调用站点被显式调用。
     *
     * Executes a {@link Pipeline} based on the provided configuration and returns a {@link
     * JobClient} which allows to interact with the job being executed, e.g. cancel it or take a
     * savepoint.
     *
     * <p><b>ATTENTION:</b> The caller is responsible for managing the lifecycle of the returned
     * {@link JobClient}. This means that e.g. {@code close()} should be called explicitly at the
     * call-site.
     *
     * @param pipeline the {@link Pipeline} to execute
     * @param configuration the {@link Configuration} with the required execution parameters
     * @param userCodeClassloader the {@link ClassLoader} to deserialize usercode
     * @return a {@link CompletableFuture} with the {@link JobClient} corresponding to the pipeline.
     */
    CompletableFuture<JobClient> execute(
            final Pipeline pipeline,
            final Configuration configuration,
            // J: parent-first, user...
            final ClassLoader userCodeClassloader)
            throws Exception;
}
