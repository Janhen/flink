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

package org.apache.flink.util.concurrent;

import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * {@link Consumer} 接口的受控扩展，会重新抛出 {@link CompletionException} 中包装的异常。
 *
 * A checked extension of the {@link Consumer} interface which rethrows exceptions wrapped in a
 * {@link CompletionException}.
 *
 * @param <T> type of the first argument
 * @param <E> type of the thrown exception
 */
public interface FutureConsumerWithException<T, E extends Throwable> extends Consumer<T> {

    void acceptWithException(T value) throws E;

    @Override
    default void accept(T value) {
        try {
            acceptWithException(value);
        } catch (Throwable t) {
            throw new CompletionException(t);
        }
    }
}
