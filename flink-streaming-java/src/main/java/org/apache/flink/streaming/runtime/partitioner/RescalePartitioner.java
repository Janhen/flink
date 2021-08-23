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
import org.apache.flink.runtime.jobgraph.DistributionPattern;
import org.apache.flink.runtime.plugable.SerializationDelegate;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

/**
 * 通过循环通过输出通道来平均分配数据的分区器。它只分布到下游节点的子集，因为
 * {@link org.apache.flink.streaming.api.graph.StreamingJobGraphGenerator}
 * 当遇到 {@code RescalePartitioner} 时，StreamingJobGraphGenerator
 * 实例化一个 {@link DistributionPattern#POINTWISE} 分布模式。
 *
 * <p>上游操作向其发送元素的下游操作子集取决于上游和下游操作的并行度。例如，如果上游操作的并行度为 2，而下游操作的并行度
 * 为 4，那么一个上游操作将把元素分配给两个下游操作，而另一个上游操作将分配给另外两个下游操作。另一方面，如果下游操作的并
 * 行度为 2，而上游操作的并行度为 4，那么两个上游操作将分配给一个下游操作，而另外两个上游操作将分配给另一个下游操作。
 *
 * <p>如果不同的并行度不是彼此的倍数，那么一个或多个下游操作与上游操作的输入数量将不同。
 *
 * Partitioner that distributes the data equally by cycling through the output channels. This
 * distributes only to a subset of downstream nodes because {@link
 * org.apache.flink.streaming.api.graph.StreamingJobGraphGenerator} instantiates a {@link
 * DistributionPattern#POINTWISE} distribution pattern when encountering {@code RescalePartitioner}.
 *
 * <p>The subset of downstream operations to which the upstream operation sends elements depends on
 * the degree of parallelism of both the upstream and downstream operation. For example, if the
 * upstream operation has parallelism 2 and the downstream operation has parallelism 4, then one
 * upstream operation would distribute elements to two downstream operations while the other
 * upstream operation would distribute to the other two downstream operations. If, on the other
 * hand, the downstream operation has parallelism 2 while the upstream operation has parallelism 4
 * then two upstream operations will distribute to one downstream operation while the other two
 * upstream operations will distribute to the other downstream operations.
 *
 * <p>In cases where the different parallelisms are not multiples of each other one or several
 * downstream operations will have a differing number of inputs from upstream operations.
 *
 * @param <T> Type of the elements in the Stream being rescaled
 */
@Internal
public class RescalePartitioner<T> extends StreamPartitioner<T> {
    private static final long serialVersionUID = 1L;

    private int nextChannelToSendTo = -1;

    @Override
    public int selectChannel(SerializationDelegate<StreamRecord<T>> record) {
        if (++nextChannelToSendTo >= numberOfChannels) {
            nextChannelToSendTo = 0;
        }
        return nextChannelToSendTo;
    }

    public StreamPartitioner<T> copy() {
        return this;
    }

    @Override
    public String toString() {
        return "RESCALE";
    }
}
