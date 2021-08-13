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

package org.apache.flink.streaming.api.operators;

import org.apache.flink.annotation.PublicEvolving;

/** Interface for the multi-input operators that can process EndOfInput event. */
// 可以处理 EndOfInput 事件的多输入操作符的接口。
@PublicEvolving
public interface BoundedMultiInput {

    /**
     * 它被通知没有更多的数据将会由 {@code inputId} 标识的输入到达。的 {@code inputId} 被编号从1开始，而`1`表示
     * 第一输入。
     *
     * It is notified that no more data will arrive on the input identified by the {@code inputId}.
     * The {@code inputId} is numbered starting from 1, and `1` indicates the first input.
     */
    void endInput(int inputId) throws Exception;
}
