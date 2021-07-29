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

package org.apache.flink.types;

import org.apache.flink.annotation.Public;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;

import java.io.IOException;

/** Interface to be implemented by basic types that support to be copied efficiently. */
// 由支持有效复制的基本类型实现的接口。
@Public
public interface CopyableValue<T> extends Value {

    /**
     * 获取数据类型序列化时的长度，以字节为单位。
     *
     * Gets the length of the data type when it is serialized, in bytes.
     *
     * @return The length of the data type, or {@code -1}, if variable length.
     */
    int getBinaryLength();

    /**
     * 在 {@code target} 实例中执行该对象的深度拷贝。
     *
     * Performs a deep copy of this object into the {@code target} instance.
     *
     * @param target Object to copy into.
     */
    void copyTo(T target);

    /**
     * 将此对象复制到新实例中。
     *
     * <p>当存储多个对象时，此方法对于一般的用户定义函数克隆 {@link CopyableValue} 非常有用。使用对象重用时，必须
     * 创建深度副本，并且类型擦除防止调用 new。
     *
     * Performs a deep copy of this object into a new instance.
     *
     * <p>This method is useful for generic user-defined functions to clone a {@link CopyableValue}
     * when storing multiple objects. With object reuse a deep copy must be created and type erasure
     * prevents calling new.
     *
     * @return New object with copied fields.
     */
    T copy();

    /**
     * 将下一个序列化实例从{@code source}复制到{@code target}。
     *
     * <p>该方法等价于在 {@code IOReadableWritable.read(DataInputView)} 之后调用
     * {@code IOReadableWritable.write(DataOutputView)}，但不需要中间反序列化。
     *
     * Copies the next serialized instance from {@code source} to {@code target}.
     *
     * <p>This method is equivalent to calling {@code IOReadableWritable.read(DataInputView)}
     * followed by {@code IOReadableWritable.write(DataOutputView)} but does not require
     * intermediate deserialization.
     *
     * @param source Data source for serialized instance.
     * @param target Data target for serialized instance.
     * @see org.apache.flink.core.io.IOReadableWritable
     */
    void copy(DataInputView source, DataOutputView target) throws IOException;
}
