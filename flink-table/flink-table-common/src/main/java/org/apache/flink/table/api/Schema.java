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

package org.apache.flink.table.api;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.Column.ComputedColumn;
import org.apache.flink.table.catalog.Column.MetadataColumn;
import org.apache.flink.table.catalog.Column.PhysicalColumn;
import org.apache.flink.table.catalog.Constraint;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.catalog.SchemaResolver;
import org.apache.flink.table.catalog.UniqueConstraint;
import org.apache.flink.table.catalog.WatermarkSpec;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.expressions.SqlCallExpression;
import org.apache.flink.table.types.AbstractDataType;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.utils.EncodingUtils;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.StringUtils;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.flink.table.types.logical.utils.LogicalTypeChecks.hasRoot;

/**
 * 表或视图的模式。
 *
 * <p>模式表示SQL中{@code CREATE TABLE (schema) WITH (options)} DDL语句的模式部分。它定义了不同类型的列、
 *   约束、时间属性和水印策略。可以跨不同的目录引用对象(如函数或类型)。
 *
 * <p>在API和编目中使用该类来定义一个未解析的模式，该模式将被转换为{@link ResolvedSchema}。该类的一些方法执行
 *   基本验证，但是主要验证发生在解析过程中。因此，一个未解决的模式可能是不完整的，可能在以后的阶段被丰富或与不同的
 *   模式合并。
 *
 * <p>因为该类的实例未解析，所以不应该直接持久化它。{@link #toString()}只显示所包含对象的摘要。
 *
 * Schema of a table or view.
 *
 * <p>A schema represents the schema part of a {@code CREATE TABLE (schema) WITH (options)} DDL
 * statement in SQL. It defines columns of different kind, constraints, time attributes, and
 * watermark strategies. It is possible to reference objects (such as functions or types) across
 * different catalogs.
 *
 * <p>This class is used in the API and catalogs to define an unresolved schema that will be
 * translated to {@link ResolvedSchema}. Some methods of this class perform basic validation,
 * however, the main validation happens during the resolution. Thus, an unresolved schema can be
 * incomplete and might be enriched or merged with a different schema at a later stage.
 *
 * <p>Since an instance of this class is unresolved, it should not be directly persisted. The {@link
 * #toString()} shows only a summary of the contained objects.
 */
@PublicEvolving
public final class Schema {

    // 未解析的列
    private final List<UnresolvedColumn> columns;

    // 未解析的水印
    private final List<UnresolvedWatermarkSpec> watermarkSpecs;

    // 未解析的主键约束
    private final @Nullable UnresolvedPrimaryKey primaryKey;

    private Schema(
            List<UnresolvedColumn> columns,
            List<UnresolvedWatermarkSpec> watermarkSpecs,
            @Nullable UnresolvedPrimaryKey primaryKey) {
        this.columns = columns;
        this.watermarkSpecs = watermarkSpecs;
        this.primaryKey = primaryKey;
    }

    /** Builder for configuring and creating instances of {@link Schema}. */
    public static Schema.Builder newBuilder() {
        return new Builder();
    }

    public List<UnresolvedColumn> getColumns() {
        return columns;
    }

    public List<UnresolvedWatermarkSpec> getWatermarkSpecs() {
        return watermarkSpecs;
    }

    public Optional<UnresolvedPrimaryKey> getPrimaryKey() {
        return Optional.ofNullable(primaryKey);
    }

    /** Resolves the given {@link Schema} to a validated {@link ResolvedSchema}. */
    public ResolvedSchema resolve(SchemaResolver resolver) {
        return resolver.resolve(this);
    }

    @Override
    public String toString() {
        final List<Object> components = new ArrayList<>();
        components.addAll(columns);
        components.addAll(watermarkSpecs);
        if (primaryKey != null) {
            components.add(primaryKey);
        }
        return components.stream()
                .map(Objects::toString)
                .map(s -> "  " + s)
                .collect(Collectors.joining(",\n", "(\n", "\n)"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Schema schema = (Schema) o;
        return columns.equals(schema.columns)
                && watermarkSpecs.equals(schema.watermarkSpecs)
                && Objects.equals(primaryKey, schema.primaryKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns, watermarkSpecs, primaryKey);
    }

    // --------------------------------------------------------------------------------------------

    /** A builder for constructing an immutable but still unresolved {@link Schema}. */
    public static final class Builder {

        private final List<UnresolvedColumn> columns;

        private final List<UnresolvedWatermarkSpec> watermarkSpecs;

        private @Nullable UnresolvedPrimaryKey primaryKey;

        private Builder() {
            columns = new ArrayList<>();
            watermarkSpecs = new ArrayList<>();
        }

        /** Adopts all members from the given unresolved schema. */
        // 采用给定的未解析模式中的所有成员。
        public Builder fromSchema(Schema unresolvedSchema) {
            columns.addAll(unresolvedSchema.columns);
            watermarkSpecs.addAll(unresolvedSchema.watermarkSpecs);
            if (unresolvedSchema.primaryKey != null) {
                primaryKeyNamed(
                        unresolvedSchema.primaryKey.getConstraintName(),
                        unresolvedSchema.primaryKey.getColumnNames());
            }
            return this;
        }

        /** Adopts all members from the given resolved schema. */
        public Builder fromResolvedSchema(ResolvedSchema resolvedSchema) {
            addResolvedColumns(resolvedSchema.getColumns());
            addResolvedWatermarkSpec(resolvedSchema.getWatermarkSpecs());
            resolvedSchema.getPrimaryKey().ifPresent(this::addResolvedConstraint);
            return this;
        }

        /** Adopts all fields of the given row as physical columns of the schema. */
        // 采用给定行的所有字段作为模式的物理列
        public Builder fromRowDataType(DataType dataType) {
            Preconditions.checkNotNull(dataType, "Data type must not be null.");
            Preconditions.checkArgument(
                    hasRoot(dataType.getLogicalType(), LogicalTypeRoot.ROW),
                    "Data type of ROW expected.");
            final List<DataType> fieldDataTypes = dataType.getChildren();
            final List<String> fieldNames = ((RowType) dataType.getLogicalType()).getFieldNames();
            IntStream.range(0, fieldDataTypes.size())
                    .forEach(i -> column(fieldNames.get(i), fieldDataTypes.get(i)));
            return this;
        }

        /** Adopts the given field names and field data types as physical columns of the schema. */
        // 使用给定的字段名和字段数据类型作为模式的物理列
        public Builder fromFields(String[] fieldNames, AbstractDataType<?>[] fieldDataTypes) {
            Preconditions.checkNotNull(fieldNames, "Field names must not be null.");
            Preconditions.checkNotNull(fieldDataTypes, "Field data types must not be null.");
            Preconditions.checkArgument(
                    fieldNames.length == fieldDataTypes.length,
                    "Field names and field data types must have the same length.");
            IntStream.range(0, fieldNames.length)
                    .forEach(i -> column(fieldNames[i], fieldDataTypes[i]));
            return this;
        }

        /** Adopts the given field names and field data types as physical columns of the schema. */
        // 使用给定的字段名和字段数据类型作为模式的物理列
        public Builder fromFields(
                List<String> fieldNames, List<? extends AbstractDataType<?>> fieldDataTypes) {
            Preconditions.checkNotNull(fieldNames, "Field names must not be null.");
            Preconditions.checkNotNull(fieldDataTypes, "Field data types must not be null.");
            Preconditions.checkArgument(
                    fieldNames.size() == fieldDataTypes.size(),
                    "Field names and field data types must have the same length.");
            IntStream.range(0, fieldNames.size())
                    .forEach(i -> column(fieldNames.get(i), fieldDataTypes.get(i)));
            return this;
        }

        /** Adopts all columns from the given list. */
        // 采用给定列表中的所有列。
        public Builder fromColumns(List<UnresolvedColumn> unresolvedColumns) {
            columns.addAll(unresolvedColumns);
            return this;
        }

        /**
         * 声明附加到此模式的物理列。
         *
         * <p>物理列是数据库中已知的常规列。它们定义物理数据中字段的名称、类型和顺序。因此，物理列表示从外部系统读取
         *   和写入的有效负载。连接器和格式使用这些列(按照定义的顺序)来配置自己。其他类型的列可以在物理列之间声明，
         *   但不会影响最终的物理模式。
         *
         * Declares a physical column that is appended to this schema.
         *
         * <p>Physical columns are regular columns known from databases. They define the names, the
         * types, and the order of fields in the physical data. Thus, physical columns represent the
         * payload that is read from and written to an external system. Connectors and formats use
         * these columns (in the defined order) to configure themselves. Other kinds of columns can
         * be declared between physical columns but will not influence the final physical schema.
         *
         * @param columnName column name
         * @param dataType data type of the column
         */
        public Builder column(String columnName, AbstractDataType<?> dataType) {
            Preconditions.checkNotNull(columnName, "Column name must not be null.");
            Preconditions.checkNotNull(dataType, "Data type must not be null.");
            columns.add(new UnresolvedPhysicalColumn(columnName, dataType));
            return this;
        }

        /**
         * 声明附加到此模式的物理列。
         *
         * <p>参见{@link #column(String, AbstractDataType)}获取详细解释。
         *
         * <p>此方法使用的类型字符串可以很容易地持久化到持久目录中。
         *
         * Declares a physical column that is appended to this schema.
         *
         * <p>See {@link #column(String, AbstractDataType)} for a detailed explanation.
         *
         * <p>This method uses a type string that can be easily persisted in a durable catalog.
         *
         * @param columnName column name
         * @param serializableTypeString data type of the column as a serializable string
         * @see LogicalType#asSerializableString()
         */
        public Builder column(String columnName, String serializableTypeString) {
            return column(columnName, DataTypes.of(serializableTypeString));
        }

        /**
         * 声明附加到此模式的计算列。
         *
         * <p>计算列是通过计算一个表达式生成的虚拟列，该表达式可以引用同一表中声明的其他列。物理列和元数据列都可以访问。
         *   列本身并不物理地存储在表中。列的数据类型是从给定的表达式自动派生的，不需要手动声明。
         *
         * <p>计算列通常用于定义时间属性。例如，如果原始字段不是TIMESTAMP(3)类型或嵌套在JSON字符串中，则可以
         *   使用计算列。
         *
         * <p>Example: {@code .columnByExpression("ts", $("json_obj").get("ts").cast(TIMESTAMP(3))}
         *
         * Declares a computed column that is appended to this schema.
         *
         * <p>Computed columns are virtual columns that are generated by evaluating an expression
         * that can reference other columns declared in the same table. Both physical columns and
         * metadata columns can be accessed. The column itself is not physically stored within the
         * table. The column’s data type is derived automatically from the given expression and does
         * not have to be declared manually.
         *
         * <p>Computed columns are commonly used for defining time attributes. For example, the
         * computed column can be used if the original field is not TIMESTAMP(3) type or is nested
         * in a JSON string.
         *
         * <p>Any scalar expression can be used for in-memory/temporary tables. However, currently,
         * only SQL expressions can be persisted in a catalog. User-defined functions (also defined
         * in different catalogs) are supported.
         *
         * <p>Example: {@code .columnByExpression("ts", $("json_obj").get("ts").cast(TIMESTAMP(3))}
         *
         * @param columnName column name
         * @param expression computation of the column
         */
        public Builder columnByExpression(String columnName, Expression expression) {
            Preconditions.checkNotNull(columnName, "Column name must not be null.");
            Preconditions.checkNotNull(expression, "Expression must not be null.");
            columns.add(new UnresolvedComputedColumn(columnName, expression));
            return this;
        }

        /**
         * Declares a computed column that is appended to this schema.
         *
         * <p>See {@link #columnByExpression(String, Expression)} for a detailed explanation.
         *
         * <p>This method uses a SQL expression that can be easily persisted in a durable catalog.
         *
         * <p>Example: {@code .columnByExpression("ts", "CAST(json_obj.ts AS TIMESTAMP(3))")}
         *
         * @param columnName column name
         * @param sqlExpression computation of the column using SQL
         */
        public Builder columnByExpression(String columnName, String sqlExpression) {
            return columnByExpression(columnName, new SqlCallExpression(sqlExpression));
        }

        /**
         * 声明附加到此模式的元数据列。
         *
         * <p>元数据列允许访问表的每一行的连接器和或格式化特定字段。例如，元数据列可以用来读写Kafka记录的时间戳，
         *   以进行基于时间的操作。连接器和格式文档列出了每个组件的可用元数据字段。
         *
         * <p>每个元数据字段都由基于字符串的键标识，并具有文档化的数据类型。为方便起见，如果列的数据类型与元数据字段
         *   的数据类型不同，运行时将执行显式强制类型转换。当然，这要求这两种数据类型是兼容的。
         *
         * <p>注意:此方法假设元数据键等于列名，并且元数据列可以同时用于读写。
         *
         * Declares a metadata column that is appended to this schema.
         *
         * <p>Metadata columns allow to access connector and/or format specific fields for every row
         * of a table. For example, a metadata column can be used to read and write the timestamp
         * from and to Kafka records for time-based operations. The connector and format
         * documentation lists the available metadata fields for every component.
         *
         * <p>Every metadata field is identified by a string-based key and has a documented data
         * type. For convenience, the runtime will perform an explicit cast if the data type of the
         * column differs from the data type of the metadata field. Of course, this requires that
         * the two data types are compatible.
         *
         * <p>Note: This method assumes that the metadata key is equal to the column name and the
         * metadata column can be used for both reading and writing.
         *
         * @param columnName column name
         * @param dataType data type of the column
         */
        public Builder columnByMetadata(String columnName, AbstractDataType<?> dataType) {
            return columnByMetadata(columnName, dataType, null, false);
        }

        /**
         * 声明附加到此模式的元数据列。
         *
         * <p>参见{@link #column(String, AbstractDataType)}获取详细解释。
         *
         * <p>此方法使用的类型字符串可以很容易地持久化到持久目录中。
         *
         * Declares a metadata column that is appended to this schema.
         *
         * <p>See {@link #column(String, AbstractDataType)} for a detailed explanation.
         *
         * <p>This method uses a type string that can be easily persisted in a durable catalog.
         *
         * @param columnName column name
         * @param serializableTypeString data type of the column
         */
        public Builder columnByMetadata(String columnName, String serializableTypeString) {
            return columnByMetadata(columnName, serializableTypeString, null, false);
        }

        /**
         * 声明附加到此模式的元数据列。
         *
         * <p>元数据列允许访问表的每一行的连接器和或格式化特定字段。例如，元数据列可以用来读写Kafka记录的时间戳，以
         *   进行基于时间的操作。连接器和格式文档列出了每个组件的可用元数据字段。
         *
         * <p>每个元数据字段都由基于字符串的键标识，并具有文档化的数据类型。为方便起见，如果列的数据类型与元数据字段
         *   的数据类型不同，运行时将执行显式强制类型转换。当然，这要求这两种数据类型是兼容的。
         *
         * <p>默认情况下，元数据列可以同时用于读写。然而，在许多情况下，外部系统提供的只读元数据字段比可写字段更多。
         *   因此，可以通过设置{@code isVirtual}标志为{@code true}来排除元数据列的持久化。
         *
         * <p>注意:此方法假设元数据键等于列名。
         *
         * Declares a metadata column that is appended to this schema.
         *
         * <p>Metadata columns allow to access connector and/or format specific fields for every row
         * of a table. For example, a metadata column can be used to read and write the timestamp
         * from and to Kafka records for time-based operations. The connector and format
         * documentation lists the available metadata fields for every component.
         *
         * <p>Every metadata field is identified by a string-based key and has a documented data
         * type. For convenience, the runtime will perform an explicit cast if the data type of the
         * column differs from the data type of the metadata field. Of course, this requires that
         * the two data types are compatible.
         *
         * <p>By default, a metadata column can be used for both reading and writing. However, in
         * many cases an external system provides more read-only metadata fields than writable
         * fields. Therefore, it is possible to exclude metadata columns from persisting by setting
         * the {@code isVirtual} flag to {@code true}.
         *
         * <p>Note: This method assumes that the metadata key is equal to the column name.
         *
         * @param columnName column name
         * @param dataType data type of the column
         * @param isVirtual whether the column should be persisted or not
         */
        public Builder columnByMetadata(
                String columnName, AbstractDataType<?> dataType, boolean isVirtual) {
            return columnByMetadata(columnName, dataType, null, isVirtual);
        }

        /**
         * Declares a metadata column that is appended to this schema.
         *
         * <p>Metadata columns allow to access connector and/or format specific fields for every row
         * of a table. For example, a metadata column can be used to read and write the timestamp
         * from and to Kafka records for time-based operations. The connector and format
         * documentation lists the available metadata fields for every component.
         *
         * <p>Every metadata field is identified by a string-based key and has a documented data
         * type. The metadata key can be omitted if the column name should be used as the
         * identifying metadata key. For convenience, the runtime will perform an explicit cast if
         * the data type of the column differs from the data type of the metadata field. Of course,
         * this requires that the two data types are compatible.
         *
         * <p>Note: This method assumes that a metadata column can be used for both reading and
         * writing.
         *
         * @param columnName column name
         * @param dataType data type of the column
         * @param metadataKey identifying metadata key, if null the column name will be used as
         *     metadata key
         */
        public Builder columnByMetadata(
                String columnName, AbstractDataType<?> dataType, @Nullable String metadataKey) {
            return columnByMetadata(columnName, dataType, metadataKey, false);
        }

        /**
         * Declares a metadata column that is appended to this schema.
         *
         * <p>Metadata columns allow to access connector and/or format specific fields for every row
         * of a table. For example, a metadata column can be used to read and write the timestamp
         * from and to Kafka records for time-based operations. The connector and format
         * documentation lists the available metadata fields for every component.
         *
         * <p>Every metadata field is identified by a string-based key and has a documented data
         * type. The metadata key can be omitted if the column name should be used as the
         * identifying metadata key. For convenience, the runtime will perform an explicit cast if
         * the data type of the column differs from the data type of the metadata field. Of course,
         * this requires that the two data types are compatible.
         *
         * <p>By default, a metadata column can be used for both reading and writing. However, in
         * many cases an external system provides more read-only metadata fields than writable
         * fields. Therefore, it is possible to exclude metadata columns from persisting by setting
         * the {@code isVirtual} flag to {@code true}.
         *
         * @param columnName column name
         * @param dataType data type of the column
         * @param metadataKey identifying metadata key, if null the column name will be used as
         *     metadata key
         * @param isVirtual whether the column should be persisted or not
         */
        public Builder columnByMetadata(
                String columnName,
                AbstractDataType<?> dataType,
                @Nullable String metadataKey,
                boolean isVirtual) {
            Preconditions.checkNotNull(columnName, "Column name must not be null.");
            Preconditions.checkNotNull(dataType, "Data type must not be null.");
            columns.add(new UnresolvedMetadataColumn(columnName, dataType, metadataKey, isVirtual));
            return this;
        }

        /**
         * Declares a metadata column that is appended to this schema.
         *
         * <p>See {@link #columnByMetadata(String, AbstractDataType, String, boolean)} for a
         * detailed explanation.
         *
         * <p>This method uses a type string that can be easily persisted in a durable catalog.
         *
         * @param columnName column name
         * @param serializableTypeString data type of the column
         * @param metadataKey identifying metadata key, if null the column name will be used as
         *     metadata key
         * @param isVirtual whether the column should be persisted or not
         */
        public Builder columnByMetadata(
                String columnName,
                String serializableTypeString,
                @Nullable String metadataKey,
                boolean isVirtual) {
            return columnByMetadata(
                    columnName, DataTypes.of(serializableTypeString), metadataKey, isVirtual);
        }

        /**
         * 声明给定的列应该作为事件时间(即行时间)属性，并将相应的水印策略指定为表达式。
         *
         * <p>该列的类型必须为{@code TIMESTAMP(3)}或{@code TIMESTAMP_LTZ(3)}，并且是模式中的顶级列。它可能
         *   是一个计算列。
         *
         * <p>框架在运行时对每条记录计算水印生成表达式。框架将周期性地发出最大的生成水印。如果当前水印仍然与前一个相同，
         *   或为空，或返回的水印值小于上次发出的水印值，则不会发出新的水印。水印在配置定义的时间间隔内发出。
         *
         * <p>任何标量表达式都可以用于为内存中临时表声明水印策略。但是，目前只有SQL表达式可以持久化到目录中。表达式
         *   的返回数据类型必须是{@code TIMESTAMP(3)}。支持用户定义的函数(也在不同的目录中定义)。
         *
         * <p>Example: {@code .watermark("ts", $("ts).minus(lit(5).seconds())}
         *
         * Declares that the given column should serve as an event-time (i.e. rowtime) attribute and
         * specifies a corresponding watermark strategy as an expression.
         *
         * <p>The column must be of type {@code TIMESTAMP(3)} or {@code TIMESTAMP_LTZ(3)} and be a
         * top-level column in the schema. It may be a computed column.
         *
         * <p>The watermark generation expression is evaluated by the framework for every record
         * during runtime. The framework will periodically emit the largest generated watermark. If
         * the current watermark is still identical to the previous one, or is null, or the value of
         * the returned watermark is smaller than that of the last emitted one, then no new
         * watermark will be emitted. A watermark is emitted in an interval defined by the
         * configuration.
         *
         * <p>Any scalar expression can be used for declaring a watermark strategy for
         * in-memory/temporary tables. However, currently, only SQL expressions can be persisted in
         * a catalog. The expression's return data type must be {@code TIMESTAMP(3)}. User-defined
         * functions (also defined in different catalogs) are supported.
         *
         * <p>Example: {@code .watermark("ts", $("ts).minus(lit(5).seconds())}
         *
         * @param columnName the column name used as a rowtime attribute
         * @param watermarkExpression the expression used for watermark generation
         */
        public Builder watermark(String columnName, Expression watermarkExpression) {
            Preconditions.checkNotNull(columnName, "Column name must not be null.");
            Preconditions.checkNotNull(
                    watermarkExpression, "Watermark expression must not be null.");
            this.watermarkSpecs.add(new UnresolvedWatermarkSpec(columnName, watermarkExpression));
            return this;
        }

        /**
         * 声明给定的列应该作为事件时间(即行时间)属性，并将相应的水印策略指定为表达式。
         *
         * Declares that the given column should serve as an event-time (i.e. rowtime) attribute and
         * specifies a corresponding watermark strategy as an expression.
         *
         * <p>See {@link #watermark(String, Expression)} for a detailed explanation.
         *
         * <p>This method uses a SQL expression that can be easily persisted in a durable catalog.
         *
         * <p>Example: {@code .watermark("ts", "ts - INTERVAL '5' SECOND")}
         */
        public Builder watermark(String columnName, String sqlExpression) {
            return watermark(columnName, new SqlCallExpression(sqlExpression));
        }

        /**
         * 为一组给定列声明一个主键约束。主键唯一地标识表中的一行。主列中的任何一列都不能为空。主键仅提供信息。它将
         * 不会被执行。它可以用于优化。数据所有者有责任确保数据的唯一性。
         *
         * <p>主键将被分配一个随机名称。
         *
         * Declares a primary key constraint for a set of given columns. Primary key uniquely
         * identify a row in a table. Neither of columns in a primary can be nullable. The primary
         * key is informational only. It will not be enforced. It can be used for optimizations. It
         * is the data owner's responsibility to ensure uniqueness of the data.
         *
         * <p>The primary key will be assigned a random name.
         *
         * @param columnNames columns that form a unique primary key
         */
        public Builder primaryKey(String... columnNames) {
            Preconditions.checkNotNull(columnNames, "Primary key column names must not be null.");
            return primaryKey(Arrays.asList(columnNames));
        }

        /**
         * Declares a primary key constraint for a set of given columns. Primary key uniquely
         * identify a row in a table. Neither of columns in a primary can be nullable. The primary
         * key is informational only. It will not be enforced. It can be used for optimizations. It
         * is the data owner's responsibility to ensure uniqueness of the data.
         *
         * <p>The primary key will be assigned a generated name in the format {@code PK_col1_col2}.
         *
         * @param columnNames columns that form a unique primary key
         */
        public Builder primaryKey(List<String> columnNames) {
            Preconditions.checkNotNull(columnNames, "Primary key column names must not be null.");
            final String generatedConstraintName =
                    columnNames.stream().collect(Collectors.joining("_", "PK_", ""));
            return primaryKeyNamed(generatedConstraintName, columnNames);
        }

        /**
         * 为一组给定列声明一个主键约束。主键唯一地标识表中的一行。主列中的任何一列都不能为空。主键仅提供信息。它将
         * 不会被执行。它可以用于优化。数据所有者有责任确保数据的唯一性。
         *
         * Declares a primary key constraint for a set of given columns. Primary key uniquely
         * identify a row in a table. Neither of columns in a primary can be nullable. The primary
         * key is informational only. It will not be enforced. It can be used for optimizations. It
         * is the data owner's responsibility to ensure uniqueness of the data.
         *
         * @param constraintName name for the primary key, can be used to reference the constraint
         * @param columnNames columns that form a unique primary key
         */
        public Builder primaryKeyNamed(String constraintName, String... columnNames) {
            Preconditions.checkNotNull(columnNames, "Primary key column names must not be null.");
            return primaryKeyNamed(constraintName, Arrays.asList(columnNames));
        }

        /**
         * Declares a primary key constraint for a set of given columns. Primary key uniquely
         * identify a row in a table. Neither of columns in a primary can be nullable. The primary
         * key is informational only. It will not be enforced. It can be used for optimizations. It
         * is the data owner's responsibility to ensure uniqueness of the data.
         *
         * @param constraintName name for the primary key, can be used to reference the constraint
         * @param columnNames columns that form a unique primary key
         */
        public Builder primaryKeyNamed(String constraintName, List<String> columnNames) {
            Preconditions.checkState(
                    primaryKey == null, "Multiple primary keys are not supported.");
            Preconditions.checkNotNull(
                    constraintName, "Primary key constraint name must not be null.");
            Preconditions.checkArgument(
                    !StringUtils.isNullOrWhitespaceOnly(constraintName),
                    "Primary key constraint name must not be empty.");
            Preconditions.checkArgument(
                    columnNames != null && columnNames.size() > 0,
                    "Primary key constraint must be defined for at least a single column.");
            primaryKey = new UnresolvedPrimaryKey(constraintName, columnNames);
            return this;
        }

        /** Returns an instance of an unresolved {@link Schema}. */
        public Schema build() {
            return new Schema(columns, watermarkSpecs, primaryKey);
        }

        // ----------------------------------------------------------------------------------------

        private void addResolvedColumns(List<Column> columns) {
            columns.forEach(
                    c -> {
                        if (c instanceof PhysicalColumn) {
                            final PhysicalColumn physicalColumn = (PhysicalColumn) c;
                            column(physicalColumn.getName(), physicalColumn.getDataType());
                        } else if (c instanceof ComputedColumn) {
                            final ComputedColumn computedColumn = (ComputedColumn) c;
                            columnByExpression(
                                    computedColumn.getName(), computedColumn.getExpression());
                        } else if (c instanceof MetadataColumn) {
                            final MetadataColumn metadataColumn = (MetadataColumn) c;
                            columnByMetadata(
                                    metadataColumn.getName(),
                                    metadataColumn.getDataType(),
                                    metadataColumn.getMetadataKey().orElse(null),
                                    metadataColumn.isVirtual());
                        }
                    });
        }

        private void addResolvedWatermarkSpec(List<WatermarkSpec> specs) {
            specs.forEach(
                    s ->
                            watermarkSpecs.add(
                                    new UnresolvedWatermarkSpec(
                                            s.getRowtimeAttribute(), s.getWatermarkExpression())));
        }

        private void addResolvedConstraint(UniqueConstraint constraint) {
            if (constraint.getType() == Constraint.ConstraintType.PRIMARY_KEY) {
                primaryKeyNamed(constraint.getName(), constraint.getColumns());
            } else {
                throw new IllegalArgumentException("Unsupported constraint type.");
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // Helper classes for representing the schema
    // --------------------------------------------------------------------------------------------

    /** Super class for all kinds of columns in an unresolved schema. */
    // 用于未解析模式中所有类型列的超类。
    public abstract static class UnresolvedColumn {
        final String columnName;

        UnresolvedColumn(String columnName) {
            this.columnName = columnName;
        }

        public String getName() {
            return columnName;
        }

        @Override
        public String toString() {
            return EncodingUtils.escapeIdentifier(columnName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UnresolvedColumn that = (UnresolvedColumn) o;
            return columnName.equals(that.columnName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(columnName);
        }
    }

    /**
     * 声明一个物理列，该物理列将在模式解析期间解析为{@link PhysicalColumn}。
     *
     * Declaration of a physical column that will be resolved to {@link PhysicalColumn} during
     * schema resolution.
     */
    public static final class UnresolvedPhysicalColumn extends UnresolvedColumn {

        private final AbstractDataType<?> dataType;

        UnresolvedPhysicalColumn(String columnName, AbstractDataType<?> dataType) {
            super(columnName);
            this.dataType = dataType;
        }

        public AbstractDataType<?> getDataType() {
            return dataType;
        }

        @Override
        public String toString() {
            return String.format("%s %s", super.toString(), dataType.toString());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            UnresolvedPhysicalColumn that = (UnresolvedPhysicalColumn) o;
            return dataType.equals(that.dataType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), dataType);
        }
    }

    /**
     * 声明将在模式解析期间解析为{@link ComputedColumn}的计算列。
     *
     * Declaration of a computed column that will be resolved to {@link ComputedColumn} during
     * schema resolution.
     */
    public static final class UnresolvedComputedColumn extends UnresolvedColumn {

        private final Expression expression;

        UnresolvedComputedColumn(String columnName, Expression expression) {
            super(columnName);
            this.expression = expression;
        }

        public Expression getExpression() {
            return expression;
        }

        @Override
        public String toString() {
            return String.format("%s AS %s", super.toString(), expression.asSummaryString());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            UnresolvedComputedColumn that = (UnresolvedComputedColumn) o;
            return expression.equals(that.expression);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), expression);
        }
    }

    /**
     * 声明将在模式解析期间解析为{@link MetadataColumn}的元数据列。
     *
     * Declaration of a metadata column that will be resolved to {@link MetadataColumn} during
     * schema resolution.
     */
    public static final class UnresolvedMetadataColumn extends UnresolvedColumn {

        private final AbstractDataType<?> dataType;
        private final @Nullable String metadataKey;
        private final boolean isVirtual;

        UnresolvedMetadataColumn(
                String columnName,
                AbstractDataType<?> dataType,
                @Nullable String metadataKey,
                boolean isVirtual) {
            super(columnName);
            this.dataType = dataType;
            this.metadataKey = metadataKey;
            this.isVirtual = isVirtual;
        }

        public AbstractDataType<?> getDataType() {
            return dataType;
        }

        public @Nullable String getMetadataKey() {
            return metadataKey;
        }

        public boolean isVirtual() {
            return isVirtual;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append(" METADATA");
            if (metadataKey != null) {
                sb.append(" FROM '");
                sb.append(EncodingUtils.escapeSingleQuotes(metadataKey));
                sb.append("'");
            }
            if (isVirtual) {
                sb.append(" VIRTUAL");
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            UnresolvedMetadataColumn that = (UnresolvedMetadataColumn) o;
            return isVirtual == that.isVirtual
                    && dataType.equals(that.dataType)
                    && Objects.equals(metadataKey, that.metadataKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), dataType, metadataKey, isVirtual);
        }
    }

    /**
     * 声明将在模式解析期间解析为{@link WatermarkSpec}的水印策略。
     *
     * Declaration of a watermark strategy that will be resolved to {@link WatermarkSpec} during
     * schema resolution.
     */
    public static final class UnresolvedWatermarkSpec {

        private final String columnName;
        private final Expression watermarkExpression;

        UnresolvedWatermarkSpec(String columnName, Expression watermarkExpression) {
            this.columnName = columnName;
            this.watermarkExpression = watermarkExpression;
        }

        public String getColumnName() {
            return columnName;
        }

        public Expression getWatermarkExpression() {
            return watermarkExpression;
        }

        @Override
        public String toString() {
            return String.format(
                    "WATERMARK FOR %s AS %s",
                    EncodingUtils.escapeIdentifier(columnName),
                    watermarkExpression.asSummaryString());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UnresolvedWatermarkSpec that = (UnresolvedWatermarkSpec) o;
            return columnName.equals(that.columnName)
                    && watermarkExpression.equals(that.watermarkExpression);
        }

        @Override
        public int hashCode() {
            return Objects.hash(columnName, watermarkExpression);
        }
    }

    /** Super class for all kinds of constraints in an unresolved schema. */
    // 用于解析模式中所有类型约束的超类。
    public abstract static class UnresolvedConstraint {

        private final String constraintName;

        UnresolvedConstraint(String constraintName) {
            this.constraintName = constraintName;
        }

        public String getConstraintName() {
            return constraintName;
        }

        @Override
        public String toString() {
            return String.format("CONSTRAINT %s", EncodingUtils.escapeIdentifier(constraintName));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UnresolvedConstraint that = (UnresolvedConstraint) o;
            return constraintName.equals(that.constraintName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(constraintName);
        }
    }

    /**
     * 声明一个主键，该主键将在模式解析期间解析为{@link UniqueConstraint}。
     *
     * Declaration of a primary key that will be resolved to {@link UniqueConstraint} during schema
     * resolution.
     */
    public static final class UnresolvedPrimaryKey extends UnresolvedConstraint {

        private final List<String> columnNames;

        UnresolvedPrimaryKey(String constraintName, List<String> columnNames) {
            super(constraintName);
            this.columnNames = columnNames;
        }

        public List<String> getColumnNames() {
            return columnNames;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s PRIMARY KEY (%s) NOT ENFORCED",
                    super.toString(),
                    columnNames.stream()
                            .map(EncodingUtils::escapeIdentifier)
                            .collect(Collectors.joining(", ")));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            UnresolvedPrimaryKey that = (UnresolvedPrimaryKey) o;
            return columnNames.equals(that.columnNames);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), columnNames);
        }
    }
}
