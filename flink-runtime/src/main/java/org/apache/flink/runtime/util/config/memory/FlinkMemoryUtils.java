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

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;

/**
 * 派生{@link FlinkMemory}组件的实用程序。
 *
 * Utility to derive the {@link FlinkMemory} components.
 *
 * <p>The {@link FlinkMemory} represents memory components which constitute the Total Flink Memory.
 * The Flink memory components can be derived from either its total size or a subset of configured
 * required fine-grained components. See implementations for details about the concrete fine-grained
 * components.
 *
 * @param <FM> the Flink memory components
 */
public interface FlinkMemoryUtils<FM extends FlinkMemory> {
    FM deriveFromRequiredFineGrainedOptions(Configuration config);

    FM deriveFromTotalFlinkMemory(Configuration config, MemorySize totalFlinkMemorySize);
}
