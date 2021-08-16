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

package org.apache.flink.streaming.api.transformations;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.dag.Transformation;

import org.apache.flink.shaded.guava18.com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * 这表示拓扑中的一个反馈点。
 *
 * <p>这与迭代在批处理中的工作方式不同。一旦一个反馈点被定义，你可以连接一个或几个 {@code Transformations} 作为一个反
 *    馈边。从反馈点向下的操作将从这个反馈点的输入和反馈边缘接收元素。
 *
 * <p>输入边和反馈边的分割都被保留。它们还可以使用不同的分区策略。然而，这要求反馈 {@code Transformation} 的并行性必
 *    须与输入 {@code Transformation} 的并行性匹配。
 *
 * <p>输入 {@code Transformation} 和反馈 {@code Transformation} 的类型必须匹配。
 *
 * This represents a feedback point in a topology.
 *
 * <p>This is different from how iterations work in batch processing. Once a feedback point is
 * defined you can connect one or several {@code Transformations} as a feedback edges. Operations
 * downstream from the feedback point will receive elements from the input of this feedback point
 * and from the feedback edges.
 *
 * <p>Both the partitioning of the input and the feedback edges is preserved. They can also have
 * differing partitioning strategies. This requires, however, that the parallelism of the feedback
 * {@code Transformations} must match the parallelism of the input {@code Transformation}.
 *
 * <p>The type of the input {@code Transformation} and the feedback {@code Transformation} must
 * match.
 *
 * @param <T> The type of the input elements and the feedback elements.
 */
@Internal
public class FeedbackTransformation<T> extends Transformation<T> {

    private final Transformation<T> input;

    // J: 反馈边
    private final List<Transformation<T>> feedbackEdges;

    private final Long waitTime;

    /**
     * Creates a new {@code FeedbackTransformation} from the given input.
     *
     * @param input The input {@code Transformation}
     * @param waitTime The wait time of the feedback operator. After the time expires the operation
     *     will close and not receive any more feedback elements.
     */
    public FeedbackTransformation(Transformation<T> input, Long waitTime) {
        super("Feedback", input.getOutputType(), input.getParallelism());
        this.input = input;
        this.waitTime = waitTime;
        this.feedbackEdges = Lists.newArrayList();
    }

    /** Returns the input {@code Transformation} of this {@code FeedbackTransformation}. */
    public Transformation<T> getInput() {
        return input;
    }

    /**
     * 增加 feedback edge。{@code Transformation}的并行度必须匹配这个{@code FeedbackTransformation}的输入
     * {@code Transformation}的并行度
     *
     * Adds a feedback edge. The parallelism of the {@code Transformation} must match the
     * parallelism of the input {@code Transformation} of this {@code FeedbackTransformation}
     *
     * @param transform The new feedback {@code Transformation}.
     */
    public void addFeedbackEdge(Transformation<T> transform) {

        if (transform.getParallelism() != this.getParallelism()) {
            throw new UnsupportedOperationException(
                    "Parallelism of the feedback stream must match the parallelism of the original"
                            + " stream. Parallelism of original stream: "
                            + this.getParallelism()
                            + "; parallelism of feedback stream: "
                            + transform.getParallelism()
                            + ". Parallelism can be modified using DataStream#setParallelism() method");
        }

        feedbackEdges.add(transform);
    }

    /** Returns the list of feedback {@code Transformations}. */
    public List<Transformation<T>> getFeedbackEdges() {
        return feedbackEdges;
    }

    /**
     * 返回等待时间。这是反馈操作符持续倾听反馈元素的时间。一旦时间过期，该操作将关闭，并且不再接收其他元素。
     *
     * Returns the wait time. This is the amount of time that the feedback operator keeps listening
     * for feedback elements. Once the time expires the operation will close and will not receive
     * further elements.
     */
    public Long getWaitTime() {
        return waitTime;
    }

    @Override
    public Collection<Transformation<?>> getTransitivePredecessors() {
        List<Transformation<?>> result = Lists.newArrayList();
        result.add(this);
        result.addAll(input.getTransitivePredecessors());
        return result;
    }
}
