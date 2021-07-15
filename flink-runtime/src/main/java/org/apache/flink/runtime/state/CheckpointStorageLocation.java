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

import java.io.IOException;

/**
 * 一个特定检查点的存储位置，提供数据持久性、元数据持久性和生命周期清理方法。
 *
 * <p>CheckpointStorageLocations 通常是通过{@link CheckpointStorage#initializeLocationForCheckpoint(long)}
 * 或{@link CheckpointStorage#initializeLocationForSavepoint(long, String)}创建和初始化的。
 *
 * A storage location for one particular checkpoint, offering data persistent, metadata persistence,
 * and lifecycle/cleanup methods.
 *
 * <p>CheckpointStorageLocations are typically created and initialized via {@link
 * CheckpointStorage#initializeLocationForCheckpoint(long)} or {@link
 * CheckpointStorage#initializeLocationForSavepoint(long, String)}.
 */
public interface CheckpointStorageLocation extends CheckpointStreamFactory {

    /**
     * 创建输出流以将检查点元数据持久化到。
     *
     * Creates the output stream to persist the checkpoint metadata to.
     *
     * @return The output stream to persist the checkpoint metadata to.
     * @throws IOException Thrown, if the stream cannot be opened due to an I/O error.
     */
    CheckpointMetadataOutputStream createMetadataOutputStream() throws IOException;

    /**
     * 在检查点失败的情况下处理检查点位置。此方法处理该位置的所有数据，而不仅仅是调用此方法的特定节点或进程写入的数据。
     *
     * Disposes the checkpoint location in case the checkpoint has failed. This method disposes all
     * the data at that location, not just the data written by the particular node or process that
     * calls this method.
     */
    void disposeOnFailure() throws IOException;

    /**
     * 获取对存储位置的引用。此引用通过检查点 RPC 消息和检查点屏障发送到目标存储位置，格式避免后端特定类。
     *
     * <p>如果没有需要传递的自定义位置信息，这个方法可以简单地返回{@link CheckpointStorageLocationReference#getDefault()}。
     *
     * Gets a reference to the storage location. This reference is sent to the target storage
     * location via checkpoint RPC messages and checkpoint barriers, in a format avoiding
     * backend-specific classes.
     *
     * <p>If there is no custom location information that needs to be communicated, this method can
     * simply return {@link CheckpointStorageLocationReference#getDefault()}.
     */
    CheckpointStorageLocationReference getLocationReference();
}
