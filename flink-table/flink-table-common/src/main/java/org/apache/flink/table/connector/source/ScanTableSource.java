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

package org.apache.flink.table.connector.source;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.abilities.SupportsAggregatePushDown;
import org.apache.flink.table.connector.source.abilities.SupportsFilterPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsPartitionPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsProjectionPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsReadingMetadata;
import org.apache.flink.table.connector.source.abilities.SupportsSourceWatermark;
import org.apache.flink.table.connector.source.abilities.SupportsWatermarkPushDown;
import org.apache.flink.types.RowKind;

import java.io.Serializable;

/**
 * 一个 {@link DynamicTableSource}，在运行时扫描外部存储系统中的所有行。
 *
 * <p>扫描行不需要只包含插入，也可以包含更新和删除。因此，可以使用表源读取(有限或无限)更改日志。给定的
 *   {@link ChangelogMode}表示规划器在运行时可以预期的一组更改。
 *
 * <p>对于常规批处理场景，源可以发出一个限定的仅插入行的流。
 *
 * <p>对于更改数据捕获(CDC)场景，源可以发出带有插入、更新和删除行的有界或无界流。参见 {@link RowKind}。
 *
 * ...
 *
 * <p>A {@link ScanTableSource}可以实现以下能力，这些能力可能会在规划过程中改变实例:
 *
 *   <li>{@link SupportsWatermarkPushDown} 支持水印下推
 *   <li>{@link SupportsSourceWatermark} 支持 Source 水印
 *   <li>{@link SupportsFilterPushDown} 支持谓词下推
 *   <li>{@link SupportsAggregatePushDown} 支持聚合下推
 *   <li>{@link SupportsProjectionPushDown} 支持投影下推
 *   <li>{@link SupportsPartitionPushDown} 支持分区下推
 *   <li>{@link SupportsReadingMetadata} 支持读取元数据
 *
 * <p>在最后一步，规划器将调用 {@link #getScanRuntimeProvider(ScanContext)} 来获得运行时实现的 provider。
 *
 * A {@link DynamicTableSource} that scans all rows from an external storage system during runtime.
 *
 * <p>The scanned rows don't have to contain only insertions but can also contain updates and
 * deletions. Thus, the table source can be used to read a (finite or infinite) changelog. The given
 * {@link ChangelogMode} indicates the set of changes that the planner can expect during runtime.
 *
 * <p>For regular batch scenarios, the source can emit a bounded stream of insert-only rows.
 *
 * <p>For regular streaming scenarios, the source can emit an unbounded stream of insert-only rows.
 *
 * <p>For change data capture (CDC) scenarios, the source can emit bounded or unbounded streams with
 * insert, update, and delete rows. See also {@link RowKind}.
 *
 * <p>A {@link ScanTableSource} can implement the following abilities that might mutate an instance
 * during planning:
 *
 * <ul>
 *   <li>{@link SupportsWatermarkPushDown}
 *   <li>{@link SupportsSourceWatermark}
 *   <li>{@link SupportsFilterPushDown}
 *   <li>{@link SupportsAggregatePushDown}
 *   <li>{@link SupportsProjectionPushDown}
 *   <li>{@link SupportsPartitionPushDown}
 *   <li>{@link SupportsReadingMetadata}
 * </ul>
 *
 * <p>In the last step, the planner will call {@link #getScanRuntimeProvider(ScanContext)} for
 * obtaining a provider of runtime implementation.
 */
@PublicEvolving
public interface ScanTableSource extends DynamicTableSource {

    /**
     * 返回计划程序在运行时可预期的更改集。
     *
     * Returns the set of changes that the planner can expect during runtime.
     *
     * @see RowKind
     */
    ChangelogMode getChangelogMode();

    /**
     * 返回用于读取数据的运行时实现的提供程序。
     *
     * Returns a provider of runtime implementation for reading the data.
     *
     * <p>There might exist different interfaces for runtime implementation which is why {@link
     * ScanRuntimeProvider} serves as the base interface. Concrete {@link ScanRuntimeProvider}
     * interfaces might be located in other Flink modules.
     *
     * <p>Independent of the provider interface, the table runtime expects that a source
     * implementation emits internal data structures (see {@link
     * org.apache.flink.table.data.RowData} for more information).
     *
     * <p>The given {@link ScanContext} offers utilities by the planner for creating runtime
     * implementation with minimal dependencies to internal data structures.
     *
     * <p>See {@code org.apache.flink.table.connector.source.SourceFunctionProvider} in {@code
     * flink-table-api-java-bridge}.
     */
    ScanRuntimeProvider getScanRuntimeProvider(ScanContext runtimeProviderContext);

    // --------------------------------------------------------------------------------------------
    // Helper interfaces
    // --------------------------------------------------------------------------------------------

    /**
     * 通过 {@link ScanRuntimeProvider} 创建运行时实现的上下文。
     *
     * Context for creating runtime implementation via a {@link ScanRuntimeProvider}.
     *
     * <p>It offers utilities by the planner for creating runtime implementation with minimal
     * dependencies to internal data structures.
     *
     * <p>Methods should be called in {@link #getScanRuntimeProvider(ScanContext)}. The returned
     * instances are {@link Serializable} and can be directly passed into the runtime implementation
     * class.
     */
    interface ScanContext extends DynamicTableSource.Context {
        // may introduce scan specific methods in the future
    }

    /**
     * 为读取数据提供实际的运行时实现。
     *
     * Provides actual runtime implementation for reading the data.
     *
     * <p>There might exist different interfaces for runtime implementation which is why {@link
     * ScanRuntimeProvider} serves as the base interface. Concrete {@link ScanRuntimeProvider}
     * interfaces might be located in other Flink modules.
     *
     * <p>See {@code org.apache.flink.table.connector.source.SourceFunctionProvider} in {@code
     * flink-table-api-java-bridge}.
     */
    interface ScanRuntimeProvider {

        /** Returns whether the data is bounded or not. */
        boolean isBounded();
    }
}
