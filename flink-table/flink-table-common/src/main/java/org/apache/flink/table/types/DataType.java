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

package org.apache.flink.table.types;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.util.Preconditions;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 描述表生态系统中值的数据类型。该类的实例可用于声明操作的输入和或输出类型。
 *
 * <p> {@link DataType} 类有两个职责:声明逻辑类型和向计划者提供关于数据的物理表示的提示。虽然逻辑类型是强制性的，
 *     但提示是可选的，但在其他 api 的边缘是有用的。
 *
 * <p>逻辑类型独立于任何物理表示，接近SQL标准的“数据类型”术语。有关可用逻辑类型及其属性的更多信息，请参阅
 *   {@link org.apache.flink.table.types.logical.LogicalType} 及其子类。
 *
 * <p>表格生态系统的边缘需要物理提示。提示指示实现所期望的数据格式。例如，数据源可以表示它使用
 *   {@link java.sql.Timestamp} 类而不是使用 {@link java.time.LocalDateTime} 为逻辑时间戳生成值。有了这些
 *   信息，运行时就能够将生成的类转换为其内部数据格式。作为回报，数据接收器可以声明它从运行时使用的数据格式。
 *
 * Describes the data type of a value in the table ecosystem. Instances of this class can be used to
 * declare input and/or output types of operations.
 *
 * <p>The {@link DataType} class has two responsibilities: declaring a logical type and giving hints
 * about the physical representation of data to the planner. While the logical type is mandatory,
 * hints are optional but useful at the edges to other APIs.
 *
 * <p>The logical type is independent of any physical representation and is close to the "data type"
 * terminology of the SQL standard. See {@link org.apache.flink.table.types.logical.LogicalType} and
 * its subclasses for more information about available logical types and their properties.
 *
 * <p>Physical hints are required at the edges of the table ecosystem. Hints indicate the data
 * format that an implementation expects. For example, a data source could express that it produces
 * values for logical timestamps using a {@link java.sql.Timestamp} class instead of using {@link
 * java.time.LocalDateTime}. With this information, the runtime is able to convert the produced
 * class into its internal data format. In return, a data sink can declare the data format it
 * consumes from the runtime.
 *
 * @see DataTypes for a list of supported data types and instances of this class.
 */
@PublicEvolving
public abstract class DataType implements AbstractDataType<DataType>, Serializable {

    protected final LogicalType logicalType;

    protected final Class<?> conversionClass;

    DataType(LogicalType logicalType, @Nullable Class<?> conversionClass) {
        this.logicalType =
                Preconditions.checkNotNull(logicalType, "Logical type must not be null.");
        this.conversionClass =
                performEarlyClassValidation(
                        logicalType, ensureConversionClass(logicalType, conversionClass));
    }

    /**
     * Returns the corresponding logical type.
     *
     * @return a parameterized instance of {@link LogicalType}
     */
    public LogicalType getLogicalType() {
        return logicalType;
    }

    /**
     * Returns the corresponding conversion class for representing values. If no conversion class
     * was defined manually, the default conversion defined by the logical type is used.
     *
     * @see LogicalType#getDefaultConversion()
     * @return the expected conversion class
     */
    public Class<?> getConversionClass() {
        return conversionClass;
    }

    public abstract List<DataType> getChildren();

    public abstract <R> R accept(DataTypeVisitor<R> visitor);

    @Override
    public String toString() {
        return logicalType.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataType dataType = (DataType) o;
        return logicalType.equals(dataType.logicalType)
                && conversionClass.equals(dataType.conversionClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logicalType, conversionClass);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * 这个方法应该可以捕获最常见的错误。但是，由于我们不知道数据类型是用于输入声明还是用于输出声明，因此需要在更深层
     * 进行另一个验证。
     *
     * This method should catch the most common errors. However, another validation is required in
     * deeper layers as we don't know whether the data type is used for input or output declaration.
     */
    private static <C> Class<C> performEarlyClassValidation(
            LogicalType logicalType, Class<C> candidate) {

        if (candidate != null
                && !logicalType.supportsInputConversion(candidate)
                && !logicalType.supportsOutputConversion(candidate)) {
            throw new ValidationException(
                    String.format(
                            "Logical type '%s' does not support a conversion from or to class '%s'.",
                            logicalType.asSummaryString(), candidate.getName()));
        }
        return candidate;
    }

    private static Class<?> ensureConversionClass(
            LogicalType logicalType, @Nullable Class<?> clazz) {
        if (clazz == null) {
            return logicalType.getDefaultConversion();
        }
        return clazz;
    }
}
