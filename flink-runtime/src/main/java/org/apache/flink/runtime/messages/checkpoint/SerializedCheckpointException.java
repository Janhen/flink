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

package org.apache.flink.runtime.messages.checkpoint;

import org.apache.flink.runtime.checkpoint.CheckpointException;
import org.apache.flink.runtime.checkpoint.CheckpointFailureReason;
import org.apache.flink.util.SerializedThrowable;

import java.io.Serializable;

/**
 * 序列化的检查点异常，它包装了检查点失败的原因及其序列化的可抛出性。
 *
 * Serialized checkpoint exception which wraps the checkpoint failure reason and its serialized
 * throwable.
 */
public class SerializedCheckpointException implements Serializable {
    private static final long serialVersionUID = 1L;

    private final CheckpointFailureReason checkpointFailureReason;
    private final SerializedThrowable serializedThrowable;

    public SerializedCheckpointException(CheckpointException checkpointException) {
        this.checkpointFailureReason = checkpointException.getCheckpointFailureReason();
        this.serializedThrowable = new SerializedThrowable(checkpointException);
    }

    public CheckpointFailureReason getCheckpointFailureReason() {
        return checkpointFailureReason;
    }

    public SerializedThrowable getSerializedThrowable() {
        return serializedThrowable;
    }

    public CheckpointException unwrap() {
        return new CheckpointException(checkpointFailureReason, serializedThrowable);
    }
}
