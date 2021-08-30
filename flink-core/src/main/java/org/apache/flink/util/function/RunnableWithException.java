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

package org.apache.flink.util.function;

import org.apache.flink.annotation.Public;

/**
 * 与 {@link Runnable} 类似，此接口用于捕获要执行的代码块。与 {@code Runnable} 相比，此接口允许抛出已检查的异常。
 *
 * Similar to a {@link Runnable}, this interface is used to capture a block of code to be executed.
 * In contrast to {@code Runnable}, this interface allows throwing checked exceptions.
 */
@Public
@FunctionalInterface
public interface RunnableWithException extends ThrowingRunnable<Exception> {

    /**
     * 工作方法。
     *
     * The work method.
     *
     * @throws Exception Exceptions may be thrown.
     */
    @Override
    void run() throws Exception;
}
