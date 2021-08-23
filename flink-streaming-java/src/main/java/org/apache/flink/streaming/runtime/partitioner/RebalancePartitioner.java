/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.runtime.partitioner;

import org.apache.flink.annotation.Internal;
import org.apache.flink.runtime.plugable.SerializationDelegate;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 通过循环通过输出通道来平均分配数据的分区器。
 *
 * Partitioner that distributes the data equally by cycling through the output channels.
 *
 * @param <T> Type of the elements in the Stream being rebalanced
 */
@Internal
public class RebalancePartitioner<T> extends StreamPartitioner<T> {
    private static final long serialVersionUID = 1L;

    private int nextChannelToSendTo;

    @Override
    public void setup(int numberOfChannels) {
        super.setup(numberOfChannels);

        // 初始的时候选择一个随机数
        nextChannelToSendTo = ThreadLocalRandom.current().nextInt(numberOfChannels);
    }

    @Override
    public int selectChannel(SerializationDelegate<StreamRecord<T>> record) {
        // J: rebalance  逐个向上增加分配
        nextChannelToSendTo = (nextChannelToSendTo + 1) % numberOfChannels;
        return nextChannelToSendTo;
    }

    public StreamPartitioner<T> copy() {
        return this;
    }

    @Override
    public String toString() {
        return "REBALANCE";
    }
}
