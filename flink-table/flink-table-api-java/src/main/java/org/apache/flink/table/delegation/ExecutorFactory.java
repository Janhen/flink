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

package org.apache.flink.table.delegation;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.factories.ComponentFactory;

import java.util.Map;

/**
 * 创建{@link Executor}的工厂。
 *
 * <p>该工厂与Java的服务提供者接口(Service Provider Interfaces, SPI)一起使用，用于发现。使用一组描述所需配置的
 *   规范化属性调用工厂。这些属性可能包括执行配置，如水印间隔、最大并行度等，特定于表的初始化配置，如查询是否应该以批处理模式执行。
 *
 * Factory that creates {@link Executor}.
 *
 * <p>This factory is used with Java's Service Provider Interfaces (SPI) for discovering. A factory
 * is called with a set of normalized properties that describe the desired configuration. Those
 * properties may include execution configurations such as watermark interval, max parallelism etc.,
 * table specific initialization configuration such as if the queries should be executed in batch
 * mode.
 *
 * <p><b>Important:</b> The implementations of this interface should also implement method
 *
 * <pre>
 * {@code public Executor create(Map<String, String> properties, StreamExecutionEnvironment executionEnvironment);}
 * </pre>
 *
 * <p>This method will be used when instantiating a {@link
 * org.apache.flink.table.api.TableEnvironment} from a bridging module which enables conversion
 * from/to {@code DataStream} API and requires a pre configured {@code StreamTableEnvironment}.
 */
@Internal
public interface ExecutorFactory extends ComponentFactory {

    /**
     * Creates a corresponding {@link Executor}.
     *
     * @param properties Static properties of the {@link Executor}, the same that were used for
     *     factory lookup.
     * @return instance of a {@link Executor}
     */
    Executor create(Map<String, String> properties);
}
