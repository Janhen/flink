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

package org.apache.flink.streaming.api.operators.util;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.core.io.SimpleVersionedSerialization;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.util.FlinkRuntimeException;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * 一个{@link ListState}使用{@link SimpleVersionedSerializer}而不是{@link TypeSerializer}。
 *
 * <p>状态封装了一个类型为{@code byte[]}的{@link ListState}，这意味着它内部只保留字节并惰性地将它们反序列化
 * 为对象。与使用{@code TypeSerializer}的{@code ListState}状态相比，这有两个主要含义:
 *
 * 此状态不参与<i>>状态迁移<i>。字节永远不会被转换，不同的状态版本由版本化的序列化器惰性地解析。
 *
 * 这种状态通常比直接使用{@code TypeSerializer}的状态要慢，因为会有额外的拷贝到字节数组和额外的版本编码。
 *
 * A {@link ListState} that uses a {@link SimpleVersionedSerializer} instead of a {@link
 * TypeSerializer}.
 *
 * <p>The state wraps a {@link ListState} of type {@code byte[]}, meaning it internally keeps only
 * bytes and lazily deserializes them into objects. This has two major implications, compared to a
 * {@code ListState} states that uses a {@code TypeSerializer}:
 *
 * <ul>
 *   <li>This state does not participate in <i>>state migration</i>. The bytes are never converted
 *       and different state versions are lazily resolved by the versioned serializer.
 *   <li>This state is generally slower than states that directly use the {@code TypeSerializer},
 *       because of extra copies into byte arrays and extra version encodings.
 * </ul>
 *
 * @param <T> The type of the objects stored in the state.
 */
public class SimpleVersionedListState<T> implements ListState<T> {
    // 原始的 byte[]
    private final ListState<byte[]> rawState;

    private final SimpleVersionedSerializer<T> serializer;

    /**
     * Creates a new SimpleVersionedListState that reads and writes bytes from the given raw
     * ListState with the given serializer.
     */
    public SimpleVersionedListState(
            ListState<byte[]> rawState, SimpleVersionedSerializer<T> serializer) {
        this.rawState = checkNotNull(rawState);
        this.serializer = checkNotNull(serializer);
    }

    @Override
    public void update(@Nullable List<T> values) throws Exception {
        // J: 整个 list 序列化
        rawState.update(serializeAll(values));
    }

    @Override
    public void addAll(@Nullable List<T> values) throws Exception {
        rawState.addAll(serializeAll(values));
    }

    @Override
    public Iterable<T> get() throws Exception {
        final Iterable<byte[]> rawIterable = rawState.get();
        final SimpleVersionedSerializer<T> serializer = this.serializer;

        return () -> new DeserializingIterator<>(rawIterable.iterator(), serializer);
    }

    @Override
    public void add(T value) throws Exception {
        // J: 添加进 byte[]
        rawState.add(serialize(value));
    }

    @Override
    public void clear() {
        rawState.clear();
    }

    // ------------------------------------------------------------------------
    //  utils
    // ------------------------------------------------------------------------

    private byte[] serialize(T value) throws IOException {
        return SimpleVersionedSerialization.writeVersionAndSerialize(serializer, value);
    }

    @Nullable
    private List<byte[]> serializeAll(@Nullable List<T> values) throws IOException {
        if (values == null) {
            return null;
        }

        final ArrayList<byte[]> rawValues = new ArrayList<>(values.size());
        for (T value : values) {
            // J: 每个元素使用 SimpleVersionedSerialization 进行序列化成 byte[]
            rawValues.add(serialize(value));
        }
        return rawValues;
    }

    // J: 反序列化迭代器
    private static final class DeserializingIterator<T> implements Iterator<T> {

        private final Iterator<byte[]> rawIterator;
        private final SimpleVersionedSerializer<T> serializer;

        private DeserializingIterator(
                Iterator<byte[]> rawIterator, SimpleVersionedSerializer<T> serializer) {
            this.rawIterator = rawIterator;
            this.serializer = serializer;
        }

        @Override
        public boolean hasNext() {
            return rawIterator.hasNext();
        }

        @Override
        public T next() {
            final byte[] bytes = rawIterator.next();
            try {
                return SimpleVersionedSerialization.readVersionAndDeSerialize(serializer, bytes);
            } catch (IOException e) {
                throw new FlinkRuntimeException("Failed to deserialize value", e);
            }
        }
    }
}
