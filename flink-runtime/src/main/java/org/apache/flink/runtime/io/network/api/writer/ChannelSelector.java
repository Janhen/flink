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

package org.apache.flink.runtime.io.network.api.writer;

import org.apache.flink.core.io.IOReadableWritable;

/**
 * {@link ChannelSelector}决定记录应该写入到哪个逻辑通道。
 *
 * The {@link ChannelSelector} determines to which logical channels a record should be written to.
 *
 * @param <T> the type of record which is sent through the attached output gate
 */
public interface ChannelSelector<T extends IOReadableWritable> {

    /**
     * 用输出通道的数量初始化通道选择器。
     *
     * Initializes the channel selector with the number of output channels.
     *
     * @param numberOfChannels the total number of output channels which are attached to respective
     *     output gate.
     */
    void setup(int numberOfChannels);

    /**
     * 返回逻辑通道索引，给定的记录应写入该索引。为广播通道选择器调用这个方法是非法的，在这种情况下这个方法仍然不能实现
     * (例如抛出{@link UnsupportedOperationException})。
     *
     * Returns the logical channel index, to which the given record should be written. It is illegal
     * to call this method for broadcast channel selectors and this method can remain not
     * implemented in that case (for example by throwing {@link UnsupportedOperationException}).
     *
     * @param record the record to determine the output channels for.
     * @return an integer number which indicates the index of the output channel through which the
     *     record shall be forwarded.
     */
    int selectChannel(T record);

    /**
     * 返回通道选择器是否总是选择所有输出通道。
     *
     * Returns whether the channel selector always selects all the output channels.
     *
     * @return true if the selector is for broadcast mode.
     */
    boolean isBroadcast();
}
