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

package org.apache.flink.table.runtime.operators.bundle.trigger;

import org.apache.flink.annotation.Internal;

/** Interface for bundle trigger callbacks that can be registered to a {@link BundleTrigger}. */
// 可以注册到 {@link BundleTrigger} 的捆绑触发器回调接口。
@Internal
public interface BundleTriggerCallback {

    /**
     * 调用此方法以完成当前包并在触发触发器时启动新包。
     *
     * This method is invoked to finish current bundle and start a new one when the trigger was
     * fired.
     *
     * @throws Exception This method may throw exceptions. Throwing an exception will cause the
     *     operation to fail and may trigger recovery.
     */
    void finishBundle() throws Exception;
}
