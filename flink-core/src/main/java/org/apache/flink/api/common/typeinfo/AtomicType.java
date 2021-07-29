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

package org.apache.flink.api.common.typeinfo;

import org.apache.flink.annotation.Public;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeComparator;

/**
 * 原子类型是一种类型，它被视为一个不可分割的单元，整个类型作为一个键。原子类型定义了为该类型创建比较器的方法作为键。
 * 原子类型的例子包括基本类型(int, long, String，…)和类似的自定义类。
 *
 * <p>与原子类型相比，是复合类型，其中类型信息可以识别单个字段，并且单个字段可以用作键。
 *
 * An atomic type is a type that is treated as one indivisible unit and where the entire type acts
 * as a key. The atomic type defines the method to create a comparator for this type as a key.
 * Example atomic types are the basic types (int, long, String, ...) and comparable custom classes.
 *
 * <p>In contrast to atomic types are composite types, where the type information is aware of the
 * individual fields and individual fields may be used as a key.
 */
@Public
public interface AtomicType<T> {

    /**
     * Creates a comparator for this type.
     *
     * @param sortOrderAscending True, if the comparator should define the order to be ascending,
     *     false, if the comparator should define the order to be descending.
     * @param executionConfig The config from which the comparator will be parametrized.
     *     Parametrization includes for example registration of class tags for frameworks like Kryo.
     * @return A comparator for this type.
     */
    TypeComparator<T> createComparator(boolean sortOrderAscending, ExecutionConfig executionConfig);
}
