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

package org.apache.flink.streaming.api.datastream;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.Utils;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.api.functions.async.AsyncFunction;
import org.apache.flink.streaming.api.operators.async.AsyncWaitOperator;
import org.apache.flink.streaming.api.operators.async.AsyncWaitOperatorFactory;

import java.util.concurrent.TimeUnit;

/**
 * 将 {@link AsyncFunction} 应用于数据流的helper类。
 *
 * A helper class to apply {@link AsyncFunction} to a data stream.
 *
 * <pre>{@code
 * DataStream<String> input = ...
 * AsyncFunction<String, Tuple<String, String>> asyncFunc = ...
 *
 * AsyncDataStream.orderedWait(input, asyncFunc, timeout, TimeUnit.MILLISECONDS, 100);
 * }</pre>
 */
@PublicEvolving
public class AsyncDataStream {

    /** Output mode for asynchronous operations. */
    // 异步操作的输出模式。
    public enum OutputMode {
        ORDERED,
        UNORDERED
    }

    private static final int DEFAULT_QUEUE_CAPACITY = 100;

    /**
     * 添加一个AsyncWaitOperator。
     *
     * Add an AsyncWaitOperator.
     *
     * @param in The {@link DataStream} where the {@link AsyncWaitOperator} will be added.
     * @param func {@link AsyncFunction} wrapped inside {@link AsyncWaitOperator}.
     * @param timeout for the asynchronous operation to complete
     * @param bufSize The max number of inputs the {@link AsyncWaitOperator} can hold inside.
     * @param mode Processing mode for {@link AsyncWaitOperator}.
     * @param <IN> Input type.
     * @param <OUT> Output type.
     * @return A new {@link SingleOutputStreamOperator}
     */
    private static <IN, OUT> SingleOutputStreamOperator<OUT> addOperator(
            DataStream<IN> in,
            AsyncFunction<IN, OUT> func,
            long timeout,
            int bufSize,
            OutputMode mode) {

        TypeInformation<OUT> outTypeInfo =
                TypeExtractor.getUnaryOperatorReturnType(
                        func,
                        AsyncFunction.class,
                        0,
                        1,
                        new int[] {1, 0},
                        in.getType(),
                        Utils.getCallLocationName(),
                        true);

        // create transform
        AsyncWaitOperatorFactory<IN, OUT> operatorFactory =
                new AsyncWaitOperatorFactory<>(
                        in.getExecutionEnvironment().clean(func), timeout, bufSize, mode);

        return in.transform("async wait operator", outTypeInfo, operatorFactory);
    }

    /**
     * 添加一个 AsyncWaitOperator。输出流记录的顺序可以重新排序。
     *
     * J: 设置 timeout, capacity 在满了的情况下，会反压上游节点
     *
     * Add an AsyncWaitOperator. The order of output stream records may be reordered.
     *
     * @param in Input {@link DataStream}
     * @param func {@link AsyncFunction}
     * @param timeout for the asynchronous operation to complete
     * @param timeUnit of the given timeout
     * @param capacity The max number of async i/o operation that can be triggered
     * @param <IN> Type of input record
     * @param <OUT> Type of output record
     * @return A new {@link SingleOutputStreamOperator}.
     */
    public static <IN, OUT> SingleOutputStreamOperator<OUT> unorderedWait(
            DataStream<IN> in,
            AsyncFunction<IN, OUT> func,
            long timeout,
            TimeUnit timeUnit,
            int capacity) {
        return addOperator(in, func, timeUnit.toMillis(timeout), capacity, OutputMode.UNORDERED);
    }

    /**
     * 添加一个AsyncWaitOperator。输出流记录的顺序可以重新排序。
     *
     * Add an AsyncWaitOperator. The order of output stream records may be reordered.
     *
     * @param in Input {@link DataStream}
     * @param func {@link AsyncFunction}
     * @param timeout for the asynchronous operation to complete
     * @param timeUnit of the given timeout
     * @param <IN> Type of input record
     * @param <OUT> Type of output record
     * @return A new {@link SingleOutputStreamOperator}.
     */
    public static <IN, OUT> SingleOutputStreamOperator<OUT> unorderedWait(
            DataStream<IN> in, AsyncFunction<IN, OUT> func, long timeout, TimeUnit timeUnit) {
        return addOperator(
                in, func, timeUnit.toMillis(timeout), DEFAULT_QUEUE_CAPACITY, OutputMode.UNORDERED);
    }

    /**
     * 添加一个AsyncWaitOperator。处理输入记录的顺序保证与输入记录相同。
     *
     * Add an AsyncWaitOperator. The order to process input records is guaranteed to be the same as
     * input ones.
     *
     * @param in Input {@link DataStream}
     * @param func {@link AsyncFunction}
     * @param timeout for the asynchronous operation to complete
     * @param timeUnit of the given timeout
     * @param capacity The max number of async i/o operation that can be triggered
     * @param <IN> Type of input record
     * @param <OUT> Type of output record
     * @return A new {@link SingleOutputStreamOperator}.
     */
    public static <IN, OUT> SingleOutputStreamOperator<OUT> orderedWait(
            DataStream<IN> in,
            AsyncFunction<IN, OUT> func,
            long timeout,
            TimeUnit timeUnit,
            int capacity) {
        return addOperator(in, func, timeUnit.toMillis(timeout), capacity, OutputMode.ORDERED);
    }

    /**
     * 添加一个AsyncWaitOperator。处理输入记录的顺序保证与输入记录相同。
     *
     * Add an AsyncWaitOperator. The order to process input records is guaranteed to be the same as
     * input ones.
     *
     * @param in Input {@link DataStream}
     * @param func {@link AsyncFunction}
     * @param timeout for the asynchronous operation to complete
     * @param timeUnit of the given timeout
     * @param <IN> Type of input record
     * @param <OUT> Type of output record
     * @return A new {@link SingleOutputStreamOperator}.
     */
    public static <IN, OUT> SingleOutputStreamOperator<OUT> orderedWait(
            DataStream<IN> in, AsyncFunction<IN, OUT> func, long timeout, TimeUnit timeUnit) {
        return addOperator(
                in, func, timeUnit.toMillis(timeout), DEFAULT_QUEUE_CAPACITY, OutputMode.ORDERED);
    }
}
