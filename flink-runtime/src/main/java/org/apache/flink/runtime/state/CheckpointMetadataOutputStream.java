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

import org.apache.flink.core.fs.FSDataOutputStream;

import java.io.IOException;

/**
 * 检查点元数据的输出流。
 *
 * <p>这个流类似于{@link CheckpointStreamFactory。CheckpointStateOutputStream}，但用于元数据文件而不是数据文件。
 *
 * <p>这个流总是创建一个文件，不管写入的数据量是多少。
 *
 * An output stream for checkpoint metadata.
 *
 * <p>This stream is similar to the {@link CheckpointStreamFactory.CheckpointStateOutputStream}, but
 * for metadata files rather thancdata files.
 *
 * <p>This stream always creates a file, regardless of the amount of data written.
 */
public abstract class CheckpointMetadataOutputStream extends FSDataOutputStream {

    /**
     * 在写入所有元数据并完成检查点位置后关闭流。
     *
     * Closes the stream after all metadata was written and finalizes the checkpoint location.
     *
     * @return An object representing a finalized checkpoint storage location.
     *         表示最终检查点存储位置的对象。
     * @throws IOException Thrown, if the stream cannot be closed or the finalization fails.
     */
    public abstract CompletedCheckpointStorageLocation closeAndFinalizeCheckpoint()
            throws IOException;

    /**
     * 如果之前没有关闭流，则此方法应该关闭流。如果这个方法实际上关闭了流，它应该删除释放流背后的资源，比如流写入的文件。
     *
     * <p>上面的意思是这个方法是“不成功的关闭”，比如当取消写流时，或者当发生异常时。关闭成功案例的流必须通过
     * {@link #closeAndFinalizeCheckpoint()}。
     *
     * This method should close the stream, if has not been closed before. If this method actually
     * closes the stream, it should delete/release the resource behind the stream, such as the file
     * that the stream writes to.
     *
     * <p>The above implies that this method is intended to be the "unsuccessful close", such as
     * when cancelling the stream writing, or when an exception occurs. Closing the stream for the
     * successful case must go through {@link #closeAndFinalizeCheckpoint()}.
     *
     * @throws IOException Thrown, if the stream cannot be closed.
     */
    @Override
    public abstract void close() throws IOException;
}
