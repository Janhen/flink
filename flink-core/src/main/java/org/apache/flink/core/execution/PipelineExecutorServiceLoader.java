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
import org.apache.flink.configuration.Configuration;

import java.util.stream.Stream;

/**
 * 一个由实体实现的接口，该实体负责找到正确的 {@link PipelineExecutor} 来执行给定的
 * {@link org.apache.flink.api.dag.Pipeline}。
 *
 * An interface to be implemented by the entity responsible for finding the correct {@link
 * PipelineExecutor} to execute a given {@link org.apache.flink.api.dag.Pipeline}.
 */
@Internal
public interface PipelineExecutorServiceLoader {

    /**
     * 加载与提供的配置兼容的 {@link PipelineExecutorFactory}。可用的工厂中最多可以有一个兼容的工厂，否则会抛出异常。
     *
     * Loads the {@link PipelineExecutorFactory} which is compatible with the provided
     * configuration. There can be at most one compatible factory among the available ones,
     * otherwise an exception will be thrown.
     *
     * @return a compatible {@link PipelineExecutorFactory}.
     * @throws Exception if there is more than one compatible factories, or something went wrong
     *     when loading the registered factories.
     */
    PipelineExecutorFactory getExecutorFactory(final Configuration configuration) throws Exception;

    /** Loads and returns a stream of the names of all available executors. */
    // 加载并返回所有可用执行器的名称流。
    Stream<String> getExecutorNames();
}
