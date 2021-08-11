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

package org.apache.flink.table.data;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.DistinctType;
import org.apache.flink.table.types.logical.LocalZonedTimestampType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.StructuredType;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.types.RowKind;

import javax.annotation.Nullable;

import java.io.Serializable;

import static org.apache.flink.table.types.logical.utils.LogicalTypeChecks.getFieldCount;
import static org.apache.flink.table.types.logical.utils.LogicalTypeChecks.getPrecision;
import static org.apache.flink.table.types.logical.utils.LogicalTypeChecks.getScale;

/**
 * 内部数据结构的基本接口，表示 {@link RowType} 和其他(可能嵌套的)结构化类型的数据，如表生态系统中的
 * {@link StructuredType}。
 *
 * <p> 运行时通过 Table API 或 SQL 管道传递的所有顶级记录都是这个接口的实例。每个 {@link RowData} 包含一个
 * {@link RowKind}，它表示一行在变更日志中描述的变更类型。{@link RowKind} 只是行的元数据信息，因此不是表模式的一
 * 部分，也就是说，不是一个专用字段。
 *
 * <p> 备注: 该数据结构的所有字段必须是内部数据结构。
 *
 * <p> {@link RowData} 接口有不同的实现，它们是为不同的场景设计的:
 *
 *   <li>面向二进制的实现 {@code BinaryRowData} 由引用 {@link MemorySegment} 来支持，而不是使用 Java 对象来减
 *       少序列化和反序列化开销。
 *   <li>面向对象的实现 {@link GenericRowData} 是由一个Java {@link Object} 数组支持的，该数组构造简单，更新效率高。
 *
 * <p>{@link GenericRowData} 用于公共使用，具有稳定的行为。如果需要内部数据结构，建议使用这个类构造
 *    {@link RowData}的实例。
 *
 * <p> Flink 的 Table API 和 SQL 数据类型到内部数据结构的映射如下表所示:
 *     ...
 *
 * <p> Nullability 总是由容器数据结构处理的。
 *
 * Base interface for an internal data structure representing data of {@link RowType} and other
 * (possibly nested) structured types such as {@link StructuredType} in the table ecosystem.
 *
 * <p>All top-level records that are travelling through Table API or SQL pipelines during runtime
 * are instances of this interface. Each {@link RowData} contains a {@link RowKind} which represents
 * the kind of change that a row describes in a changelog. The {@link RowKind} is just metadata
 * information of row and thus not part of the table's schema, i.e., not a dedicated field.
 *
 * <p>Note: All fields of this data structure must be internal data structures.
 *
 * <p>The {@link RowData} interface has different implementations which are designed for different
 * scenarios:
 *
 * <ul>
 *   <li>The binary-oriented implementation {@code BinaryRowData} is backed by references to {@link
 *       MemorySegment} instead of using Java objects to reduce the serialization/deserialization
 *       overhead.
 *   <li>The object-oriented implementation {@link GenericRowData} is backed by an array of Java
 *       {@link Object} which is easy to construct and efficient to update.
 * </ul>
 *
 * <p>{@link GenericRowData} is intended for public use and has stable behavior. It is recommended
 * to construct instances of {@link RowData} with this class if internal data structures are
 * required.
 *
 * <p>The mappings from Flink's Table API and SQL data types to the internal data structures are
 * listed in the following table:
 *
 * <pre>
 * +--------------------------------+-----------------------------------------+
 * | SQL Data Types                 | Internal Data Structures                |
 * +--------------------------------+-----------------------------------------+
 * | BOOLEAN                        | boolean                                 |
 * +--------------------------------+-----------------------------------------+
 * | CHAR / VARCHAR / STRING        | {@link StringData}                      |
 * +--------------------------------+-----------------------------------------+
 * | BINARY / VARBINARY / BYTES     | byte[]                                  |
 * +--------------------------------+-----------------------------------------+
 * | DECIMAL                        | {@link DecimalData}                     |
 * +--------------------------------+-----------------------------------------+
 * | TINYINT                        | byte                                    |
 * +--------------------------------+-----------------------------------------+
 * | SMALLINT                       | short                                   |
 * +--------------------------------+-----------------------------------------+
 * | INT                            | int                                     |
 * +--------------------------------+-----------------------------------------+
 * | BIGINT                         | long                                    |
 * +--------------------------------+-----------------------------------------+
 * | FLOAT                          | float                                   |
 * +--------------------------------+-----------------------------------------+
 * | DOUBLE                         | double                                  |
 * +--------------------------------+-----------------------------------------+
 * | DATE                           | int (number of days since epoch)        |
 * +--------------------------------+-----------------------------------------+
 * | TIME                           | int (number of milliseconds of the day) |
 * +--------------------------------+-----------------------------------------+
 * | TIMESTAMP                      | {@link TimestampData}                   |
 * +--------------------------------+-----------------------------------------+
 * | TIMESTAMP WITH LOCAL TIME ZONE | {@link TimestampData}                   |
 * +--------------------------------+-----------------------------------------+
 * | INTERVAL YEAR TO MONTH         | int (number of months)                  |
 * +--------------------------------+-----------------------------------------+
 * | INTERVAL DAY TO MONTH          | long (number of milliseconds)           |
 * +--------------------------------+-----------------------------------------+
 * | ROW / structured types         | {@link RowData}                         |
 * +--------------------------------+-----------------------------------------+
 * | ARRAY                          | {@link ArrayData}                       |
 * +--------------------------------+-----------------------------------------+
 * | MAP / MULTISET                 | {@link MapData}                         |
 * +--------------------------------+-----------------------------------------+
 * | RAW                            | {@link RawValueData}                    |
 * +--------------------------------+-----------------------------------------+
 * </pre>
 *
 * <p>Nullability is always handled by the container data structure.
 */
@PublicEvolving
public interface RowData {

    /**
     * 返回该行中的字段数。
     *
     * <p>该数字不包括 {@link RowKind}。它是分开保存的。
     *
     * Returns the number of fields in this row.
     *
     * <p>The number does not include {@link RowKind}. It is kept separately.
     */
    int getArity();

    /**
     * 返回此行在更改日志中描述的更改类型。
     *
     * Returns the kind of change that this row describes in a changelog.
     *
     * @see RowKind
     */
    RowKind getRowKind();

    /**
     * 设置此行在更改日志中描述的更改类型。
     *
     * Sets the kind of change that this row describes in a changelog.
     *
     * @see RowKind
     */
    void setRowKind(RowKind kind);

    // ------------------------------------------------------------------------------------------
    // Read-only accessor methods
    // ------------------------------------------------------------------------------------------

    /** Returns true if the field is null at the given position. */
    // 如果该字段在给定位置为空，则返回 true。
    boolean isNullAt(int pos);

    /** Returns the boolean value at the given position. */
    // 返回给定位置的布尔值。
    boolean getBoolean(int pos);

    /** Returns the byte value at the given position. */
    byte getByte(int pos);

    /** Returns the short value at the given position. */
    short getShort(int pos);

    /** Returns the integer value at the given position. */
    int getInt(int pos);

    /** Returns the long value at the given position. */
    long getLong(int pos);

    /** Returns the float value at the given position. */
    float getFloat(int pos);

    /** Returns the double value at the given position. */
    double getDouble(int pos);

    /** Returns the string value at the given position. */
    StringData getString(int pos);

    /**
     * 返回给定位置的十进制值。
     *
     * <p>需要精度和小数位数来确定十进制值是否以紧凑表示形式存储（请参阅 {@link DecimalData}）。
     *
     * Returns the decimal value at the given position.
     *
     * <p>The precision and scale are required to determine whether the decimal value was stored in
     * a compact representation (see {@link DecimalData}).
     */
    DecimalData getDecimal(int pos, int precision, int scale);

    /**
     * 返回给定位置的时间戳值。
     *
     * <p> 确定时间戳值是否以紧凑表示形式存储需要精度（请参阅 {@link TimestampData}）。
     *
     * Returns the timestamp value at the given position.
     *
     * <p>The precision is required to determine whether the timestamp value was stored in a compact
     * representation (see {@link TimestampData}).
     */
    TimestampData getTimestamp(int pos, int precision);

    /** Returns the raw value at the given position. */
    // 返回给定位置的原始值。
    <T> RawValueData<T> getRawValue(int pos);

    /** Returns the binary value at the given position. */
    byte[] getBinary(int pos);

    /** Returns the array value at the given position. */
    ArrayData getArray(int pos);

    /** Returns the map value at the given position. */
    MapData getMap(int pos);

    /**
     * 返回给定位置的行值。
     *
     * <p> 正确提取行需要字段数。
     *
     * Returns the row value at the given position.
     *
     * <p>The number of fields is required to correctly extract the row.
     */
    RowData getRow(int pos, int numFields);

    // ------------------------------------------------------------------------------------------
    // Access Utilities
    // ------------------------------------------------------------------------------------------

    /**
     * Returns the field object in the internal row data structure at the given position.
     *
     * @param row the internal row data
     * @param pos position of the field to return
     * @param fieldType the field type
     * @return the field object at the specified position in this row data.
     * @deprecated Use {@link #createFieldGetter(LogicalType, int)} for avoiding logical types
     *     during runtime.
     */
    static Object get(RowData row, int pos, LogicalType fieldType) {
        if (row.isNullAt(pos)) {
            return null;
        }
        switch (fieldType.getTypeRoot()) {
            case BOOLEAN:
                return row.getBoolean(pos);
            case TINYINT:
                return row.getByte(pos);
            case SMALLINT:
                return row.getShort(pos);
            case INTEGER:
            case DATE:
            case TIME_WITHOUT_TIME_ZONE:
            case INTERVAL_YEAR_MONTH:
                return row.getInt(pos);
            case BIGINT:
            case INTERVAL_DAY_TIME:
                return row.getLong(pos);
            case TIMESTAMP_WITHOUT_TIME_ZONE:
                TimestampType timestampType = (TimestampType) fieldType;
                return row.getTimestamp(pos, timestampType.getPrecision());
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                LocalZonedTimestampType lzTs = (LocalZonedTimestampType) fieldType;
                return row.getTimestamp(pos, lzTs.getPrecision());
            case FLOAT:
                return row.getFloat(pos);
            case DOUBLE:
                return row.getDouble(pos);
            case CHAR:
            case VARCHAR:
                return row.getString(pos);
            case DECIMAL:
                DecimalType decimalType = (DecimalType) fieldType;
                return row.getDecimal(pos, decimalType.getPrecision(), decimalType.getScale());
            case ARRAY:
                return row.getArray(pos);
            case MAP:
            case MULTISET:
                return row.getMap(pos);
            case ROW:
                return row.getRow(pos, ((RowType) fieldType).getFieldCount());
            case STRUCTURED_TYPE:
                // not the most efficient code but ok for a deprecated method
                return row.getRow(pos, getFieldCount(fieldType));
            case BINARY:
            case VARBINARY:
                return row.getBinary(pos);
            case RAW:
                return row.getRawValue(pos);
            default:
                throw new UnsupportedOperationException("Unsupported type: " + fieldType);
        }
    }

    /**
     * Creates an accessor for getting elements in an internal row data structure at the given
     * position.
     *
     * @param fieldType the element type of the row
     * @param fieldPos the element type of the row
     */
    static FieldGetter createFieldGetter(LogicalType fieldType, int fieldPos) {
        final FieldGetter fieldGetter;
        // ordered by type root definition
        switch (fieldType.getTypeRoot()) {
            case CHAR:
            case VARCHAR:
                fieldGetter = row -> row.getString(fieldPos);
                break;
            case BOOLEAN:
                fieldGetter = row -> row.getBoolean(fieldPos);
                break;
            case BINARY:
            case VARBINARY:
                fieldGetter = row -> row.getBinary(fieldPos);
                break;
            case DECIMAL:
                final int decimalPrecision = getPrecision(fieldType);
                final int decimalScale = getScale(fieldType);
                fieldGetter = row -> row.getDecimal(fieldPos, decimalPrecision, decimalScale);
                break;
            case TINYINT:
                fieldGetter = row -> row.getByte(fieldPos);
                break;
            case SMALLINT:
                fieldGetter = row -> row.getShort(fieldPos);
                break;
            case INTEGER:
            case DATE:
            case TIME_WITHOUT_TIME_ZONE:
            case INTERVAL_YEAR_MONTH:
                fieldGetter = row -> row.getInt(fieldPos);
                break;
            case BIGINT:
            case INTERVAL_DAY_TIME:
                fieldGetter = row -> row.getLong(fieldPos);
                break;
            case FLOAT:
                fieldGetter = row -> row.getFloat(fieldPos);
                break;
            case DOUBLE:
                fieldGetter = row -> row.getDouble(fieldPos);
                break;
            case TIMESTAMP_WITHOUT_TIME_ZONE:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                final int timestampPrecision = getPrecision(fieldType);
                fieldGetter = row -> row.getTimestamp(fieldPos, timestampPrecision);
                break;
            case TIMESTAMP_WITH_TIME_ZONE:
                throw new UnsupportedOperationException();
            case ARRAY:
                fieldGetter = row -> row.getArray(fieldPos);
                break;
            case MULTISET:
            case MAP:
                fieldGetter = row -> row.getMap(fieldPos);
                break;
            case ROW:
            case STRUCTURED_TYPE:
                final int rowFieldCount = getFieldCount(fieldType);
                fieldGetter = row -> row.getRow(fieldPos, rowFieldCount);
                break;
            case DISTINCT_TYPE:
                fieldGetter =
                        createFieldGetter(((DistinctType) fieldType).getSourceType(), fieldPos);
                break;
            case RAW:
                fieldGetter = row -> row.getRawValue(fieldPos);
                break;
            case NULL:
            case SYMBOL:
            case UNRESOLVED:
            default:
                throw new IllegalArgumentException();
        }
        if (!fieldType.isNullable()) {
            return fieldGetter;
        }
        return row -> {
            if (row.isNullAt(fieldPos)) {
                return null;
            }
            return fieldGetter.getFieldOrNull(row);
        };
    }

    /**
     * Accessor for getting the field of a row during runtime.
     *
     * @see #createFieldGetter(LogicalType, int)
     */
    interface FieldGetter extends Serializable {
        @Nullable
        Object getFieldOrNull(RowData row);
    }
}
