/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.state;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.util.StringBasedID;

/**
 * 这个类表示一个键，它唯一标识(在逻辑级别上)在{@link SharedStateRegistry}中注册的状态句柄。两个逻辑上应该相同的文
 * 件应该具有相同的{@link SharedStateRegistryKey}。逻辑等价的含义取决于应用。
 *
 * This class represents a key that uniquely identifies (on a logical level) state handles for
 * registration in the {@link SharedStateRegistry}. Two files which should logically be the same
 * should have the same {@link SharedStateRegistryKey}. The meaning of logical equivalence is up to
 * the application.
 */
public class SharedStateRegistryKey extends StringBasedID {

    private static final long serialVersionUID = 1L;

    public SharedStateRegistryKey(String prefix, StateHandleID stateHandleID) {
        super(prefix + '-' + stateHandleID);
    }

    @VisibleForTesting
    public SharedStateRegistryKey(String keyString) {
        super(keyString);
    }
}
