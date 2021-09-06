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

package org.apache.flink.table.connector.sink;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.RuntimeConverter;
import org.apache.flink.table.connector.sink.abilities.SupportsOverwrite;
import org.apache.flink.table.connector.sink.abilities.SupportsPartitioning;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;

import javax.annotation.Nullable;

import java.io.Serializable;

/**
 * 将动态表接收到外部存储系统。
 *
 * <p>动态表是 Flink 的 Table & SQL API 的核心概念，用于以统一的方式处理有界和无界数据。根据定义，动态表可以随
 *    时间变化。
 *
 * <p>在写动态表时，内容总是可以被认为是一个变更日志（有限或无限），所有变更都被连续写出，直到变更日志用完。给定的
 *    {@link ChangelogMode} 表示接收器在运行时接受的更改集。
 *
 * <p> 对于常规批处理场景，接收器只能接受仅插入行并写出有界流。
 *
 * <p> 对于常规流场景，接收器只能接受仅插入的行并且可以写出无界流。
 *
 * <p>对于变更数据捕获 (CDC) 方案，接收器可以使用插入、更新和删除行写出有界或无界流。另请参阅 {@link RowKind}。
 *
 * <p>{@link DynamicTableSink} 的实例可以被看作是最终产生用于写入实际数据的具体运行时实现的工厂。
 *
 * <p>根据可选声明的能力，规划器可能会对实例应用更改，从而改变生成的运行时实现。
 *
 * <p>在最后一步，planner 会调用 {@link #getSinkRuntimeProvider(Context)} 获取运行时实现的提供者。
 *
 * Sink of a dynamic table to an external storage system.
 *
 * <p>Dynamic tables are the core concept of Flink's Table & SQL API for processing both bounded and
 * unbounded data in a unified fashion. By definition, a dynamic table can change over time.
 *
 * <p>When writing a dynamic table, the content can always be considered as a changelog (finite or
 * infinite) for which all changes are written out continuously until the changelog is exhausted.
 * The given {@link ChangelogMode} indicates the set of changes that the sink accepts during
 * runtime.
 *
 * <p>For regular batch scenarios, the sink can solely accept insert-only rows and write out bounded
 * streams.
 *
 * <p>For regular streaming scenarios, the sink can solely accept insert-only rows and can write out
 * unbounded streams.
 *
 * <p>For change data capture (CDC) scenarios, the sink can write out bounded or unbounded streams
 * with insert, update, and delete rows. See also {@link RowKind}.
 *
 * <p>Instances of {@link DynamicTableSink} can be seen as factories that eventually produce
 * concrete runtime implementation for writing the actual data.
 *
 * <p>Depending on the optionally declared abilities, the planner might apply changes to an instance
 * and thus mutate the produced runtime implementation.
 *
 * <p>A {@link DynamicTableSink} can implement the following abilities:
 *
 * <ul>
 *   <li>{@link SupportsPartitioning}
 *   <li>{@link SupportsOverwrite}
 * </ul>
 *
 * <p>In the last step, the planner will call {@link #getSinkRuntimeProvider(Context)} for obtaining
 * a provider of runtime implementation.
 */
@PublicEvolving
public interface DynamicTableSink {

    /**
     * 返回接收器在运行时接受的更改集。
     *
     * 规划者可以提出建议，但是水槽有最终的决定它需要什么。如果规划器不支持此模式，它将抛出一个错误。例如，接收器可以
     * 返回它只支持 {@link ChangelogMode#insertOnly()}。
     *
     * Returns the set of changes that the sink accepts during runtime.
     *
     * <p>The planner can make suggestions but the sink has the final decision what it requires. If
     * the planner does not support this mode, it will throw an error. For example, the sink can
     * return that it only supports {@link ChangelogMode#insertOnly()}.
     *
     * @param requestedMode expected set of changes by the current plan
     */
    ChangelogMode getChangelogMode(ChangelogMode requestedMode);

    /**
     * 返回用于写入数据的运行时实现的提供程序。
     *
     * <p>运行时实现可能存在不同的接口，这就是为什么 {@link SinkRuntimeProvider} 作为基础接口。具体的
     *   {@link SinkRuntimeProvider} 接口可能位于其他 Flink 模块中。
     *
     * <p>独立于提供者接口，表运行时期望接收器实现接受内部数据结构(见
     * {@link org.apache.flink.table.data。RowData}取更多信息)。
     *
     * <p>给定的 {@link Context} 由规划器提供实用程序，用于创建对内部数据结构依赖最小的运行时实现。
     *
     * Returns a provider of runtime implementation for writing the data.
     *
     * <p>There might exist different interfaces for runtime implementation which is why {@link
     * SinkRuntimeProvider} serves as the base interface. Concrete {@link SinkRuntimeProvider}
     * interfaces might be located in other Flink modules.
     *
     * <p>Independent of the provider interface, the table runtime expects that a sink
     * implementation accepts internal data structures (see {@link
     * org.apache.flink.table.data.RowData} for more information).
     *
     * <p>The given {@link Context} offers utilities by the planner for creating runtime
     * implementation with minimal dependencies to internal data structures.
     *
     * <p>See {@code org.apache.flink.table.connector.sink.SinkFunctionProvider} in {@code
     * flink-table-api-java-bridge}.
     */
    SinkRuntimeProvider getSinkRuntimeProvider(Context context);

    /**
     * 在规划期间创建此实例的副本。该副本应该是所有可变成员的深层副本。
     *
     * Creates a copy of this instance during planning. The copy should be a deep copy of all
     * mutable members.
     */
    DynamicTableSink copy();

    /** Returns a string that summarizes this sink for printing to a console or log. */
    // 返回总结此接收器以打印到控制台或日志的字符串。
    String asSummaryString();

    // --------------------------------------------------------------------------------------------
    // Helper interfaces
    // --------------------------------------------------------------------------------------------

    /**
     * 通过 {@link SinkRuntimeProvider} 创建运行时实现的上下文。
     *
     * <p>它由 planner 提供实用程序，用于创建对内部数据结构的依赖最小的运行时实现。
     *
     * <p>方法应该在 {@link #getSinkRuntimeProvider(Context)} 中调用。返回的实例为{@link Serializable}，
     *    可以直接传入运行时实现类。
     *
     * Context for creating runtime implementation via a {@link SinkRuntimeProvider}.
     *
     * <p>It offers utilities by the planner for creating runtime implementation with minimal
     * dependencies to internal data structures.
     *
     * <p>Methods should be called in {@link #getSinkRuntimeProvider(Context)}. The returned
     * instances are {@link Serializable} and can be directly passed into the runtime implementation
     * class.
     */
    interface Context {

        /**
         * 返回运行时实现是否可以期望有限的行数。
         *
         * <p>该信息可能来自会话的执行模式和或查询类型。
         *
         * Returns whether a runtime implementation can expect a finite number of rows.
         *
         * <p>This information might be derived from the session's execution mode and/or kind of
         * query.
         */
        boolean isBounded();

        /**
         * 创建描述给定的 {@link DataType} 的内部数据结构的类型信息。
         *
         * Creates type information describing the internal data structures of the given {@link
         * DataType}.
         *
         * @see TableSchema#toPhysicalRowDataType()
         */
        TypeInformation<?> createTypeInformation(DataType consumedDataType);

        /**
         * 创建一个转换器，用于在Flink的内部数据结构和由给定的 {@link DataType} 指定的对象之间进行映射，该对象
         * 可以被传递到运行时实现中。
         *
         * <p>例如，{@link RowData} 及其字段可以转换为 {@link Row}，或者结构化类型的内部表示可以转换回原始的
         * (可能嵌套的) POJO。
         *
         * Creates a converter for mapping between Flink's internal data structures and objects
         * specified by the given {@link DataType} that can be passed into a runtime implementation.
         *
         * <p>For example, {@link RowData} and its fields can be converted into a {@link Row}, or
         * the internal representation for structured types can be converted back into the original
         * (possibly nested) POJO.
         *
         * @see LogicalType#supportsOutputConversion(Class)
         */
        DataStructureConverter createDataStructureConverter(DataType consumedDataType);
    }

    /**
     * 用于在 Flink 的内部数据结构和由给定的 {@link DataType} 指定的对象之间进行映射的转换器，该对象可以传递到
     * 运行时实现中。
     *
     * Converter for mapping between Flink's internal data structures and objects specified by the
     * given {@link DataType} that can be passed into a runtime implementation.
     *
     * <p>For example, {@link RowData} and its fields can be converted into a {@link Row}, or the
     * internal representation for structured types can be converted back into the original
     * (possibly nested) POJO.
     *
     * @see LogicalType#supportsOutputConversion(Class)
     */
    interface DataStructureConverter extends RuntimeConverter {

        /** Converts the given internal structure into an external object. */
        // 将给定的内部结构转换为外部对象。
        @Nullable
        Object toExternal(@Nullable Object internalStructure);
    }

    /**
     * 提供用于写入数据的实际运行时实现。
     *
     * <p>运行时实现可能存在不同的接口，这就是为什么 {@link SinkRuntimeProvider} 作为基础接口。具体的
     *   {@link SinkRuntimeProvider} 接口可能位于其他 Flink 模块中。
     *
     * Provides actual runtime implementation for writing the data.
     *
     * <p>There might exist different interfaces for runtime implementation which is why {@link
     * SinkRuntimeProvider} serves as the base interface. Concrete {@link SinkRuntimeProvider}
     * interfaces might be located in other Flink modules.
     *
     * <p>See {@code org.apache.flink.table.connector.sink.SinkFunctionProvider} in {@code
     * flink-table-api-java-bridge}.
     */
    interface SinkRuntimeProvider {
        // marker interface
    }
}
