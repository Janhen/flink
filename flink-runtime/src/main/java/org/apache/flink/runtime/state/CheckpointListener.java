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

package org.apache.flink.runtime.state;

import org.apache.flink.annotation.Public;

/**
 * 该接口必须由希望在所有参与者完全确认检查点后接收提交通知的功能操作实现。
 *
 * This interface must be implemented by functions/operations that want to receive a commit
 * notification once a checkpoint has been completely acknowledged by all participants.
 */
@Public
public interface CheckpointListener {

    /**
     * 一旦分布式检查点完成，此方法将作为通知调用。
     *
     * <p>请注意，此方法期间的任何异常都不会导致检查点再次失败。
     *
     * This method is called as a notification once a distributed checkpoint has been completed.
     *
     * <p>Note that any exception during this method will not cause the checkpoint to fail any more.
     *
     * @param checkpointId The ID of the checkpoint that has been completed.
     * @throws Exception This method can propagate exceptions, which leads to a failure/recovery for
     *     the task. Not that this will NOT lead to the checkpoint being revoked.
     */
    void notifyCheckpointComplete(long checkpointId) throws Exception;

    /**
     * 一旦分布式检查点被中止，此方法将作为通知调用。
     *
     * This method is called as a notification once a distributed checkpoint has been aborted.
     *
     * @param checkpointId The ID of the checkpoint that has been aborted.
     * @throws Exception This method can propagate exceptions, which leads to a failure/recovery for
     *     the task.
     */
    default void notifyCheckpointAborted(long checkpointId) throws Exception {}
}
