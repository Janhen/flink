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

package org.apache.flink.table.catalog.stats;

import java.util.HashMap;
import java.util.Map;

/** Column statistics value base class. */
// 基本的列统计信息
public abstract class CatalogColumnStatisticsDataBase {
    /** number of null values. */
    private final Long nullCount;

    // 列的配置项
    private final Map<String, String> properties;

    public CatalogColumnStatisticsDataBase(Long nullCount) {
        this(nullCount, new HashMap<>());
    }

    public CatalogColumnStatisticsDataBase(Long nullCount, Map<String, String> properties) {
        this.nullCount = nullCount;
        this.properties = properties;
    }

    public Long getNullCount() {
        return nullCount;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * Create a deep copy of "this" instance.
     *
     * @return a deep copy
     */
    public abstract CatalogColumnStatisticsDataBase copy();
}
