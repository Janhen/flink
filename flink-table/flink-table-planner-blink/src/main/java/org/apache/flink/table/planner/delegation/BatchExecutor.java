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

package org.apache.flink.table.planner.delegation;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.dag.Pipeline;
import org.apache.flink.api.dag.Transformation;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.table.api.TableConfig;
import org.apache.flink.table.delegation.Executor;
import org.apache.flink.table.planner.utils.ExecutorUtils;

import java.util.List;

/**
 * {@link Executor} 的实现，由 {@link StreamExecutionEnvironment} 支持。这是
 * {@link org.apache.flink.table.planner.delegation.BatchPlanner} 支持的唯一执行器。
 *
 * An implementation of {@link Executor} that is backed by a {@link StreamExecutionEnvironment}.
 * This is the only executor that {@link org.apache.flink.table.planner.delegation.BatchPlanner}
 * supports.
 */
@Internal
public class BatchExecutor extends ExecutorBase {

    @VisibleForTesting
    public BatchExecutor(StreamExecutionEnvironment executionEnvironment) {
        super(executionEnvironment);
    }

    @Override
    public Pipeline createPipeline(
            List<Transformation<?>> transformations, TableConfig tableConfig, String jobName) {
        StreamExecutionEnvironment execEnv = getExecutionEnvironment();
        ExecutorUtils.setBatchProperties(execEnv);
        StreamGraph streamGraph = ExecutorUtils.generateStreamGraph(execEnv, transformations);
        streamGraph.setJobName(getNonEmptyJobName(jobName));
        ExecutorUtils.setBatchProperties(streamGraph, tableConfig);
        return streamGraph;
    }
}
