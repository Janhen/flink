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

package org.apache.flink.streaming.api.functions.co;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.functions.AbstractRichFunction;
import org.apache.flink.streaming.api.TimeDomain;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

/**
 * 处理两个键控流的元素并产生单个输出流的函数。
 * <p>该函数将对输入流中的每个元素调用，并可以产生0个或多个输出元素。与{@link CoFlatMapFunction}相反，
 * 该函数还可以通过提供的{@link Context}查询时间(事件和处理)和设置计时器。当响应 set timer 的触发时，函数可以发出更多的元素。
 * <p>连接流的一个示例用例是对另一个流(stream  {@code stream a})中包含的元素应用一组随着时间变化的规则({@code stream a})。
 * {@code 流A} 中包含的规则可以存储在状态中，并等待新元素到达{@code 流B}。
 * 在 {@code 流B} 上接收到一个新元素时，函数现在可以将之前存储的规则应用到元素上，并直接发出一个结果，或者注册一个计时器，以在将来触发一个动作。
 *
 * A function that processes elements of two keyed streams and produces a single output one.
 *
 * <p>The function will be called for every element in the input streams and can produce zero or
 * more output elements. Contrary to the {@link CoFlatMapFunction}, this function can also query the
 * time (both event and processing) and set timers, through the provided {@link Context}. When
 * reacting to the firing of set timers the function can emit yet more elements.
 *
 * <p>An example use-case for connected streams would be the application of a set of rules that
 * change over time ({@code stream A}) to the elements contained in another stream (stream {@code
 * B}). The rules contained in {@code stream A} can be stored in the state and wait for new elements
 * to arrive on {@code stream B}. Upon reception of a new element on {@code stream B}, the function
 * can now apply the previously stored rules to the element and directly emit a result, and/or
 * register a timer that will trigger an action in the future.
 *
 * @param <K> Type of the key.
 * @param <IN1> Type of the first input.
 * @param <IN2> Type of the second input.
 * @param <OUT> Output type.
 */
@PublicEvolving
public abstract class KeyedCoProcessFunction<K, IN1, IN2, OUT> extends AbstractRichFunction {

    private static final long serialVersionUID = 1L;

    /**
     * This method is called for each element in the first of the connected streams.
     *
     * <p>This function can output zero or more elements using the {@link Collector} parameter and
     * also update internal state or set timers using the {@link Context} parameter.
     *
     * @param value The stream element
     * @param ctx A {@link Context} that allows querying the timestamp of the element, querying the
     *     {@link TimeDomain} of the firing timer and getting a {@link TimerService} for registering
     *     timers and querying the time. The context is only valid during the invocation of this
     *     method, do not store it.
     * @param out The collector to emit resulting elements to
     * @throws Exception The function may throw exceptions which cause the streaming program to fail
     *     and go into recovery.
     */
    public abstract void processElement1(IN1 value, Context ctx, Collector<OUT> out)
            throws Exception;

    /**
     * This method is called for each element in the second of the connected streams.
     *
     * <p>This function can output zero or more elements using the {@link Collector} parameter and
     * also update internal state or set timers using the {@link Context} parameter.
     *
     * @param value The stream element
     * @param ctx A {@link Context} that allows querying the timestamp of the element, querying the
     *     {@link TimeDomain} of the firing timer and getting a {@link TimerService} for registering
     *     timers and querying the time. The context is only valid during the invocation of this
     *     method, do not store it.
     * @param out The collector to emit resulting elements to
     * @throws Exception The function may throw exceptions which cause the streaming program to fail
     *     and go into recovery.
     */
    public abstract void processElement2(IN2 value, Context ctx, Collector<OUT> out)
            throws Exception;

    /**
     * 当使用{@link TimerService}设置的计时器被触发时调用。
     *
     * Called when a timer set using {@link TimerService} fires.
     *
     * @param timestamp The timestamp of the firing timer.
     * @param ctx An {@link OnTimerContext} that allows querying the timestamp of the firing timer,
     *     querying the {@link TimeDomain} of the firing timer and getting a {@link TimerService}
     *     for registering timers and querying the time. The context is only valid during the
     *     invocation of this method, do not store it.
     * @param out The collector for returning result values.
     * @throws Exception This method may throw exceptions. Throwing an exception will cause the
     *     operation to fail and may trigger recovery.
     */
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<OUT> out) throws Exception {}

    /**
     * Information available in an invocation of {@link #processElement1(Object, Context,
     * Collector)}/ {@link #processElement2(Object, Context, Collector)} or {@link #onTimer(long,
     * OnTimerContext, Collector)}.
     */
    public abstract class Context {

        /**
         * 当前正在处理的元素的时间戳或触发计时器的时间戳。<p>这可能是{@code null}，例如，
         * 如果你的程序的时间特征被设置为{@link org.apache.flink.streaming.api.TimeCharacteristic#ProcessingTime}。
         *
         * Timestamp of the element currently being processed or timestamp of a firing timer.
         *
         * <p>This might be {@code null}, for example if the time characteristic of your program is
         * set to {@link org.apache.flink.streaming.api.TimeCharacteristic#ProcessingTime}.
         */
        public abstract Long timestamp();

        /** A {@link TimerService} for querying time and registering timers. */
        public abstract TimerService timerService();

        /**
         * 向由{@link OutputTag}标识的侧输出发出一条记录。
         *
         * Emits a record to the side output identified by the {@link OutputTag}.
         *
         * @param outputTag the {@code OutputTag} that identifies the side output to emit to.
         * @param value The record to emit.
         */
        public abstract <X> void output(OutputTag<X> outputTag, X value);

        /** Get key of the element being processed. */
        public abstract K getCurrentKey();
    }

    /**
     * 调用{@link #onTimer(long, OnTimerContext, Collector)}中可用的信息。
     *
     * Information available in an invocation of {@link #onTimer(long, OnTimerContext, Collector)}.
     */
    public abstract class OnTimerContext extends Context {
        /** The {@link TimeDomain} of the firing timer. */
        public abstract TimeDomain timeDomain();

        /** Get key of the firing timer. */
        @Override
        public abstract K getCurrentKey();
    }
}
