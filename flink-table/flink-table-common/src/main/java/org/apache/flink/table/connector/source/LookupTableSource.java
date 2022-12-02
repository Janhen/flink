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
import org.apache.flink.types.RowKind;

import java.io.Serializable;

/**
 * {@link DynamicTableSource} 在运行时通过一个或多个键查找外部存储系统的行。
 *
 * <p>与 {@link ScanTableSource} 相比，source 不需要读取整个表，并且可以在必要时从外部表(可能是不断变化的)惰性地获取单个值。
 *
 * <p>注意:与 {@link ScanTableSource} 相比，{@link LookupTableSource} 目前只支持发出只插入的更改(参见
 *   {@link RowKind})。不支持进一步的能力。
 *
 * <p>在最后一步中，规划器将调用 {@link #getLookupRuntimeProvider(LookupContext)} 来获取运行时实现的提供者。
 *   执行查找所需的关键字段是从规划器的查询派生出来的，将在给定的 {@link LookupContext#getKeys()} 中提供。这些
 *   关键字段的值在运行时传递。
 *
 * A {@link DynamicTableSource} that looks up rows of an external storage system by one or more keys
 * during runtime.
 *
 * <p>Compared to {@link ScanTableSource}, the source does not have to read the entire table and can
 * lazily fetch individual values from a (possibly continuously changing) external table when
 * necessary.
 *
 * <p>Note: Compared to {@link ScanTableSource}, a {@link LookupTableSource} does only support
 * emitting insert-only changes currently (see also {@link RowKind}). Further abilities are not
 * supported.
 *
 * <p>In the last step, the planner will call {@link #getLookupRuntimeProvider(LookupContext)} for
 * obtaining a provider of runtime implementation. The key fields that are required to perform a
 * lookup are derived from a query by the planner and will be provided in the given {@link
 * LookupContext#getKeys()}. The values for those key fields are passed during runtime.
 */
@PublicEvolving
public interface LookupTableSource extends DynamicTableSource {

    /**
     * 返回用于读取数据的运行时实现的提供程序。
     *
     * Returns a provider of runtime implementation for reading the data.
     *
     * <p>There exist different interfaces for runtime implementation which is why {@link
     * LookupRuntimeProvider} serves as the base interface.
     *
     * <p>Independent of the provider interface, a source implementation can work on either
     * arbitrary objects or internal data structures (see {@link org.apache.flink.table.data} for
     * more information).
     *
     * <p>The given {@link LookupContext} offers utilities by the planner for creating runtime
     * implementation with minimal dependencies to internal data structures.
     *
     * @see TableFunctionProvider
     * @see AsyncTableFunctionProvider
     */
    LookupRuntimeProvider getLookupRuntimeProvider(LookupContext context);

    // --------------------------------------------------------------------------------------------
    // Helper interfaces
    // --------------------------------------------------------------------------------------------

    /**
     * 通过 {@link LookupRuntimeProvider} 创建运行时实现的上下文。
     *
     * Context for creating runtime implementation via a {@link LookupRuntimeProvider}.
     *
     * <p>It offers utilities by the planner for creating runtime implementation with minimal
     * dependencies to internal data structures.
     *
     * <p>Methods should be called in {@link #getLookupRuntimeProvider(LookupContext)}. Returned
     * instances that are {@link Serializable} can be directly passed into the runtime
     * implementation class.
     */
    interface LookupContext extends DynamicTableSource.Context {

        /**
         * 返回应在查找期间使用的键索引路径数组。索引是基于0的，并支持(可能嵌套)结构中的复合键。
         *
         * Returns an array of key index paths that should be used during the lookup. The indices
         * are 0-based and support composite keys within (possibly nested) structures.
         *
         * <p>For example, given a table with data type {@code ROW < i INT, s STRING, r ROW < i2
         * INT, s2 STRING > >}, this method would return {@code [[0], [2, 1]]} when {@code i} and
         * {@code s2} are used for performing a lookup.
         *
         * @return array of key index paths
         */
        int[][] getKeys();
    }

    /**
     * 为读取数据提供实际的运行时实现。
     *
     * <p>运行时实现有不同的接口，这就是为什么{@link LookupRuntimeProvider}作为基接口。
     *
     * Provides actual runtime implementation for reading the data.
     *
     * <p>There exist different interfaces for runtime implementation which is why {@link
     * LookupRuntimeProvider} serves as the base interface.
     *
     * @see TableFunctionProvider
     * @see AsyncTableFunctionProvider
     */
    interface LookupRuntimeProvider {
        // marker interface
    }
}
