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

/**
 * 定义运算符的链接方案。当一个操作符被链接到前驱时，这意味着它们运行在同一个线程中。它们成为由多个步骤组成的一个操作符。
 *
 * <p>StreamOperator 使用的默认值是 {@link #HEAD}，这意味着操作符没有链接到它的前任。大多数运算符使用
 *    {@link #ALWAYS} 覆盖它，这意味着它们将尽可能链接到前辈。
 *
 * Defines the chaining scheme for the operator. When an operator is chained to the predecessor, it
 * means that they run in the same thread. They become one operator consisting of multiple steps.
 *
 * <p>The default value used by the StreamOperator is {@link #HEAD}, which means that the operator
 * is not chained to its predecessor. Most operators override this with {@link #ALWAYS}, meaning
 * they will be chained to predecessors whenever possible.
 */
@PublicEvolving
public enum ChainingStrategy {

    /**
     * 只要有可能，operator 就会被急切地链接起来。
     *
     * <p>为了优化性能，允许最大链接和增加 operator 并行性通常是一个很好的做法。
     *
     * Operators will be eagerly chained whenever possible.
     *
     * <p>To optimize performance, it is generally a good practice to allow maximal chaining and
     * increase operator parallelism.
     */
    ALWAYS,

    /** The operator will not be chained to the preceding or succeeding operators. */
    // 运算符不会链接到前面或后面的运算符。
    NEVER,

    /**
     * 运算符不会链接到 predecessor，但后继可以链接到此运算符。
     *
     * The operator will not be chained to the predecessor, but successors may chain to this
     * operator.
     */
    HEAD
}
