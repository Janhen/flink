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

package org.apache.flink.api.java.typeutils;

import org.apache.flink.annotation.Public;
import org.apache.flink.api.common.typeinfo.TypeInformation;

/**
 * 这个接口可以通过函数和输入格式来实现，以告诉 framework 它们生成的数据类型。此方法可作为反射分析的替代方法，在生成的
 * 数据类型可能因参数化而不同的情况下非常有用。
 *
 * J: 对于结果类型是 {@code List<T>} 之类带有的泛型的类型，实现
 *
 * This interface can be implemented by functions and input formats to tell the framework about
 * their produced data type. This method acts as an alternative to the reflection analysis that is
 * otherwise performed and is useful in situations where the produced data type may vary depending
 * on parametrization.
 */
@Public
public interface ResultTypeQueryable<T> {

    /**
     * 获取由该函数或输入格式产生的数据类型(作为 {@link TypeInformation})。
     *
     * Gets the data type (as a {@link TypeInformation}) produced by this function or input format.
     *
     * @return The data type produced by this function or input format.
     */
    TypeInformation<T> getProducedType();
}
