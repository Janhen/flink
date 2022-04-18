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
package org.apache.flink.types;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.java.typeutils.runtime.RowSerializer;
import org.apache.flink.util.Preconditions;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.flink.types.RowUtils.deepEqualsRow;
import static org.apache.flink.types.RowUtils.deepHashCodeRow;

/**
 * 行是固定长度、感知空值的复合类型，用于以确定的字段顺序存储多个值。无论字段的类型如何，每个字段都可以为空。不能自动
 * 推断行字段的类型;因此，无论何时生成一行，都需要提供类型信息。
 *
 * <p>行的主要用途是在Flink的Table和SQL生态系统以及其他api之间架起桥梁。因此，一行不仅包含模式部分(包含字段)，而且
 *   还附加一个{@link RowKind}，用于在变更日志中编码变更。因此，可以将一行视为变更日志中的一个条目。例如，在常规的
 *   批处理场景中，一个变更日志将由{@link RowKind#INSERT}行的有界流组成。行类型与字段是分开的，可以通过使用
 *   {@link #getKind()}和{@link #setKind(RowKind)}来访问。
 *
 * <p>一行的字段可以基于位置访问，也可以基于名称访问。实现者可以决定一行在创建过程中应该以哪种字段模式进行操作。由框架
 *   生成的行支持两种字段模式的混合(即命名位置):
 *
 * A row is a fixed-length, null-aware composite type for storing multiple values in a deterministic
 * field order. Every field can be null regardless of the field's type. The type of row fields
 * cannot be automatically inferred; therefore, it is required to provide type information whenever
 * a row is produced.
 *
 * <p>The main purpose of rows is to bridge between Flink's Table and SQL ecosystem and other APIs.
 * Therefore, a row does not only consist of a schema part (containing the fields) but also attaches
 * a {@link RowKind} for encoding a change in a changelog. Thus, a row can be considered as an entry
 * in a changelog. For example, in regular batch scenarios, a changelog would consist of a bounded
 * stream of {@link RowKind#INSERT} rows. The row kind is kept separate from the fields and can be
 * accessed by using {@link #getKind()} and {@link #setKind(RowKind)}.
 *
 * <p>Fields of a row can be accessed either position-based or name-based. An implementer can decide
 * in which field mode a row should operate during creation. Rows that were produced by the
 * framework support a hybrid of both field modes (i.e. named positions):
 *
 * <h1>Position-based field mode</h1>
 *
 * <p>{@link Row#withPositions(int)} creates a fixed-length row. The fields can be accessed by
 * position (zero-based) using {@link #getField(int)} and {@link #setField(int, Object)}. Every
 * field is initialized with {@code null} by default.
 *
 * <h1>Name-based field mode</h1>
 *
 * <p>{@link Row#withNames()} creates a variable-length row. The fields can be accessed by name
 * using {@link #getField(String)} and {@link #setField(String, Object)}. Every field is initialized
 * during the first call to {@link #setField(String, Object)} for the given name. However, the
 * framework will initialize missing fields with {@code null} and reorder all fields once more type
 * information is available during serialization or input conversion. Thus, even name-based rows
 * eventually become fixed-length composite types with a deterministic field order. Name-based rows
 * perform worse than position-based rows but simplify row creation and code readability.
 *
 * <h1>Hybrid / named-position field mode</h1>
 *
 * <p>Rows that were produced by the framework (after deserialization or output conversion) are
 * fixed-length rows with a deterministic field order that can map static field names to field
 * positions. Thus, fields can be accessed both via {@link #getField(int)} and {@link
 * #getField(String)}. Both {@link #setField(int, Object)} and {@link #setField(String, Object)} are
 * supported for existing fields. However, adding new field names via {@link #setField(String,
 * Object)} is not allowed. A hybrid row's {@link #equals(Object)} supports comparing to all kinds
 * of rows. A hybrid row's {@link #hashCode()} is only valid for position-based rows.
 *
 * <p>A row instance is in principle {@link Serializable}. However, it may contain non-serializable
 * fields in which case serialization will fail if the row is not serialized with Flink's
 * serialization stack.
 *
 * <p>The {@link #equals(Object)} and {@link #hashCode()} methods of this class support all external
 * conversion classes of the table ecosystem.
 */
@PublicEvolving
public final class Row implements Serializable {

    private static final long serialVersionUID = 3L;

    /** The kind of change a row describes in a changelog. */
    // 更改日志中一行描述的更改类型
    private RowKind kind;

    /** Fields organized by position. Either this or {@link #fieldByName} is set. */
    // 按位置组织的属性。或者{@link #fieldByName}被设置
    private final @Nullable Object[] fieldByPosition;

    /** Fields organized by name. Either this or {@link #fieldByPosition} is set. */
    // 按名称组织的字段。要么设置了这个，要么设置了{@link #fieldByPosition}
    private final @Nullable Map<String, Object> fieldByName;

    /** Mapping from field names to positions. Requires {@link #fieldByPosition} semantics. */
    // 从字段名到位置的映射。需要{@link #fieldByPosition}语义
    private final @Nullable LinkedHashMap<String, Integer> positionByName;

    Row(
            RowKind kind,
            @Nullable Object[] fieldByPosition,
            @Nullable Map<String, Object> fieldByName,
            @Nullable LinkedHashMap<String, Integer> positionByName) {
        this.kind = kind;
        this.fieldByPosition = fieldByPosition;
        this.fieldByName = fieldByName;
        this.positionByName = positionByName;
    }

    /**
     * Creates a fixed-length row in position-based field mode.
     *
     * <p>The semantics are equivalent to {@link Row#withPositions(RowKind, int)}. This constructor
     * exists for backwards compatibility.
     *
     * @param kind kind of change a row describes in a changelog
     * @param arity the number of fields in the row
     *              行中字段的数量
     */
    public Row(RowKind kind, int arity) {
        this.kind = Preconditions.checkNotNull(kind, "Row kind must not be null.");
        this.fieldByPosition = new Object[arity];
        this.fieldByName = null;
        this.positionByName = null;
    }

    /**
     * 在基于位置的字段模式下创建固定长度的行。
     *
     * <p>语义等价于{@link Row#withPositions(int)}。此构造函数的存在是为了向后兼容。
     *
     * Creates a fixed-length row in position-based field mode.
     *
     * <p>The semantics are equivalent to {@link Row#withPositions(int)}. This constructor exists
     * for backwards compatibility.
     *
     * @param arity the number of fields in the row
     */
    public Row(int arity) {
        this(RowKind.INSERT, arity);
    }

    /**
     * Creates a fixed-length row in position-based field mode.
     *
     * <p>Fields can be accessed by position via {@link #setField(int, Object)} and {@link
     * #getField(int)}.
     *
     * <p>See the class documentation of {@link Row} for more information.
     *
     * @param kind kind of change a row describes in a changelog
     * @param arity the number of fields in the row
     * @return a new row instance
     */
    public static Row withPositions(RowKind kind, int arity) {
        return new Row(kind, new Object[arity], null, null);
    }

    /**
     * Creates a fixed-length row in position-based field mode.
     *
     * <p>Fields can be accessed by position via {@link #setField(int, Object)} and {@link
     * #getField(int)}.
     *
     * <p>By default, a row describes an {@link RowKind#INSERT} change.
     *
     * <p>See the class documentation of {@link Row} for more information.
     *
     * @param arity the number of fields in the row
     * @return a new row instance
     */
    public static Row withPositions(int arity) {
        return withPositions(RowKind.INSERT, arity);
    }

    /**
     * 以基于名称的字段模式创建可变长度的行。
     *
     * <p>字段可以通过名称({@link #setField(String, Object)}和 {@link #getField(String)}访问。
     *
     * <p>有关更多信息，请参阅{@link Row}的类文档。
     *
     * Creates a variable-length row in name-based field mode.
     *
     * <p>Fields can be accessed by name via {@link #setField(String, Object)} and {@link
     * #getField(String)}.
     *
     * <p>See the class documentation of {@link Row} for more information.
     *
     * @param kind kind of change a row describes in a changelog
     * @return a new row instance
     */
    public static Row withNames(RowKind kind) {
        return new Row(kind, null, new HashMap<>(), null);
    }

    /**
     * Creates a variable-length row in name-based field mode.
     *
     * <p>Fields can be accessed by name via {@link #setField(String, Object)} and {@link
     * #getField(String)}.
     *
     * <p>By default, a row describes an {@link RowKind#INSERT} change.
     *
     * <p>See the class documentation of {@link Row} for more information.
     *
     * @return a new row instance
     */
    public static Row withNames() {
        return withNames(RowKind.INSERT);
    }

    /**
     * Returns the kind of change that this row describes in a changelog.
     *
     * <p>By default, a row describes an {@link RowKind#INSERT} change.
     *
     * @see RowKind
     */
    public RowKind getKind() {
        return kind;
    }

    /**
     * Sets the kind of change that this row describes in a changelog.
     *
     * <p>By default, a row describes an {@link RowKind#INSERT} change.
     *
     * @see RowKind
     */
    public void setKind(RowKind kind) {
        Preconditions.checkNotNull(kind, "Row kind must not be null.");
        this.kind = kind;
    }

    /**
     * 返回行中的字段数
     *
     * <p>注意:行类型与字段保持分离，不包含在这个数字中
     *
     * Returns the number of fields in the row.
     *
     * <p>Note: The row kind is kept separate from the fields and is not included in this number.
     *
     * @return the number of fields in the row
     */
    public int getArity() {
        if (fieldByPosition != null) {
            return fieldByPosition.length;
        } else {
            assert fieldByName != null;
            return fieldByName.size();
        }
    }

    /**
     * Returns the field's content at the specified field position.
     *
     * <p>Note: The row must operate in position-based field mode.
     *
     * @param pos the position of the field, 0-based
     * @return the field's content at the specified position
     */
    public @Nullable Object getField(int pos) {
        if (fieldByPosition != null) {
            return fieldByPosition[pos];
        } else {
            throw new IllegalArgumentException(
                    "Accessing a field by position is not supported in name-based field mode.");
        }
    }

    /**
     * Returns the field's content at the specified field position.
     *
     * <p>Note: The row must operate in position-based field mode.
     *
     * <p>This method avoids a lot of manual casting in the user implementation.
     *
     * @param pos the position of the field, 0-based
     * @return the field's content at the specified position
     */
    @SuppressWarnings("unchecked")
    public <T> T getFieldAs(int pos) {
        return (T) getField(pos);
    }

    /**
     * Returns the field's content using the specified field name.
     *
     * <p>Note: The row must operate in name-based field mode.
     *
     * @param name the name of the field or null if not set previously
     * @return the field's content
     */
    public @Nullable Object getField(String name) {
        if (fieldByName != null) {
            return fieldByName.get(name);
        } else if (positionByName != null) {
            final Integer pos = positionByName.get(name);
            if (pos == null) {
                throw new IllegalArgumentException(
                        String.format("Unknown field name '%s' for mapping to a position.", name));
            }
            assert fieldByPosition != null;
            return fieldByPosition[pos];
        } else {
            throw new IllegalArgumentException(
                    "Accessing a field by name is not supported in position-based field mode.");
        }
    }

    /**
     * Returns the field's content using the specified field name.
     *
     * <p>Note: The row must operate in name-based field mode.
     *
     * <p>This method avoids a lot of manual casting in the user implementation.
     *
     * @param name the name of the field, set previously
     * @return the field's content
     */
    @SuppressWarnings("unchecked")
    public <T> T getFieldAs(String name) {
        return (T) getField(name);
    }

    /**
     * Sets the field's content at the specified position.
     *
     * <p>Note: The row must operate in position-based field mode.
     *
     * @param pos the position of the field, 0-based
     * @param value the value to be assigned to the field at the specified position
     */
    public void setField(int pos, @Nullable Object value) {
        if (fieldByPosition != null) {
            fieldByPosition[pos] = value;
        } else {
            throw new IllegalArgumentException(
                    "Accessing a field by position is not supported in name-based field mode.");
        }
    }

    /**
     * Sets the field's content using the specified field name.
     *
     * <p>Note: The row must operate in name-based field mode.
     *
     * @param name the name of the field
     * @param value the value to be assigned to the field
     */
    public void setField(String name, @Nullable Object value) {
        if (fieldByName != null) {
            fieldByName.put(name, value);
        } else if (positionByName != null) {
            final Integer pos = positionByName.get(name);
            if (pos == null) {
                throw new IllegalArgumentException(
                        String.format(
                                "Unknown field name '%s' for mapping to a row position. "
                                        + "Available names are: %s",
                                name, positionByName.keySet()));
            }
            assert fieldByPosition != null;
            fieldByPosition[pos] = value;
        } else {
            throw new IllegalArgumentException(
                    "Accessing a field by name is not supported in position-based field mode.");
        }
    }

    /**
     * Returns the set of field names if this row operates in name-based field mode, otherwise null.
     *
     * <p>This method is a helper method for serializers and converters but can also be useful for
     * other row transformations.
     *
     * @param includeNamedPositions whether or not to include named positions when this row operates
     *     in a hybrid field mode
     */
    public @Nullable Set<String> getFieldNames(boolean includeNamedPositions) {
        if (fieldByName != null) {
            return fieldByName.keySet();
        }
        if (includeNamedPositions && positionByName != null) {
            return positionByName.keySet();
        }
        return null;
    }

    /** Clears all fields of this row. */
    public void clear() {
        if (fieldByPosition != null) {
            Arrays.fill(fieldByPosition, null);
        } else {
            assert fieldByName != null;
            fieldByName.clear();
        }
    }

    @Override
    public String toString() {
        return RowUtils.deepToStringRow(kind, fieldByPosition, fieldByName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Row other = (Row) o;
        return deepEqualsRow(
                kind,
                fieldByPosition,
                fieldByName,
                positionByName,
                other.kind,
                other.fieldByPosition,
                other.fieldByName,
                other.positionByName);
    }

    @Override
    public int hashCode() {
        return deepHashCodeRow(kind, fieldByPosition, fieldByName);
    }

    // --------------------------------------------------------------------------------------------
    // Utility methods
    // --------------------------------------------------------------------------------------------

    /**
     * 以基于位置的字段模式创建固定长度的行，并将给定的值分配给该行的字段。
     *
     * <p>在很多情况下，这个方法应该比{@link Row#withPositions(int)}更方便。
     *
     * Creates a fixed-length row in position-based field mode and assigns the given values to the
     * row's fields.
     *
     * <p>This method should be more convenient than {@link Row#withPositions(int)} in many cases.
     *
     * <p>For example:
     *
     * <pre>
     *     Row.of("hello", true, 1L);
     * </pre>
     *
     * instead of
     *
     * <pre>
     *     Row row = Row.withPositions(3);
     *     row.setField(0, "hello");
     *     row.setField(1, true);
     *     row.setField(2, 1L);
     * </pre>
     *
     * <p>By default, a row describes an {@link RowKind#INSERT} change.
     */
    public static Row of(Object... values) {
        final Row row = new Row(values.length);
        for (int i = 0; i < values.length; i++) {
            row.setField(i, values[i]);
        }
        return row;
    }

    /**
     * 使用给定类型在基于位置的字段模式中创建固定长度的行，并将给定值赋给行字段。
     *
     * <p>在很多情况下，这个方法应该比{@link Row#withPositions(RowKind, int)}更方便
     *
     * Creates a fixed-length row in position-based field mode with given kind and assigns the given
     * values to the row's fields.
     *
     * <p>This method should be more convenient than {@link Row#withPositions(RowKind, int)} in many
     * cases.
     *
     * <p>For example:
     *
     * <pre>
     *     Row.ofKind(RowKind.INSERT, "hello", true, 1L);
     * </pre>
     *
     * instead of
     *
     * <pre>
     *     Row row = Row.withPositions(RowKind.INSERT, 3);
     *     row.setField(0, "hello");
     *     row.setField(1, true);
     *     row.setField(2, 1L);
     * </pre>
     */
    public static Row ofKind(RowKind kind, Object... values) {
        final Row row = new Row(kind, values.length);
        for (int i = 0; i < values.length; i++) {
            row.setField(i, values[i]);
        }
        return row;
    }

    /**
     * 创建从另一行复制的新行(包括其{@link RowKind})。
     *
     * <p>该方法不执行深度复制。如果需要，使用{@link RowSerializer#copy(Row)}。
     *
     * Creates a new row which is copied from another row (including its {@link RowKind}).
     *
     * <p>This method does not perform a deep copy. Use {@link RowSerializer#copy(Row)} if required.
     */
    public static Row copy(Row row) {
        final Object[] newFieldByPosition;
        if (row.fieldByPosition != null) {
            newFieldByPosition = new Object[row.fieldByPosition.length];
            System.arraycopy(
                    row.fieldByPosition, 0, newFieldByPosition, 0, newFieldByPosition.length);
        } else {
            newFieldByPosition = null;
        }

        final Map<String, Object> newFieldByName;
        if (row.fieldByName != null) {
            newFieldByName = new HashMap<>(row.fieldByName);
        } else {
            newFieldByName = null;
        }

        return new Row(row.kind, newFieldByPosition, newFieldByName, row.positionByName);
    }

    /**
     * 创建具有投影字段和与另一行相同的{@link RowKind}的新行。
     *
     * <p>该方法不执行深度复制。
     *
     * <p>备注:行必须以基于位置的字段模式操作。字段名不投影。
     *
     * Creates a new row with projected fields and identical {@link RowKind} from another row.
     *
     * <p>This method does not perform a deep copy.
     *
     * <p>Note: The row must operate in position-based field mode. Field names are not projected.
     *
     * @param fieldPositions field indices to be projected
     */
    public static Row project(Row row, int[] fieldPositions) {
        final Row newRow = Row.withPositions(row.kind, fieldPositions.length);
        for (int i = 0; i < fieldPositions.length; i++) {
            newRow.setField(i, row.getField(fieldPositions[i]));
        }
        return newRow;
    }

    /**
     * 创建具有投影字段和与另一行相同的{@link RowKind}的新行。
     *
     * <p>该方法不执行深度复制。
     *
     * <p>备注:行必须为基于名称的字段模式。
     *
     * Creates a new row with projected fields and identical {@link RowKind} from another row.
     *
     * <p>This method does not perform a deep copy.
     *
     * <p>Note: The row must operate in name-based field mode.
     *
     * @param fieldNames field names to be projected
     */
    public static Row project(Row row, String[] fieldNames) {
        final Row newRow = Row.withNames(row.getKind());
        for (String fieldName : fieldNames) {
            newRow.setField(fieldName, row.getField(fieldName));
        }
        return newRow;
    }

    /**
     * 创建带有从其他行复制并按给定顺序附加到结果行的字段的新行。第一行的{@link RowKind}决定了结果的{@link RowKind}。
     *
     * <p>该方法不执行深度复制。
     *
     * <p>备注:所有行必须在基于位置的字段模式下操作。
     *
     * Creates a new row with fields that are copied from the other rows and appended to the
     * resulting row in the given order. The {@link RowKind} of the first row determines the {@link
     * RowKind} of the result.
     *
     * <p>This method does not perform a deep copy.
     *
     * <p>Note: All rows must operate in position-based field mode.
     */
    public static Row join(Row first, Row... remainings) {
        Preconditions.checkArgument(
                first.fieldByPosition != null,
                "All rows must operate in position-based field mode.");
        int newLength = first.fieldByPosition.length;
        for (Row remaining : remainings) {
            Preconditions.checkArgument(
                    remaining.fieldByPosition != null,
                    "All rows must operate in position-based field mode.");
            newLength += remaining.fieldByPosition.length;
        }

        final Row joinedRow = new Row(first.kind, newLength);
        int index = 0;

        // copy the first row
        assert joinedRow.fieldByPosition != null;
        System.arraycopy(
                first.fieldByPosition,
                0,
                joinedRow.fieldByPosition,
                index,
                first.fieldByPosition.length);
        index += first.fieldByPosition.length;

        // copy the remaining rows
        for (Row remaining : remainings) {
            assert remaining.fieldByPosition != null;
            System.arraycopy(
                    remaining.fieldByPosition,
                    0,
                    joinedRow.fieldByPosition,
                    index,
                    remaining.fieldByPosition.length);
            index += remaining.fieldByPosition.length;
        }

        return joinedRow;
    }
}
