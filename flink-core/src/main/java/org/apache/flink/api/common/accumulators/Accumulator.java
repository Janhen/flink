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

package org.apache.flink.api.common.accumulators;

import org.apache.flink.annotation.Public;

import java.io.Serializable;

/**
 * 累加器从用户函数和 operator 中收集分布式统计信息或聚合。每个并行实例创建并更新自己的累加器对象，累加器的不同并行
 * 实例稍后会合并。在作业结束时被系统合并。结果可以从作业执行的结果中获得，也可以从 web 运行时监视器中获得。
 *
 * <p>累加器的灵感来自 Hadoop/MapReduce 计数器。
 *
 * <p>添加到累加器的类型可能与返回的类型不同。这就是集合累加器的情况:我们添加单个对象，但结果是一组对象。
 *
 * J: 默认情况下在作业结束时候进行系统合并，对应监控，可自定义时间间隔合并所有的累加器结果?
 *
 * Accumulators collect distributed statistics or aggregates in a from user functions and operators.
 * Each parallel instance creates and updates its own accumulator object, and the different parallel
 * instances of the accumulator are later merged. merged by the system at the end of the job. The
 * result can be obtained from the result of a job execution, or from the web runtime monitor.
 *
 * <p>The accumulators are inspired by the Hadoop/MapReduce counters.
 *
 * <p>The type added to the accumulator might differ from the type returned. This is the case e.g.
 * for a set-accumulator: We add single objects, but the result is a set of objects.
 *
 * @param <V> Type of values that are added to the accumulator
 * @param <R> Type of the accumulator result as it will be reported to the client
 */
@Public
public interface Accumulator<V, R extends Serializable> extends Serializable, Cloneable {
    /** @param value The value to add to the accumulator object */
    // 要加到累加器对象的值
    void add(V value);

    /** @return local The local value from the current UDF context */
    // @return local 当前 UDF 上下文的本地值
    R getLocalValue();

    /** Reset the local value. This only affects the current UDF context. */
    // 重置本地值。这仅影响当前的 UDF 上下文。
    void resetLocal();

    /**
     * 由系统内部使用以在作业结束时合并收集器的收集部分。
     *
     * Used by system internally to merge the collected parts of an accumulator at the end of the
     * job.
     *
     * @param other Reference to accumulator to merge in.
     */
    void merge(Accumulator<V, R> other);

    /**
     * 复制累加器。所有子类都需要正确实现克隆并且不能抛出 {@link java.lang.CloneNotSupportedException}
     *
     * Duplicates the accumulator. All subclasses need to properly implement cloning and cannot
     * throw a {@link java.lang.CloneNotSupportedException}
     *
     * @return The duplicated accumulator.
     */
    Accumulator<V, R> clone();
}
