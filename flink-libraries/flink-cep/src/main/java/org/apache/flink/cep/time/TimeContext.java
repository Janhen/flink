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

package org.apache.flink.cep.time;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.cep.functions.PatternProcessFunction;

/**
 * 允许访问与时间相关的特征，例如当前处理时间或当前处理元素的时间戳。用于{@link PatternProcessFunction}和
 * {@link org.apache.flink.cep.pattern.conditions.IterativeCondition}
 *
 * Enables access to time related characteristics such as current processing time or timestamp of
 * currently processed element. Used in {@link PatternProcessFunction} and {@link
 * org.apache.flink.cep.pattern.conditions.IterativeCondition}
 */
@PublicEvolving
public interface TimeContext {

    /**
     * Timestamp of the element currently being processed.
     *
     * <p>In case of {@link org.apache.flink.cep.time.TimeBehaviour#ProcessingTime} this means the
     * time when the event entered the cep operator.
     */
    long timestamp();

    /** Returns the current processing time. */
    long currentProcessingTime();
}
