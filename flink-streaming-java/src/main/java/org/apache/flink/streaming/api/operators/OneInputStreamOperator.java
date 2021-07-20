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

package org.apache.flink.streaming.api.operators;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.LatencyMarker;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

/**
 * 一个输入的流操作符的接口。使用{@link org.apache.flink.streaming.api.operators.AbstractStreamOperator}
 * 如果想要实现自定义操作符，可以将AbstractStreamOperator作为基类。
 *
 * Interface for stream operators with one input. Use {@link
 * org.apache.flink.streaming.api.operators.AbstractStreamOperator} as a base class if you want to
 * implement a custom operator.
 *
 * @param <IN> The input type of the operator
 * @param <OUT> The output type of the operator
 */
@PublicEvolving
public interface OneInputStreamOperator<IN, OUT> extends StreamOperator<OUT> {

    /**
     * 处理到达该操作符的一个元素。此方法保证不会与该操作符的其他方法同时调用
     *
     * Processes one element that arrived at this operator. This method is guaranteed to not be
     * called concurrently with other methods of the operator.
     */
    void processElement(StreamRecord<IN> element) throws Exception;

    /**
     * 处理{@link Watermark}。此方法保证不会与该 operator 的其他方法同时调用
     *
     * Processes a {@link Watermark}. This method is guaranteed to not be called concurrently with
     * other methods of the operator.
     *
     * @see org.apache.flink.streaming.api.watermark.Watermark
     */
    void processWatermark(Watermark mark) throws Exception;

    void processLatencyMarker(LatencyMarker latencyMarker) throws Exception;
}
