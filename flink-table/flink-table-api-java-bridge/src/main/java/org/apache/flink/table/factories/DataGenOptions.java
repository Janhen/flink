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

import org.apache.flink.annotation.Internal;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

import static org.apache.flink.configuration.ConfigOptions.key;

/** {@link ConfigOption}s for {@link DataGenTableSourceFactory}. */
// {@link DataGenTableSourceFactory}的ConfigOption。
@Internal
public class DataGenOptions {

    public static final Long ROWS_PER_SECOND_DEFAULT_VALUE = 10000L;

    // J: 针对特定的属性限制
    public static final String FIELDS = "fields";
    public static final String KIND = "kind";
    public static final String START = "start";
    public static final String END = "end";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String LENGTH = "length";

    public static final String SEQUENCE = "sequence";
    public static final String RANDOM = "random";

    // 以控制发出速率。
    public static final ConfigOption<Long> ROWS_PER_SECOND =
            key("rows-per-second")
                    .longType()
                    .defaultValue(ROWS_PER_SECOND_DEFAULT_VALUE)
                    .withDescription("Rows per second to control the emit rate.");

    // 要发出的行总数。默认情况下，源是不受限制的
    public static final ConfigOption<Long> NUMBER_OF_ROWS =
            key("number-of-rows")
                    .longType()
                    .noDefaultValue()
                    .withDescription(
                            "Total number of rows to emit. By default, the source is unbounded.");

    /** Placeholder {@link ConfigOption}. Not used for retrieving values. */
    // 占位符{@link ConfigOption}。不用于检索值。
    // 这个'#'字段的生成器。可以是'sequence'或'random'。
    public static final ConfigOption<String> FIELD_KIND =
            ConfigOptions.key(String.format("%s.#.%s", FIELDS, KIND))
                    .stringType()
                    .defaultValue("random")
                    .withDescription("Generator of this '#' field. Can be 'sequence' or 'random'.");

    /** Placeholder {@link ConfigOption}. Not used for retrieving values. */
    // 占位符{@link ConfigOption}。不用于检索值。
    // 为类型为“随机”的字段生成的最小值。字段类型可能的最小值。
    public static final ConfigOption<String> FIELD_MIN =
            ConfigOptions.key(String.format("%s.#.%s", FIELDS, MIN))
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Minimum value to generate for fields of kind 'random'. Minimum value possible for the type of the field.");

    /** Placeholder {@link ConfigOption}. Not used for retrieving values. */
    // 为类型为“随机”的字段生成的最大值。字段类型可能的最大值。
    public static final ConfigOption<String> FIELD_MAX =
            ConfigOptions.key(String.format("%s.#.%s", FIELDS, MAX))
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Maximum value to generate for fields of kind 'random'. Maximum value possible for the type of the field.");

    /** Placeholder {@link ConfigOption}. Not used for retrieving values. */
    public static final ConfigOption<Integer> FIELD_LENGTH =
            ConfigOptions.key(String.format("%s.#.%s", FIELDS, LENGTH))
                    .intType()
                    .defaultValue(100)
                    .withDescription(
                            "Size or length of the collection for generating char/varchar/string/array/map/multiset types.");

    /** Placeholder {@link ConfigOption}. Not used for retrieving values. */
    public static final ConfigOption<String> FIELD_START =
            ConfigOptions.key(String.format("%s.#.%s", FIELDS, START))
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Start value of sequence generator.");

    /** Placeholder {@link ConfigOption}. Not used for retrieving values. */
    // 占位符{@link ConfigOption}。不用于检索值。
    public static final ConfigOption<String> FIELD_END =
            ConfigOptions.key(String.format("%s.#.%s", FIELDS, END))
                    .stringType()
                    .noDefaultValue()
                    .withDescription("End value of sequence generator.");

    private DataGenOptions() {}
}
