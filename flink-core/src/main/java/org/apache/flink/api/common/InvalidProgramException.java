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

package org.apache.flink.api.common;

import org.apache.flink.annotation.Public;

/**
 * 抛出一个异常，以指示组合程序无效。无效程序的例子包括忽略关键参数的操作，或者输入类型和类型签名不匹配的函数。
 *
 * An exception thrown to indicate that the composed program is invalid. Examples of invalid
 * programs are operations where crucial parameters are omitted, or functions where the input type
 * and the type signature do not match.
 */
@Public
public class InvalidProgramException extends RuntimeException {

    private static final long serialVersionUID = 3119881934024032887L;

    /** Creates a new exception with no message. */
    public InvalidProgramException() {
        super();
    }

    /**
     * Creates a new exception with the given message.
     *
     * @param message The exception message.
     */
    public InvalidProgramException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message The exception message.
     * @param e The exception cause.
     */
    public InvalidProgramException(String message, Throwable e) {
        super(message, e);
    }
}
