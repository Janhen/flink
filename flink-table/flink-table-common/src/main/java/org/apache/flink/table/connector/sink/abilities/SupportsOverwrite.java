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

package org.apache.flink.table.connector.sink.abilities;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.connector.sink.DynamicTableSink;

/**
 * 允许覆盖 {@link DynamicTableSink} 中的现有数据。
 *
 * <p>默认情况下，如果没有实现该接口，则不能使用 SQL {@code INSERT OVERWRITE} 子句覆盖现有的表或分区。
 *
 * Enables to overwrite existing data in a {@link DynamicTableSink}.
 *
 * <p>By default, if this interface is not implemented, existing tables or partitions cannot be
 * overwritten using e.g. the SQL {@code INSERT OVERWRITE} clause.
 */
@PublicEvolving
public interface SupportsOverwrite {

    /** Provides whether existing data should be overwritten or not. */
    // 提供是否应该覆盖现有数据。
    void applyOverwrite(boolean overwrite);
}
