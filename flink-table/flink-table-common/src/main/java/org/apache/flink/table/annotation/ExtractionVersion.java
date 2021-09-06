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

package org.apache.flink.table.annotation;

import org.apache.flink.annotation.PublicEvolving;

/**
 * 描述基于反射的数据类型提取的预期行为的逻辑版本。
 *
 * <p>这个枚举是为了将来的向后兼容性。每当提取逻辑发生变化时，旧函数和结构化类型类在相应版本化时仍应返回与以前相同的
 *   数据类型。
 *
 * Logical version that describes the expected behavior of the reflection-based data type
 * extraction.
 *
 * <p>This enumeration is meant for future backward compatibility. Whenever the extraction logic is
 * changed, old function and structured type classes should still return the same data type as
 * before when versioned accordingly.
 */
@PublicEvolving
public enum ExtractionVersion {

    /** Default if no version is specified. */
    UNKNOWN,

    /** Initial reflection-based extraction logic according to FLIP-65. */
    V1
}
