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
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.RuntimeConverter;
import org.apache.flink.table.connector.source.abilities.SupportsComputedColumnPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsFilterPushDown;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.types.Row;

import javax.annotation.Nullable;

import java.io.Serializable;

/**
 * 来自外部存储系统的动态表的源。
 *
 * <p>动态表是 Flink 的 Table & SQL API 的核心概念，用于以统一的方式处理有界和无界数据。根据定义，动态表可以随
 *   时间变化。
 *
 * <p>读取动态表格时，内容可以被认为是：
 *
 * <ul>
 *     <li>一个变更日志（有限的或无限的），所有变更都会持续消耗，直到变更日志用完。有关详细信息，请参阅
 *       {@link ScanTableSource}。
 *     <li>一个不断变化或非常大的外部表，其内容通常不会被完全读取，但在必要时会查询单个值。有关详细信息，请参阅
 *       {@link LookupTableSource}。
 * <ul>
 *
 * Source of a dynamic table from an external storage system.
 *
 * <p>Dynamic tables are the core concept of Flink's Table & SQL API for processing both bounded and
 * unbounded data in a unified fashion. By definition, a dynamic table can change over time.
 *
 * <p>When reading a dynamic table, the content can either be considered as:
 *
 * <ul>
 *   <li>A changelog (finite or infinite) for which all changes are consumed continuously until the
 *       changelog is exhausted. See {@link ScanTableSource} for more information.
 *   <li>A continuously changing or very large external table whose content is usually never read
 *       entirely but queried for individual values when necessary. See {@link LookupTableSource}
 *       for more information.
 * </ul>
 *
 * <p>Note: Both interfaces can be implemented at the same time. The planner decides about their
 * usage depending on the specified query.
 *
 * <p>Instances of the above mentioned interfaces can be seen as factories that eventually produce
 * concrete runtime implementation for reading the actual data.
 *
 * <p>Depending on the optionally declared abilities such as {@link SupportsComputedColumnPushDown}
 * or {@link SupportsFilterPushDown}, the planner might apply changes to an instance and thus
 * mutates the produced runtime implementation.
 */
@PublicEvolving
public interface DynamicTableSource {

    /**
     * 在规划期间创建此实例的副本。副本应该是所有可变成员的深层副本。
     *
     * Creates a copy of this instance during planning. The copy should be a deep copy of all
     * mutable members.
     */
    DynamicTableSource copy();

    /** Returns a string that summarizes this source for printing to a console or log. */
    // 返回一个字符串，该字符串汇总此源以便打印到控制台或日志。
    String asSummaryString();

    // --------------------------------------------------------------------------------------------
    // Helper interfaces
    // --------------------------------------------------------------------------------------------

    /**
     * Base context for creating runtime implementation via a {@link
     * ScanTableSource.ScanRuntimeProvider} and {@link LookupTableSource.LookupRuntimeProvider}.
     *
     * <p>It offers utilities by the planner for creating runtime implementation with minimal
     * dependencies to internal data structures.
     *
     * <p>Methods should be called in {@link
     * ScanTableSource#getScanRuntimeProvider(ScanTableSource.ScanContext)} and {@link
     * LookupTableSource#getLookupRuntimeProvider(LookupTableSource.LookupContext)}. The returned
     * instances are {@link Serializable} and can be directly passed into the runtime implementation
     * class.
     */
    interface Context {

        /**
         * Creates type information describing the internal data structures of the given {@link
         * DataType}.
         *
         * @see TableSchema#toPhysicalRowDataType()
         */
        TypeInformation<?> createTypeInformation(DataType producedDataType);

        /**
         * Creates a converter for mapping between objects specified by the given {@link DataType}
         * and Flink's internal data structures that can be passed into a runtime implementation.
         *
         * <p>For example, a {@link Row} and its fields can be converted into {@link RowData}, or a
         * (possibly nested) POJO can be converted into the internal representation for structured
         * types.
         *
         * @see LogicalType#supportsInputConversion(Class)
         * @see TableSchema#toPhysicalRowDataType()
         */
        DataStructureConverter createDataStructureConverter(DataType producedDataType);
    }

    /**
     * 用于在运行时在对象和 Flink 的内部数据结构之间映射的转换器。
     *
     * Converter for mapping between objects and Flink's internal data structures during runtime.
     *
     * <p>On request, the planner will provide a specialized (possibly code generated) converter
     * that can be passed into a runtime implementation.
     *
     * <p>For example, a {@link Row} and its fields can be converted into {@link RowData}, or a
     * (possibly nested) POJO can be converted into the internal representation for structured
     * types.
     *
     * @see LogicalType#supportsInputConversion(Class)
     */
    interface DataStructureConverter extends RuntimeConverter {

        /** Converts the given object into an internal data structure. */
        @Nullable
        Object toInternal(@Nullable Object externalStructure);
    }
}
