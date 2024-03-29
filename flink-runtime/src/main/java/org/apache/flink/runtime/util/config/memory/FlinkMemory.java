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

package org.apache.flink.runtime.util.config.memory;

import org.apache.flink.configuration.MemorySize;

import java.io.Serializable;

/**
 * 组成总Flink内存的内存组件。
 *
 * Memory components which constitute the Total Flink Memory.
 *
 * <p>The relationships of Flink JVM and rest memory components are shown below.
 *
 * <pre>
 *               ┌ ─ ─  Total Flink Memory - ─ ─ ┐
 *                 ┌───────────────────────────┐
 *               | │       JVM Heap Memory     │ |
 *                 └───────────────────────────┘
 *               |┌ ─ ─ - - - Off-Heap  - - ─ ─ ┐|
 *                │┌───────────────────────────┐│
 *               │ │     JVM Direct Memory     │ │
 *                │└───────────────────────────┘│
 *               │ ┌───────────────────────────┐ │
 *                ││   Rest Off-Heap Memory    ││
 *               │ └───────────────────────────┘ │
 *                └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *               └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 * </pre>
 *
 * <p>The JVM and rest memory components can consist of further concrete Flink memory components
 * depending on the process type. The Flink memory components can be derived from either its total
 * size or a subset of configured required fine-grained components. Check the implementations for
 * details about the concrete components.
 */
public interface FlinkMemory extends Serializable {
    MemorySize getJvmHeapMemorySize();

    MemorySize getJvmDirectMemorySize();

    MemorySize getTotalFlinkMemorySize();
}
