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

package org.apache.flink.table.data;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.data.binary.BinaryStringData;
import org.apache.flink.table.types.logical.CharType;
import org.apache.flink.table.types.logical.VarCharType;

/** An internal data structure representing data of {@link CharType} and {@link VarCharType}. */
// 表示{@link CharType}和{@link VarCharType}数据的内部数据结构。
@PublicEvolving
public interface StringData extends Comparable<StringData> {

    /**
     * 将此{@link StringData}对象转换为UTF-8字节数组。
     *
     * <p>注意:返回的字节数组可能被重用。
     *
     * Converts this {@link StringData} object to a UTF-8 byte array.
     *
     * <p>Note: The returned byte array may be reused.
     */
    byte[] toBytes();

    /** Converts this {@link StringData} object to a {@link String}. */
    // 将这个{@link StringData}对象转换为{@link String}。
    String toString();

    // ------------------------------------------------------------------------------------------
    // Construction Utilities
    // ------------------------------------------------------------------------------------------

    /** Creates an instance of {@link StringData} from the given {@link String}. */
    // 从给定的{@link String}中创建{@link StringData}的实例。
    static StringData fromString(String str) {
        return BinaryStringData.fromString(str);
    }

    /** Creates an instance of {@link StringData} from the given UTF-8 byte array. */
    // 从给定的UTF-8字节数组创建{@link StringData}的实例。
    static StringData fromBytes(byte[] bytes) {
        return BinaryStringData.fromBytes(bytes);
    }

    /**
     * 从给定的UTF-8字节数组中创建一个{@link StringData}的实例，该实例具有偏移量和字节数。
     *
     * Creates an instance of {@link StringData} from the given UTF-8 byte array with offset and
     * number of bytes.
     */
    static StringData fromBytes(byte[] bytes, int offset, int numBytes) {
        return BinaryStringData.fromBytes(bytes, offset, numBytes);
    }
}
