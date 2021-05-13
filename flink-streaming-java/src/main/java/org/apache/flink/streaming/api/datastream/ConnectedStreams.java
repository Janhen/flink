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

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.Public;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.Utils;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;
import org.apache.flink.streaming.api.functions.co.CoProcessFunction;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.streaming.api.operators.TwoInputStreamOperator;
import org.apache.flink.streaming.api.operators.co.CoProcessOperator;
import org.apache.flink.streaming.api.operators.co.CoStreamFlatMap;
import org.apache.flink.streaming.api.operators.co.CoStreamMap;
import org.apache.flink.streaming.api.operators.co.KeyedCoProcessOperator;
import org.apache.flink.streaming.api.operators.co.LegacyKeyedCoProcessOperator;
import org.apache.flink.streaming.api.transformations.TwoInputTransformation;

import static java.util.Objects.requireNonNull;

/**
 * ConnectedStreams 表示两个(可能)不同数据类型的连接流。对于一个流上的操作直接影响另一个流上的操作(通常通过流之间的共享状态)的情况，
 * 连接流是有用的
 *
 * 用连接流的一个例子是将随时间变化的规则应用到另一个流上。一个连接的流有规则，另一个流有应用规则的元素。连接流上的操作维护状态中的当前规则集。
 * 它可以接收规则更新并更新状态，也可以接收数据元素并将状态中的规则应用于元素。
 * <p>连接的流在概念上可以看作是任一类型的联合流，它包含第一个流的类型或第二个流的类型。
 *
 * ConnectedStreams represent two connected streams of (possibly) different data types. Connected
 * streams are useful for cases where operations on one stream directly affect the operations on the
 * other stream, usually via shared state between the streams.
 *
 * <p>An example for the use of connected streams would be to apply rules that change over time onto
 * another stream. One of the connected streams has the rules, the other stream the elements to
 * apply the rules to. The operation on the connected stream maintains the current set of rules in
 * the state. It may receive either a rule update and update the state or a data element and apply
 * the rules in the state to the element.
 *
 * <p>The connected stream can be conceptually viewed as a union stream of an Either type, that
 * holds either the first stream's type or the second stream's type.
 *
 * @param <IN1> Type of the first input data steam.
 * @param <IN2> Type of the second input data stream.
 */
@Public
public class ConnectedStreams<IN1, IN2> {

    protected final StreamExecutionEnvironment environment;
    protected final DataStream<IN1> inputStream1;
    protected final DataStream<IN2> inputStream2;

    protected ConnectedStreams(
            StreamExecutionEnvironment env, DataStream<IN1> input1, DataStream<IN2> input2) {
        this.environment = requireNonNull(env);
        this.inputStream1 = requireNonNull(input1);
        this.inputStream2 = requireNonNull(input2);
    }

    public StreamExecutionEnvironment getExecutionEnvironment() {
        return environment;
    }

    /**
     * Returns the first {@link DataStream}.
     *
     * @return The first DataStream.
     */
    public DataStream<IN1> getFirstInput() {
        return inputStream1;
    }

    /**
     * Returns the second {@link DataStream}.
     *
     * @return The second DataStream.
     */
    public DataStream<IN2> getSecondInput() {
        return inputStream2;
    }

    /**
     * Gets the type of the first input.
     *
     * @return The type of the first input
     */
    public TypeInformation<IN1> getType1() {
        return inputStream1.getType();
    }

    /**
     * Gets the type of the second input.
     *
     * @return The type of the second input
     */
    public TypeInformation<IN2> getType2() {
        return inputStream2.getType();
    }

    /**
     * 连接数据流的KeyBy操作。根据keyPosition1和keyPosition2给input1和input2元素分配键
     *
     * KeyBy operation for connected data stream. Assigns keys to the elements of input1 and input2
     * according to keyPosition1 and keyPosition2.
     *
     * @param keyPosition1 The field used to compute the hashcode of the elements in the first input
     *     stream.
     * @param keyPosition2 The field used to compute the hashcode of the elements in the second
     *     input stream.
     * @return The grouped {@link ConnectedStreams}
     */
    public ConnectedStreams<IN1, IN2> keyBy(int keyPosition1, int keyPosition2) {
        return new ConnectedStreams<>(
                this.environment,
                inputStream1.keyBy(keyPosition1),
                inputStream2.keyBy(keyPosition2));
    }

    /**
     * KeyBy operation for connected data stream. Assigns keys to the elements of input1 and input2
     * according to keyPositions1 and keyPositions2.
     *
     * @param keyPositions1 The fields used to group the first input stream.
     * @param keyPositions2 The fields used to group the second input stream.
     * @return The grouped {@link ConnectedStreams}
     */
    public ConnectedStreams<IN1, IN2> keyBy(int[] keyPositions1, int[] keyPositions2) {
        return new ConnectedStreams<>(
                environment, inputStream1.keyBy(keyPositions1), inputStream2.keyBy(keyPositions2));
    }

    /**
     * KeyBy operation for connected data stream using key expressions. Assigns keys to the elements
     * of input1 and input2 according to field1 and field2. A field expression is either the name of
     * a public field or a getter method with parentheses of the {@link DataStream}S underlying
     * type. A dot can be used to drill down into objects, as in {@code "field1.getInnerField2()" }.
     *
     * @param field1 The grouping expression for the first input
     * @param field2 The grouping expression for the second input
     * @return The grouped {@link ConnectedStreams}
     */
    public ConnectedStreams<IN1, IN2> keyBy(String field1, String field2) {
        return new ConnectedStreams<>(
                environment, inputStream1.keyBy(field1), inputStream2.keyBy(field2));
    }

    /**
     * 使用键表达式连接数据流的KeyBy操作。根据fields1和fields2输入t1和input2的元素。字段表达式可以是公共字段的名称，
     * 也可以是带有{@link DataStream}底层类型圆括号的getter方法。点可用于向下钻取对象，如{@code "field1.getInnerField2()"}。
     *
     * KeyBy operation for connected data stream using key expressions. the elements of input1 and
     * input2 according to fields1 and fields2. A field expression is either the name of a public
     * field or a getter method with parentheses of the {@link DataStream}S underlying type. A dot
     * can be used to drill down into objects, as in {@code "field1.getInnerField2()" } .
     *
     * @param fields1 The grouping expressions for the first input
     * @param fields2 The grouping expressions for the second input
     * @return The grouped {@link ConnectedStreams}
     */
    public ConnectedStreams<IN1, IN2> keyBy(String[] fields1, String[] fields2) {
        return new ConnectedStreams<>(
                environment, inputStream1.keyBy(fields1), inputStream2.keyBy(fields2));
    }

    /**
     * 连接数据流的KeyBy操作。使用keySelector1和keySelector2将键分配给input1和input2的元素。
     *
     * KeyBy operation for connected data stream. Assigns keys to the elements of input1 and input2
     * using keySelector1 and keySelector2.
     *
     * @param keySelector1 The {@link KeySelector} used for grouping the first input
     * @param keySelector2 The {@link KeySelector} used for grouping the second input
     * @return The partitioned {@link ConnectedStreams}
     */
    public ConnectedStreams<IN1, IN2> keyBy(
            KeySelector<IN1, ?> keySelector1, KeySelector<IN2, ?> keySelector2) {
        return new ConnectedStreams<>(
                environment, inputStream1.keyBy(keySelector1), inputStream2.keyBy(keySelector2));
    }

    /**
     * KeyBy operation for connected data stream. Assigns keys to the elements of input1 and input2
     * using keySelector1 and keySelector2 with explicit type information for the common key type.
     *
     * @param keySelector1 The {@link KeySelector} used for grouping the first input
     * @param keySelector2 The {@link KeySelector} used for grouping the second input
     * @param keyType The type information of the common key type.
     * @return The partitioned {@link ConnectedStreams}
     */
    public <KEY> ConnectedStreams<IN1, IN2> keyBy(
            KeySelector<IN1, KEY> keySelector1,
            KeySelector<IN2, KEY> keySelector2,
            TypeInformation<KEY> keyType) {
        return new ConnectedStreams<>(
                environment,
                inputStream1.keyBy(keySelector1, keyType),
                inputStream2.keyBy(keySelector2, keyType));
    }

    /**
     * 在{@link ConnectedStreams}上应用CoMap转换，并将输出映射到公共类型。转换对第一个输入的每个元素调用{@link CoMapFunction#map1}，
     * 对第二个输入的每个元素调用{@link CoMapFunction#map2}。每个CoMapFunction调用只返回一个元素。
     *
     * Applies a CoMap transformation on a {@link ConnectedStreams} and maps the output to a common
     * type. The transformation calls a {@link CoMapFunction#map1} for each element of the first
     * input and {@link CoMapFunction#map2} for each element of the second input. Each CoMapFunction
     * call returns exactly one element.
     *
     * @param coMapper The CoMapFunction used to jointly transform the two input DataStreams
     * @return The transformed {@link DataStream}
     */
    public <R> SingleOutputStreamOperator<R> map(CoMapFunction<IN1, IN2, R> coMapper) {

        TypeInformation<R> outTypeInfo =
                TypeExtractor.getBinaryOperatorReturnType(
                        coMapper,
                        CoMapFunction.class,
                        0,
                        1,
                        2,
                        TypeExtractor.NO_INDEX,
                        getType1(),
                        getType2(),
                        Utils.getCallLocationName(),
                        true);

        return map(coMapper, outTypeInfo);
    }

    /**
     * Applies a CoMap transformation on a {@link ConnectedStreams} and maps the output to a common
     * type. The transformation calls a {@link CoMapFunction#map1} for each element of the first
     * input and {@link CoMapFunction#map2} for each element of the second input. Each CoMapFunction
     * call returns exactly one element.
     *
     * @param coMapper The CoMapFunction used to jointly transform the two input DataStreams
     * @param outputType {@link TypeInformation} for the result type of the function.
     * @return The transformed {@link DataStream}
     */
    public <R> SingleOutputStreamOperator<R> map(
            CoMapFunction<IN1, IN2, R> coMapper, TypeInformation<R> outputType) {
        return transform("Co-Map", outputType, new CoStreamMap<>(inputStream1.clean(coMapper)));
    }

    /**
     * Applies a CoFlatMap transformation on a {@link ConnectedStreams} and maps the output to a
     * common type. The transformation calls a {@link CoFlatMapFunction#flatMap1} for each element
     * of the first input and {@link CoFlatMapFunction#flatMap2} for each element of the second
     * input. Each CoFlatMapFunction call returns any number of elements including none.
     *
     * @param coFlatMapper The CoFlatMapFunction used to jointly transform the two input DataStreams
     * @return The transformed {@link DataStream}
     */
    public <R> SingleOutputStreamOperator<R> flatMap(CoFlatMapFunction<IN1, IN2, R> coFlatMapper) {

        TypeInformation<R> outTypeInfo =
                TypeExtractor.getBinaryOperatorReturnType(
                        coFlatMapper,
                        CoFlatMapFunction.class,
                        0,
                        1,
                        2,
                        TypeExtractor.NO_INDEX,
                        getType1(),
                        getType2(),
                        Utils.getCallLocationName(),
                        true);

        return flatMap(coFlatMapper, outTypeInfo);
    }

    /**
     * Applies a CoFlatMap transformation on a {@link ConnectedStreams} and maps the output to a
     * common type. The transformation calls a {@link CoFlatMapFunction#flatMap1} for each element
     * of the first input and {@link CoFlatMapFunction#flatMap2} for each element of the second
     * input. Each CoFlatMapFunction call returns any number of elements including none.
     *
     * @param coFlatMapper The CoFlatMapFunction used to jointly transform the two input DataStreams
     * @param outputType {@link TypeInformation} for the result type of the function.
     * @return The transformed {@link DataStream}
     */
    public <R> SingleOutputStreamOperator<R> flatMap(
            CoFlatMapFunction<IN1, IN2, R> coFlatMapper, TypeInformation<R> outputType) {
        return transform(
                "Co-Flat Map", outputType, new CoStreamFlatMap<>(inputStream1.clean(coFlatMapper)));
    }

    /**
     * 在连接的输入流上应用给定的{@link CoProcessFunction}，从而创建转换后的输出流。
     * <p>该函数将对输入流中的每个元素调用，并可以产生0个或多个输出元素。与{@link #flatMap(CoFlatMapFunction)}
     * 函数相反，该函数还可以查询时间和设置计时器。当响应设置定时器的触发时，函数可以直接发射元素和或注册更多的定时器。
     *
     * Applies the given {@link CoProcessFunction} on the connected input streams, thereby creating
     * a transformed output stream.
     *
     * <p>The function will be called for every element in the input streams and can produce zero or
     * more output elements. Contrary to the {@link #flatMap(CoFlatMapFunction)} function, this
     * function can also query the time and set timers. When reacting to the firing of set timers
     * the function can directly emit elements and/or register yet more timers.
     *
     * @param coProcessFunction The {@link CoProcessFunction} that is called for each element in the
     *     stream.
     * @param <R> The type of elements emitted by the {@code CoProcessFunction}.
     * @return The transformed {@link DataStream}.
     */
    @PublicEvolving
    public <R> SingleOutputStreamOperator<R> process(
            CoProcessFunction<IN1, IN2, R> coProcessFunction) {

        TypeInformation<R> outTypeInfo =
                TypeExtractor.getBinaryOperatorReturnType(
                        coProcessFunction,
                        CoProcessFunction.class,
                        0,
                        1,
                        2,
                        TypeExtractor.NO_INDEX,
                        getType1(),
                        getType2(),
                        Utils.getCallLocationName(),
                        true);

        return process(coProcessFunction, outTypeInfo);
    }

    /**
     * Applies the given {@link CoProcessFunction} on the connected input streams, thereby creating
     * a transformed output stream.
     *
     * <p>The function will be called for every element in the input streams and can produce zero or
     * more output elements. Contrary to the {@link #flatMap(CoFlatMapFunction)} function, this
     * function can also query the time and set timers. When reacting to the firing of set timers
     * the function can directly emit elements and/or register yet more timers.
     *
     * @param coProcessFunction The {@link CoProcessFunction} that is called for each element in the
     *     stream.
     * @param <R> The type of elements emitted by the {@code CoProcessFunction}.
     * @return The transformed {@link DataStream}.
     */
    @Internal
    public <R> SingleOutputStreamOperator<R> process(
            CoProcessFunction<IN1, IN2, R> coProcessFunction, TypeInformation<R> outputType) {

        TwoInputStreamOperator<IN1, IN2, R> operator;

        if ((inputStream1 instanceof KeyedStream) && (inputStream2 instanceof KeyedStream)) {
            operator = new LegacyKeyedCoProcessOperator<>(inputStream1.clean(coProcessFunction));
        } else {
            operator = new CoProcessOperator<>(inputStream1.clean(coProcessFunction));
        }

        return transform("Co-Process", outputType, operator);
    }

    /**
     * Applies the given {@link KeyedCoProcessFunction} on the connected input keyed streams,
     * thereby creating a transformed output stream.
     *
     * <p>The function will be called for every element in the input keyed streams and can produce
     * zero or more output elements. Contrary to the {@link #flatMap(CoFlatMapFunction)} function,
     * this function can also query the time and set timers. When reacting to the firing of set
     * timers the function can directly emit elements and/or register yet more timers.
     *
     * @param keyedCoProcessFunction The {@link KeyedCoProcessFunction} that is called for each
     *     element in the stream.
     * @param <R> The type of elements emitted by the {@code CoProcessFunction}.
     * @return The transformed {@link DataStream}.
     */
    @PublicEvolving
    public <K, R> SingleOutputStreamOperator<R> process(
            KeyedCoProcessFunction<K, IN1, IN2, R> keyedCoProcessFunction) {

        TypeInformation<R> outTypeInfo =
                TypeExtractor.getBinaryOperatorReturnType(
                        keyedCoProcessFunction,
                        KeyedCoProcessFunction.class,
                        1,
                        2,
                        3,
                        TypeExtractor.NO_INDEX,
                        getType1(),
                        getType2(),
                        Utils.getCallLocationName(),
                        true);

        return process(keyedCoProcessFunction, outTypeInfo);
    }

    /**
     * Applies the given {@link KeyedCoProcessFunction} on the connected input streams, thereby
     * creating a transformed output stream.
     *
     * <p>The function will be called for every element in the input streams and can produce zero or
     * more output elements. Contrary to the {@link #flatMap(CoFlatMapFunction)} function, this
     * function can also query the time and set timers. When reacting to the firing of set timers
     * the function can directly emit elements and/or register yet more timers.
     *
     * @param keyedCoProcessFunction The {@link KeyedCoProcessFunction} that is called for each
     *     element in the stream.
     * @param <R> The type of elements emitted by the {@code CoProcessFunction}.
     * @return The transformed {@link DataStream}.
     */
    @Internal
    public <K, R> SingleOutputStreamOperator<R> process(
            KeyedCoProcessFunction<K, IN1, IN2, R> keyedCoProcessFunction,
            TypeInformation<R> outputType) {

        TwoInputStreamOperator<IN1, IN2, R> operator;

        if ((inputStream1 instanceof KeyedStream) && (inputStream2 instanceof KeyedStream)) {
            operator = new KeyedCoProcessOperator<>(inputStream1.clean(keyedCoProcessFunction));
        } else {
            throw new UnsupportedOperationException(
                    "KeyedCoProcessFunction can only be used "
                            + "when both input streams are of type KeyedStream.");
        }

        return transform("Co-Keyed-Process", outputType, operator);
    }

    @PublicEvolving
    public <R> SingleOutputStreamOperator<R> transform(
            String functionName,
            TypeInformation<R> outTypeInfo,
            TwoInputStreamOperator<IN1, IN2, R> operator) {

        // read the output type of the input Transforms to coax out errors about MissingTypeInfo
        inputStream1.getType();
        inputStream2.getType();

        TwoInputTransformation<IN1, IN2, R> transform =
                new TwoInputTransformation<>(
                        inputStream1.getTransformation(),
                        inputStream2.getTransformation(),
                        functionName,
                        operator,
                        outTypeInfo,
                        environment.getParallelism());

        if (inputStream1 instanceof KeyedStream && inputStream2 instanceof KeyedStream) {
            KeyedStream<IN1, ?> keyedInput1 = (KeyedStream<IN1, ?>) inputStream1;
            KeyedStream<IN2, ?> keyedInput2 = (KeyedStream<IN2, ?>) inputStream2;

            TypeInformation<?> keyType1 = keyedInput1.getKeyType();
            TypeInformation<?> keyType2 = keyedInput2.getKeyType();
            if (!(keyType1.canEqual(keyType2) && keyType1.equals(keyType2))) {
                throw new UnsupportedOperationException(
                        "Key types if input KeyedStreams "
                                + "don't match: "
                                + keyType1
                                + " and "
                                + keyType2
                                + ".");
            }

            transform.setStateKeySelectors(
                    keyedInput1.getKeySelector(), keyedInput2.getKeySelector());
            transform.setStateKeyType(keyType1);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        SingleOutputStreamOperator<R> returnStream =
                new SingleOutputStreamOperator(environment, transform);

        getExecutionEnvironment().addOperator(transform);

        return returnStream;
    }
}
