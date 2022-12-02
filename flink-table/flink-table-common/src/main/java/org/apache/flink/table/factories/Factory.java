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

package org.apache.flink.table.factories;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.configuration.ConfigOption;

import java.util.Set;

/**
 * 用于所有类型的工厂的基本接口，这些工厂从 Flink 的 Table & SQL API 中的键值对列表中创建对象实例。
 *
 * <p>工厂由 {@link Class} 和 {@link #factoryIdentifier()}唯一标识。
 *
 * Base interface for all kind of factories that create object instances from a list of key-value
 * pairs in Flink's Table & SQL API.
 *
 * <p>A factory is uniquely identified by {@link Class} and {@link #factoryIdentifier()}.
 *
 * <p>The list of available factories is discovered using Java's Service Provider Interfaces (SPI).
 * Classes that implement this interface can be added to {@code
 * META_INF/services/org.apache.flink.table.factories.Factory} in JAR files.
 *
 * <p>Every factory declares a set of required and optional options. This information will not be
 * used during discovery but is helpful when generating documentation and performing validation. A
 * factory may discover further (nested) factories, the options of the nested factories must not be
 * declared in the sets of this factory.
 *
 * <p>It is the responsibility of each factory to perform validation before returning an instance.
 *
 * <p>For consistency, the following style for key names of {@link ConfigOption} is recommended:
 *
 * <ul>
 *   <li>Try to <b>reuse</b> key names as much as possible. Use other factory implementations as an
 *       example.
 *   <li>Key names should be declared in <b>lower case</b>. Use "-" instead of dots or camel case to
 *       split words.
 *   <li>Key names should be <b>hierarchical</b> where appropriate. Think about how one would define
 *       such a hierarchy in JSON or YAML file (e.g. {@code sink.bulk-flush.max-actions}).
 *   <li>In case of a hierarchy, try not to use the higher level again in the key name (e.g. do
 *       {@code sink.partitioner} instead of {@code sink.sink-partitioner}) to <b>keep the keys
 *       short</b>.
 *   <li>Key names which can be templated, e.g. to refer to a specific column, should be listed
 *       using '#' as the placeholder symbol. For example, use {@code fields.#.min}.
 * </ul>
 */
@PublicEvolving
public interface Factory {

    /**
     * 返回相同工厂接口之间的唯一标识符。
     *
     * <p>为了保持一致性，标识符应声明为一个小写单词（例如 {@code kafka}）。如果不同版本存在多个工厂，则应使用 “-”
     * 附加一个版本（例如 {@code elasticsearch-7}）。
     *
     * Returns a unique identifier among same factory interfaces.
     *
     * <p>For consistency, an identifier should be declared as one lower case word (e.g. {@code
     * kafka}). If multiple factories exist for different versions, a version should be appended
     * using "-" (e.g. {@code elasticsearch-7}).
     */
    String factoryIdentifier();

    /**
     * 除了 {@link #optionalOptions()} 之外，返回此工厂的实现所需的一组 {@link ConfigOption}。
     *
     * <p>有关更多信息，请参阅 {@link Factory} 的文档。
     *
     * Returns a set of {@link ConfigOption} that an implementation of this factory requires in
     * addition to {@link #optionalOptions()}.
     *
     * <p>See the documentation of {@link Factory} for more information.
     */
    Set<ConfigOption<?>> requiredOptions();

    /**
     * 返回一组 {@link ConfigOption}，除了 {@link #requiredOptions()} 之外，该工厂的实现还使用它。
     *
     * Returns a set of {@link ConfigOption} that an implementation of this factory consumes in
     * addition to {@link #requiredOptions()}.
     *
     * <p>See the documentation of {@link Factory} for more information.
     */
    Set<ConfigOption<?>> optionalOptions();
}
