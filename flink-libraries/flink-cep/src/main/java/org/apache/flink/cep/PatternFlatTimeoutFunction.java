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

package org.apache.flink.cep;

import org.apache.flink.api.common.functions.Function;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 模式超时函数的基本接口，它可以产生多个结果元素。使用部分事件的映射调用模式平面超时函数，这些事件由它们的名称和超时
 * 发生时的时间戳标识。这些名称由{@link org.apache.flink.cep.pattern.Pattern}定义。指定受追捧的模式。此外，还
 * 提供了一个收集器参数。收集器用于发出任意数量的结果元素。
 *
 * Base interface for a pattern timeout function which can produce multiple resulting elements. A
 * pattern flat timeout function is called with a map of partial events which are identified by
 * their names and the timestamp when the timeout occurred. The names are defined by the {@link
 * org.apache.flink.cep.pattern.Pattern} specifying the sought-after pattern. Additionally, a
 * collector is provided as a parameter. The collector is used to emit an arbitrary number of
 * resulting elements.
 *
 * <pre>{@code
 * PatternStream<IN> pattern = ...
 *
 * DataStream<OUT> result = pattern.flatSelect(..., new MyPatternFlatTimeoutFunction());
 * }</pre>
 *
 * @param <IN>
 * @param <OUT>
 */
public interface PatternFlatTimeoutFunction<IN, OUT> extends Function, Serializable {

    /**
     * 给定部分模式事件的映射和超时的时间戳，生成零个或多个结果超时元素。事件由其指定的名称标识。
     *
     * Generates zero or more resulting timeout elements given a map of partial pattern events and
     * the timestamp of the timeout. The events are identified by their specified names.
     *
     * @param pattern Map containing the partial pattern. Events are identified by their names.
     * @param timeoutTimestamp Timestamp when the timeout occurred
     *                         超时时间戳
     * @param out Collector used to output the generated elements
     * @throws Exception This method may throw exceptions. Throwing an exception will cause the
     *     operation to fail and may trigger recovery.
     */
    void timeout(Map<String, List<IN>> pattern, long timeoutTimestamp, Collector<OUT> out)
            throws Exception;
}
