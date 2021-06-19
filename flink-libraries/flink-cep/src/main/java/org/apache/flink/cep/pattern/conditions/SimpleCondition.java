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

package org.apache.flink.cep.pattern.conditions;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.functions.FilterFunction;

/**
 * 一个用户定义的条件，它决定模式中是否应该接受某个元素。接受一个元素也意味着对应的{@link org.apache.flink.cep.nfa.NFA}的状态转换。
 * <p>与{@link IterativeCondition}相反，扩展这个类的条件不能访问模式中先前接受的元素。扩展这个类的条件是简单的{@code filter(…)}函数，它根据手边元素的属性来决定。
 *
 * A user-defined condition that decides if an element should be accepted in the pattern or not.
 * Accepting an element also signals a state transition for the corresponding {@link
 * org.apache.flink.cep.nfa.NFA}.
 *
 * <p>Contrary to the {@link IterativeCondition}, conditions that extend this class do not have
 * access to the previously accepted elements in the pattern. Conditions that extend this class are
 * simple {@code filter(...)} functions that decide based on the properties of the element at hand.
 */
@Internal
public abstract class SimpleCondition<T> extends IterativeCondition<T>
        implements FilterFunction<T> {

    private static final long serialVersionUID = 4942618239408140245L;

    @Override
    public boolean filter(T value, Context<T> ctx) throws Exception {
        return filter(value);
    }
}
