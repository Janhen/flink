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

package org.apache.flink.core.memory;

import org.apache.flink.annotation.Internal;
import org.apache.flink.util.Preconditions;

/** Use cases of managed memory. */
// 托管内存的用例
@Internal
public enum ManagedMemoryUseCase {
    // 算子级别
    OPERATOR(Scope.OPERATOR),
    // J: 状态后端管理的内存级别
    STATE_BACKEND(Scope.SLOT),
    // Slot 级别
    PYTHON(Scope.SLOT);

    public final Scope scope;

    ManagedMemoryUseCase(Scope scope) {
        this.scope = Preconditions.checkNotNull(scope);
    }

    /** Scope at which memory is managed for a use case. */
    // 为用例管理内存的作用域
    public enum Scope {
        // J: 槽级别的内存管理作用域
        SLOT,
        // J: 算子级别的内存管理作用域
        OPERATOR
    }
}
