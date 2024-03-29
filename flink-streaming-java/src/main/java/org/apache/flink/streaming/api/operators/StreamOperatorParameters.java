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

package org.apache.flink.streaming.api.operators;

import org.apache.flink.annotation.Experimental;
import org.apache.flink.runtime.operators.coordination.OperatorEventDispatcher;
import org.apache.flink.streaming.api.graph.StreamConfig;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.streaming.runtime.tasks.StreamTask;

import javax.annotation.Nullable;

import java.util.function.Supplier;

/**
 * Helper类构造{@link AbstractStreamOperatorV2}。包装两个内部参数，以简化用户构造扩展
 * {@link AbstractStreamOperatorV2}的类，并允许在{@link AbstractStreamOperatorV2}的构造函数中进行向后
 * 兼容的更改。
 *
 * Helper class to construct {@link AbstractStreamOperatorV2}. Wraps couple of internal parameters
 * to simplify for users construction of classes extending {@link AbstractStreamOperatorV2} and to
 * allow for backward compatible changes in the {@link AbstractStreamOperatorV2}'s constructor.
 *
 * @param <OUT> The output type of an operator that will be constructed using {@link
 *     StreamOperatorParameters}.
 */
@Experimental
public class StreamOperatorParameters<OUT> {
    // 流任务
    private final StreamTask<?, ?> containingTask;
    // 流相关的配置
    private final StreamConfig config;
    // 下游输出
    private final Output<StreamRecord<OUT>> output;
    // 处理时间提供器
    private final Supplier<ProcessingTimeService> processingTimeServiceFactory;
    // 算子事件分配器
    private final OperatorEventDispatcher operatorEventDispatcher;

    /**
     * ProcessingTimeService是惰性创建的，但被缓存，这样我们就不会创建多个ProcessingTimeService。
     *
     * The ProcessingTimeService, lazily created, but cached so that we don't create more than one.
     */
    @Nullable private ProcessingTimeService processingTimeService;

    public StreamOperatorParameters(
            StreamTask<?, ?> containingTask,
            StreamConfig config,
            Output<StreamRecord<OUT>> output,
            Supplier<ProcessingTimeService> processingTimeServiceFactory,
            OperatorEventDispatcher operatorEventDispatcher) {
        this.containingTask = containingTask;
        this.config = config;
        this.output = output;
        this.processingTimeServiceFactory = processingTimeServiceFactory;
        this.operatorEventDispatcher = operatorEventDispatcher;
    }

    public StreamTask<?, ?> getContainingTask() {
        return containingTask;
    }

    public StreamConfig getStreamConfig() {
        return config;
    }

    public Output<StreamRecord<OUT>> getOutput() {
        return output;
    }

    public ProcessingTimeService getProcessingTimeService() {
        if (processingTimeService == null) {
            processingTimeService = processingTimeServiceFactory.get();
        }
        return processingTimeService;
    }

    public OperatorEventDispatcher getOperatorEventDispatcher() {
        return operatorEventDispatcher;
    }
}
