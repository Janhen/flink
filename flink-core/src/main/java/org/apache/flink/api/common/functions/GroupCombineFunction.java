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

package org.apache.flink.api.common.functions;

import org.apache.flink.annotation.Public;
import org.apache.flink.util.Collector;

import java.io.Serializable;

/**
 * 用于组合功能（“组合器”）的通用接口。组合器充当 {@link GroupReduceFunction} 的辅助工具并“预减少”数据。
 * combine 函数通常不会看到整个元素组，而只能看到一个子组。
 *
 * <p>组合函数通常有助于提高程序效率，因为它们允许系统在收集整个组之前更早地减少数据量。
 *
 * <p>combine 函数的这种特殊变体支持每组返回多个元素。它的使用效率通常低于 {@link CombineFunction}。
 *
 * Generic interface used for combine functions ("combiners"). Combiners act as auxiliaries to a
 * {@link GroupReduceFunction} and "pre-reduce" the data. The combine functions typically do not see
 * the entire group of elements, but only a sub-group.
 *
 * <p>Combine functions are frequently helpful in increasing the program efficiency, because they
 * allow the system to reduce the data volume earlier, before the entire groups have been collected.
 *
 * <p>This special variant of the combine function supports to return more than one element per
 * group. It is frequently less efficient to use than the {@link CombineFunction}.
 *
 * @param <IN> The data type processed by the combine function.
 * @param <OUT> The data type emitted by the combine function.
 */
@Public
@FunctionalInterface
public interface GroupCombineFunction<IN, OUT> extends Function, Serializable {

    /**
     * 组合方法，称为（可能是多个定时的）元素子组。
     *
     * The combine method, called (potentially multiple timed) with subgroups of elements.
     *
     * @param values The elements to be combined.
     * @param out The collector to use to return values from the function.
     * @throws Exception The function may throw Exceptions, which will cause the program to cancel,
     *     and may trigger the recovery logic.
     */
    void combine(Iterable<IN> values, Collector<OUT> out) throws Exception;
}
