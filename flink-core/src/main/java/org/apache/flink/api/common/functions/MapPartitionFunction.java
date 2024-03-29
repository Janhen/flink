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
 * “mapPartition”函数的接口。一个“mapPartition”函数被调用一次，每个数据分区接收一个带有该分区数据元素的 Iterable。
 * 它可以返回任意数量的数据元素。
 *
 * <p>此功能旨在提高分区中元素处理的灵活性。对于大多数简单用例，请考虑使用 {@link MapFunction} 或
 *   {@link FlatMapFunction}。
 *
 * <p>MapPartitionFunction 的基本语法如下：
 *
 * ...
 *
 * Interface for "mapPartition" functions. A "mapPartition" function is called a single time per
 * data partition receives an Iterable with data elements of that partition. It may return an
 * arbitrary number of data elements.
 *
 * <p>This function is intended to provide enhanced flexibility in the processing of elements in a
 * partition. For most of the simple use cases, consider using the {@link MapFunction} or {@link
 * FlatMapFunction}.
 *
 * <p>The basic syntax for a MapPartitionFunction is as follows:
 *
 * <pre>{@code
 * DataSet<X> input = ...;
 *
 * DataSet<Y> result = input.mapPartition(new MyMapPartitionFunction());
 * }</pre>
 *
 * @param <T> Type of the input elements.
 * @param <O> Type of the returned elements.
 */
@Public
@FunctionalInterface
public interface MapPartitionFunction<T, O> extends Function, Serializable {

    /**
     * 修改或转换传入对象的用户实现的函数。
     *
     * A user-implemented function that modifies or transforms an incoming object.
     *
     * @param values All records for the mapper
     * @param out The collector to hand results to.
     * @throws Exception This method may throw exceptions. Throwing an exception will cause the
     *     operation to fail and may trigger recovery.
     */
    void mapPartition(Iterable<T> values, Collector<O> out) throws Exception;
}
