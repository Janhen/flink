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

package org.apache.flink.api.common.functions;

import org.apache.flink.annotation.Public;
import org.apache.flink.api.common.InvalidProgramException;

/**
 * {@link InvalidProgramException} 的一种特殊情况，表示操作中使用的类型无效或不一致。
 *
 * A special case of the {@link InvalidProgramException}, indicating that the types used in an
 * operation are invalid or inconsistent.
 */
@Public
public class InvalidTypesException extends InvalidProgramException {

    private static final long serialVersionUID = 1L;

    /** Creates a new exception with no message. */
    public InvalidTypesException() {
        super();
    }

    /**
     * Creates a new exception with the given message.
     *
     * @param message The exception message.
     */
    public InvalidTypesException(String message) {
        super(message);
    }

    public InvalidTypesException(String message, Throwable e) {
        super(message, e);
    }
}
