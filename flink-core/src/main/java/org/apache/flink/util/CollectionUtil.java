/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.util;

import org.apache.flink.annotation.Internal;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/** Simple utility to work with Java collections. */
@Internal
public final class CollectionUtil {

    /** A safe maximum size for arrays in the JVM. */
    public static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private CollectionUtil() {
        throw new AssertionError();
    }

    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNullOrEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static <T, R> Stream<R> mapWithIndex(
            Collection<T> input, final BiFunction<T, Integer, R> mapper) {
        final AtomicInteger count = new AtomicInteger(0);

        return input.stream().map(element -> mapper.apply(element, count.getAndIncrement()));
    }

    // 将一个集合划分为大约n个桶。
    /** Partition a collection into approximately n buckets. */
    public static <T> Collection<List<T>> partition(Collection<T> elements, int numBuckets) {
        Map<Integer, List<T>> buckets = new HashMap<>(numBuckets);

        int initialCapacity = elements.size() / numBuckets;

        int index = 0;
        for (T element : elements) {
            int bucket = index % numBuckets;
            buckets.computeIfAbsent(bucket, key -> new ArrayList<>(initialCapacity)).add(element);
        }

        return buckets.values();
    }

    public static <I, O> Collection<O> project(Collection<I> collection, Function<I, O> projector) {
        return collection.stream().map(projector).collect(toList());
    }

    /**
     * 在列表中收集Iterable中的元素。如果iterable参数为null，该方法将返回一个空列表。
     *
     * Collects the elements in the Iterable in a List. If the iterable argument is null, this
     * method returns an empty list.
     */
    public static <E> List<E> iterableToList(@Nullable Iterable<E> iterable) {
        if (iterable == null) {
            return Collections.emptyList();
        }

        final ArrayList<E> list = new ArrayList<>();
        iterable.iterator().forEachRemaining(list::add);
        return list;
    }
}
