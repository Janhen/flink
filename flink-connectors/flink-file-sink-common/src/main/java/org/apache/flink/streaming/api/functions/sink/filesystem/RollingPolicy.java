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

package org.apache.flink.streaming.api.functions.sink.filesystem;

import org.apache.flink.annotation.PublicEvolving;

import java.io.IOException;
import java.io.Serializable;

/**
 * 基于此策略，{@code Filesystem Sink}中的{@code Bucket}滚动其当前打开的部分文件并打开一个新文件。
 *
 * J: 检查点进行滚动、根据事件情况进行滚动
 *
 * The policy based on which a {@code Bucket} in the {@code Filesystem Sink} rolls its currently
 * open part file and opens a new one.
 */
@PublicEvolving
public interface RollingPolicy<IN, BucketID> extends Serializable {

    /**
     * 确定桶的正在处理的部件文件是否应该在每个检查点滚动。
     *
     * Determines if the in-progress part file for a bucket should roll on every checkpoint.
     *
     * @param partFileState the state of the currently open part file of the bucket.
     * @return {@code True} if the part file should roll, {@link false} otherwise.
     */
    boolean shouldRollOnCheckpoint(final PartFileInfo<BucketID> partFileState) throws IOException;

    /**
     * 根据当前状态(例如，它的大小)确定正在进行的零件文件是否应该滚动。
     *
     * J:事件的字节大小、事件的个数?
     *
     * Determines if the in-progress part file for a bucket should roll based on its current state,
     * e.g. its size.
     *
     * @param element the element being processed.
     * @param partFileState the state of the currently open part file of the bucket.
     * @return {@code True} if the part file should roll, {@link false} otherwise.
     */
    boolean shouldRollOnEvent(final PartFileInfo<BucketID> partFileState, IN element)
            throws IOException;

    /**
     * 根据时间条件确定桶的正在进行的零件文件是否应该滚动。
     *
     * J: 对应根据时间间隔来写入
     *
     * Determines if the in-progress part file for a bucket should roll based on a time condition.
     *
     * @param partFileState the state of the currently open part file of the bucket.
     * @param currentTime the current processing time.
     * @return {@code True} if the part file should roll, {@link false} otherwise.
     */
    boolean shouldRollOnProcessingTime(
            final PartFileInfo<BucketID> partFileState, final long currentTime) throws IOException;
}
