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

package org.apache.flink.table.connector;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.connector.sink.DynamicTableSink.SinkRuntimeProvider;

import java.util.Optional;

/**
 * 其他连接器提供程序的并行提供程序。它允许表达连接器运行时实现的自定义并行性。否则并行度由规划器决定。
 *
 * <p>注:目前，该接口仅与 {@link SinkRuntimeProvider} 一起工作。
 *
 * Parallelism provider for other connector providers. It allows to express a custom parallelism for
 * the connector runtime implementation. Otherwise the parallelism is determined by the planner.
 *
 * <p>Note: Currently, this interface only works with {@link SinkRuntimeProvider}.
 */
@PublicEvolving
public interface ParallelismProvider {

    /**
     * 返回此实例的并行度。
     *
     * <p>并行度表示在执行期间将生成多少个源或接收器的并行实例。
     *
     * <p>如果输入不是 {@link ChangelogMode#insertOnly()}，对接收器强制不同的并行度可能会打乱变更日志。
     *   因此，需要一个主键，在记录进入 {@link SinkRuntimeProvider} 实现之前，输入将被 shuffle。
     *
     * Returns the parallelism for this instance.
     *
     * <p>The parallelism denotes how many parallel instances of a source or sink will be spawned
     * during the execution.
     *
     * <p>Enforcing a different parallelism for sinks might mess up the changelog if the input is
     * not {@link ChangelogMode#insertOnly()}. Therefore, a primary key is required by which the
     * input will be shuffled before records enter the {@link SinkRuntimeProvider} implementation.
     *
     * @return empty if the connector does not provide a custom parallelism, then the planner will
     *     decide the number of parallel instances by itself.
     */
    default Optional<Integer> getParallelism() {
        return Optional.empty();
    }
}
