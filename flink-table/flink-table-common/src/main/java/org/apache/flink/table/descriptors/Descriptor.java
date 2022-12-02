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

package org.apache.flink.table.descriptors;

import org.apache.flink.annotation.PublicEvolving;

import java.util.Map;

/**
 * 接口，该接口添加一组用于描述 DDL 信息的基于字符串的规范化属性。
 *
 * <p>描述符的典型特征是:—描述符有一个默认构造函数—描述符本身包含很少的逻辑—相应的验证器验证正确性(目标:有一个单点验证)
 *
 * <p>描述符类似于构建器模式中的构建器，因此，对于构建属性来说是可变的。
 *
 * Interface that adds a set of string-based, normalized properties for describing DDL information.
 *
 * <p>Typical characteristics of a descriptor are: - descriptors have a default constructor -
 * descriptors themselves contain very little logic - corresponding validators validate the
 * correctness (goal: have a single point of validation)
 *
 * <p>A descriptor is similar to a builder in a builder pattern, thus, mutable for building
 * properties.
 */
@PublicEvolving
public interface Descriptor {

    /** Converts this descriptor into a set of properties. */
    // 将此描述符转换为一组属性。
    Map<String, String> toProperties();
}
