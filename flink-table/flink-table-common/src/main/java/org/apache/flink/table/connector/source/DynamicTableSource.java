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
import org.apache.flink.table.connector.source.abilities.SupportsFilterPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsProjectionPushDown;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.types.Row;

import javax.annotation.Nullable;

import java.io.Serializable;

/**
 * 来自外部存储系统的动态表的源。
 *
 * <p>动态表是Flink的Table & SQL API的核心概念，以统一的方式处理有界和无界数据。根据定义，动态表可以随时间变化。
 *
 * <p>当读取动态表时，内容可以被认为是:
 *   <li>一个更新日志(有限或无限)，所有的更改将被连续地消耗，直到更新日志耗尽。更多信息见{@link ScanTableSource}。
 *   <li>一个持续变化的或非常大的外部表，其内容通常不会被完全读取，而是在必要时查询单个值。更多信息请参见
 *     {@link LookupTableSource}。
 *
 * <p>注意:两个接口可以同时实现。计划器根据指定的查询决定它们的使用。
 *
 * <p>上面提到的接口的实例可以被看作工厂，最终产生具体的运行时实现来读取实际数据。
 *
 * <p>根据可选声明的能力，如{@link SupportsProjectionPushDown}或{@link SupportsFilterPushDown}，规划器
 *   可能会对实例应用更改，从而改变生成的运行时实现。
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
 * <p>Depending on the optionally declared abilities such as {@link SupportsProjectionPushDown} or
 * {@link SupportsFilterPushDown}, the planner might apply changes to an instance and thus mutates
 * the produced runtime implementation.
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
     * 基础上下文创建运行时实现通过{@link ScanTableSource.ScanRuntimeProvider}和
     * {@link LookupTableSource.LookupRuntimeProvider}。
     *
     * <p>它通过planner提供实用程序，以创建对内部数据结构依赖最小的运行时实现。
     *
     * <p>方法应该在{@link ScanTableSource#getScanRuntimeProvider(ScanTableSource.ScanContext)}和
     *   {@link LookupTableSource#getLookupRuntimeProvider(LookupTableSource.LookupContext)}中调用。
     *   返回的实例是{@link Serializable}，可以直接传递给运行时实现类。
     *
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
        <T> TypeInformation<T> createTypeInformation(DataType producedDataType);

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
     * 用于在运行时在对象和Flink的内部数据结构之间映射的转换器。
     *
     * <p>根据请求，规划器将提供一个专门的(可能是代码生成的)转换器，该转换器可以传递到运行时实现中。
     *
     * <p>例如，{@link Row}及其字段可以转换为{@link RowData}，或者(可能嵌套的)POJO可以转换为结构化类型的内部表示。
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
