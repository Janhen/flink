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
 * 模式选择函数的基本接口，它可以产生多个结果元素。模式平面选择函数是通过检测到的事件映射调用的，这些事件通过名称进行
 * 标识。这些名称由{@link org.apache.flink.cep.pattern.Pattern}。指定受追捧的模式。此外，还提供了一个收集器参数。
 * 收集器用于发出任意数量的结果元素。
 *
 * Base interface for a pattern select function which can produce multiple resulting elements. A
 * pattern flat select function is called with a map of detected events which are identified by
 * their names. The names are defined by the {@link org.apache.flink.cep.pattern.Pattern} specifying
 * the sought-after pattern. Additionally, a collector is provided as a parameter. The collector is
 * used to emit an arbitrary number of resulting elements.
 *
 * <pre>{@code
 * PatternStream<IN> pattern = ...
 *
 * DataStream<OUT> result = pattern.flatSelect(new MyPatternFlatSelectFunction());
 * }</pre>
 *
 * @param <IN>
 * @param <OUT>
 */
public interface PatternFlatSelectFunction<IN, OUT> extends Function, Serializable {

    /**
     * 根据检测到的模式事件映射生成零个或多个结果元素。事件由其指定的名称标识。
     *
     * Generates zero or more resulting elements given a map of detected pattern events. The events
     * are identified by their specified names.
     *
     * @param pattern Map containing the found pattern. Events are identified by their names.
     * @param out Collector used to output the generated elements
     * @throws Exception This method may throw exceptions. Throwing an exception will cause the
     *     operation to fail and may trigger recovery.
     */
    void flatSelect(Map<String, List<IN>> pattern, Collector<OUT> out) throws Exception;
}
