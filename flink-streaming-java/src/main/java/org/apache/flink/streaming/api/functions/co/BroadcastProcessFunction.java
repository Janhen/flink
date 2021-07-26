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

package org.apache.flink.streaming.api.functions.co;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.util.Collector;

/**
 * 要应用到
 * {@link org.apache.flink.streaming.api.datastream.BroadcastConnectedStream BroadcastConnectedStream}
 * 的函数，该函数连接{@link org.apache.flink.streaming.api.datastream.BroadcastStream BroadcastStream}，
 * 即具有广播状态的流，<b> {@link org.apache.flink.streaming.api.datastream.DataStream DataStream}。
 *
 * <p>带广播状态的流可以通过
 * {@link org.apache.flink.streaming.api.datastream.DataStream#broadcast(MapStateDescriptor[])}
 * stream.broadcast(MapStateDescriptor)}方法创建
 *
 * <p> {@code processElementOnBroadcastSide()}接受一个上下文作为参数，该上下文允许它读写广播状态，而
 * {@code processElement()}只能只读访问广播状态。
 *
 *
 * A function to be applied to a {@link
 * org.apache.flink.streaming.api.datastream.BroadcastConnectedStream BroadcastConnectedStream} that
 * connects {@link org.apache.flink.streaming.api.datastream.BroadcastStream BroadcastStream}, i.e.
 * a stream with broadcast state, with a <b>non-keyed</b> {@link
 * org.apache.flink.streaming.api.datastream.DataStream DataStream}.
 *
 * <p>The stream with the broadcast state can be created using the {@link
 * org.apache.flink.streaming.api.datastream.DataStream#broadcast(MapStateDescriptor[])}
 * stream.broadcast(MapStateDescriptor)} method.
 *
 * <p>The user has to implement two methods:
 *
 * <ol>
 *   <li>the {@link #processBroadcastElement(Object, Context, Collector)} which will be applied to
 *       each element in the broadcast side
 *   <li>and the {@link #processElement(Object, ReadOnlyContext, Collector)} which will be applied
 *       to the non-broadcasted/keyed side.
 * </ol>
 *
 * <p>The {@code processElementOnBroadcastSide()} takes as argument (among others) a context that
 * allows it to read/write to the broadcast state, while the {@code processElement()} has read-only
 * access to the broadcast state.
 *
 * @param <IN1> The input type of the non-broadcast side.
 *              非广播端的输入类型。
 * @param <IN2> The input type of the broadcast side.
 *              广播端的输入类型。
 * @param <OUT> The output type of the operator.
 *              操作符的输出类型。
 */
@PublicEvolving
public abstract class BroadcastProcessFunction<IN1, IN2, OUT> extends BaseBroadcastProcessFunction {

    private static final long serialVersionUID = 8352559162119034453L;

    /**
     * 这个方法在(非广播){@link org.apache.flink.streaming.api.datastream.DataStream 数据流}中的每个元素都被调用。
     *
     * <p>这个函数可以使用{@link Collector}参数输出0个或多个元素，查询当前processingevent时间，也查询和更新本地
     * 键态。最后，它具有<b>只读<b>访问广播状态。上下文仅在调用此方法期间有效，不要存储它。
     *
     * This method is called for each element in the (non-broadcast) {@link
     * org.apache.flink.streaming.api.datastream.DataStream data stream}.
     *
     * <p>This function can output zero or more elements using the {@link Collector} parameter,
     * query the current processing/event time, and also query and update the local keyed state.
     * Finally, it has <b>read-only</b> access to the broadcast state. The context is only valid
     * during the invocation of this method, do not store it.
     *
     * @param value The stream element.
     * @param ctx A {@link ReadOnlyContext} that allows querying the timestamp of the element,
     *     querying the current processing/event time and updating the broadcast state. The context
     *     is only valid during the invocation of this method, do not store it.
     * @param out The collector to emit resulting elements to
     * @throws Exception The function may throw exceptions which cause the streaming program to fail
     *     and go into recovery.
     */
    public abstract void processElement(
            final IN1 value, final ReadOnlyContext ctx, final Collector<OUT> out) throws Exception;

    /**
     * This method is called for each element in the {@link
     * org.apache.flink.streaming.api.datastream.BroadcastStream broadcast stream}.
     *
     * <p>This function can output zero or more elements using the {@link Collector} parameter,
     * query the current processing/event time, and also query and update the internal {@link
     * org.apache.flink.api.common.state.BroadcastState broadcast state}. These can be done through
     * the provided {@link Context}. The context is only valid during the invocation of this method,
     * do not store it.
     *
     * @param value The stream element.
     * @param ctx A {@link Context} that allows querying the timestamp of the element, querying the
     *     current processing/event time and updating the broadcast state. The context is only valid
     *     during the invocation of this method, do not store it.
     * @param out The collector to emit resulting elements to
     * @throws Exception The function may throw exceptions which cause the streaming program to fail
     *     and go into recovery.
     */
    public abstract void processBroadcastElement(
            final IN2 value, final Context ctx, final Collector<OUT> out) throws Exception;

    /**
     * A {@link BaseBroadcastProcessFunction.Context context} available to the broadcast side of a
     * {@link org.apache.flink.streaming.api.datastream.BroadcastConnectedStream}.
     */
    public abstract class Context extends BaseBroadcastProcessFunction.Context {}

    /**
     * 一个{@link BaseBroadcastProcessFunction.Context Context}可用于
     * {@link org.apache.flink.streaming.api.datastream.BroadcastConnectedStream}的非键控侧。
     * BroadcastConnectedStream}(如果有的话)。
     *
     * A {@link BaseBroadcastProcessFunction.Context context} available to the non-keyed side of a
     * {@link org.apache.flink.streaming.api.datastream.BroadcastConnectedStream} (if any).
     */
    public abstract class ReadOnlyContext extends BaseBroadcastProcessFunction.ReadOnlyContext {}
}
