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

package org.apache.flink.table.connector.source.abilities;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.Factory;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.utils.LogicalTypeCasts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 支持读取元数据列的 {@link ScanTableSource} 接口。
 *
 * <p>元数据列将额外的列添加到表的架构中。表源负责在生成的行末尾添加请求的元数据列。这包括可能从包含的格式转发元数据列。
 *
 * Interface for {@link ScanTableSource}s that support reading metadata columns.
 *
 * <p>Metadata columns add additional columns to the table's schema. A table source is responsible
 * for adding requested metadata columns at the end of produced rows. This includes potentially
 * forwarding metadata columns from contained formats.
 *
 * <p>Examples in SQL look like:
 *
 * <pre>{@code
 * // reads the column from corresponding metadata key `timestamp`
 * CREATE TABLE t1 (i INT, s STRING, timestamp TIMESTAMP(3) WITH LOCAL TIME ZONE METADATA, d DOUBLE)
 *
 * // reads the column from metadata key `timestamp` and casts to INT
 * CREATE TABLE t2 (i INT, s STRING, myTimestamp INT METADATA FROM 'timestamp', d DOUBLE)
 * }</pre>
 *
 * <p>By default, if this interface is not implemented, the statements above would fail because the
 * table source does not provide a metadata key called `timestamp`.
 *
 * <p>If this interface is implemented, {@link #listReadableMetadata()} lists all metadata keys and
 * their corresponding data types that the source exposes to the planner. The planner will use this
 * information for validation and insertion of explicit casts if necessary.
 *
 * <p>The planner will select required metadata columns (i.e. perform projection push down) and will
 * call {@link #applyReadableMetadata(List, DataType)} with a list of metadata keys. An
 * implementation must ensure that metadata columns are appended at the end of the physical row in
 * the order of the provided list after the apply method has been called.
 *
 * <p>Note: The final output data type emitted by a source changes from the physically produced data
 * type to a data type with metadata columns. {@link #applyReadableMetadata(List, DataType)} will
 * pass the updated data type for convenience. If a source implements {@link
 * SupportsProjectionPushDown}, the projection must be applied to the physical data in the first
 * step. The passed updated data type will have considered information from {@link
 * SupportsProjectionPushDown} already.
 *
 * <p>The metadata column's data type must match with {@link #listReadableMetadata()}. For the
 * examples above, this means that a table source for `t2` returns a TIMESTAMP and not INT. The
 * casting to INT will be performed by the planner in a subsequent operation:
 *
 * <pre>{@code
 * // for t1 and t2
 * ROW < i INT, s STRING, d DOUBLE >                                              // physical output
 * ROW < i INT, s STRING, d DOUBLE, timestamp TIMESTAMP(3) WITH LOCAL TIME ZONE > // final output
 * }</pre>
 */
@PublicEvolving
public interface SupportsReadingMetadata {

    /**
     * 返回可由该表源生成用于读取的元数据键及其对应数据类型的映射。
     *
     * Returns the map of metadata keys and their corresponding data types that can be produced by
     * this table source for reading.
     *
     * <p>The returned map will be used by the planner for validation and insertion of explicit
     * casts (see {@link LogicalTypeCasts#supportsExplicitCast(LogicalType, LogicalType)}) if
     * necessary.
     *
     * <p>The iteration order of the returned map determines the order of metadata keys in the list
     * passed in {@link #applyReadableMetadata(List, DataType)}. Therefore, it might be beneficial
     * to return a {@link LinkedHashMap} if a strict metadata column order is required.
     *
     * <p>If a source forwards metadata from one or more formats, we recommend the following column
     * order for consistency:
     *
     * <pre>{@code
     * KEY FORMAT METADATA COLUMNS + VALUE FORMAT METADATA COLUMNS + SOURCE METADATA COLUMNS
     * }</pre>
     *
     * <p>Metadata key names follow the same pattern as mentioned in {@link Factory}. In case of
     * duplicate names in format and source keys, format keys shall have higher precedence.
     *
     * <p>Regardless of the returned {@link DataType}s, a metadata column is always represented
     * using internal data structures (see {@link RowData}).
     *
     * @see DecodingFormat#listReadableMetadata()
     */
    Map<String, DataType> listReadableMetadata();

    /**
     * 提供生成的{@link RowData}必须作为附加元数据列包含的元数据键的列表。
     *
     * Provides a list of metadata keys that the produced {@link RowData} must contain as appended
     * metadata columns.
     *
     * <p>Note: Use the passed data type instead of {@link TableSchema#toPhysicalRowDataType()} for
     * describing the final output data type when creating {@link TypeInformation}. If the source
     * implements {@link SupportsProjectionPushDown}, the projection is already considered in the
     * given output data type.
     *
     * @param metadataKeys a subset of the keys returned by {@link #listReadableMetadata()}, ordered
     *     by the iteration order of returned map
     * @param producedDataType the final output type of the source
     * @see DecodingFormat#applyReadableMetadata(List)
     */
    void applyReadableMetadata(List<String> metadataKeys, DataType producedDataType);
}
