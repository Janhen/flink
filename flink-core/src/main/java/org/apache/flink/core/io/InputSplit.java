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

package org.apache.flink.core.io;

import org.apache.flink.annotation.Public;

import java.io.Serializable;

/**
 * 这个接口必须由所有可以分配给输入格式的输入分割来实现。
 *
 * <p>输入分割通过消息以序列化的形式传输，所以它们需要是可序列化的，如{@link java.io.Serializable}所定义的。
 *
 * This interface must be implemented by all kind of input splits that can be assigned to input
 * formats.
 *
 * <p>Input splits are transferred in serialized form via the messages, so they need to be
 * serializable as defined by {@link java.io.Serializable}.
 */
@Public
public interface InputSplit extends Serializable {

    /**
     * 返回此输入分割的数目
     *
     * Returns the number of this input split.
     *
     * @return the number of this input split
     */
    int getSplitNumber();
}
