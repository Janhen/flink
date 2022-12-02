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

package org.apache.flink.table.catalog;

import org.apache.flink.annotation.Internal;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.table.factories.Factory;

/** A collection of {@link ConfigOption} which are consistently used in multiple catalogs. */
// 在多个目录中一致使用的 {@link ConfigOption} 集合。
@Internal
public class CommonCatalogOptions {

    /**
     * 用于指定默认数据库 {@link ConfigOption} 的密钥。
     *
     * <p>请注意，我们无法在此处公开 {@link ConfigOption} 的实际实例，因为目录之间的默认值不同。
     *
     * Key used for specifying a default database {@link ConfigOption}.
     *
     * <p>Note that we cannot expose an actual instance of {@link ConfigOption} here as the default
     * values differ between catalogs.
     */
    public static final String DEFAULT_DATABASE_KEY = "default-database";

    /**
     * {@link ConfigOption} which is used during catalog discovery to match it against {@link
     * Factory#factoryIdentifier()}.
     */
    public static final ConfigOption<String> CATALOG_TYPE =
            ConfigOptions.key("type").stringType().noDefaultValue();
}
