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

import org.apache.flink.table.types.DataType;

import java.util.Objects;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * {@link TableSchema} 中定义的水印元数据。主要包括三个部分:
 *
 *   <li>rowtime 属性。
 *   <li>水印生成表达式的字符串表示。
 *   <li>表示水印生成表达式计算结果的数据类型。
 *
 * Watermark metadata defined in {@link TableSchema}. It mainly contains 3 parts:
 *
 * <ol>
 *   <li>the rowtime attribute.
 *   <li>the string representation of watermark generation expression.
 *   <li>the data type of the computation result of watermark generation expression.
 * </ol>
 */
public class WatermarkSpec {

    // 原始时间属性
    private final String rowtimeAttribute;

    // 水印表达式字符
    private final String watermarkExpressionString;

    // 水印表达式输出类型
    private final DataType watermarkExprOutputType;

    public WatermarkSpec(
            String rowtimeAttribute,
            String watermarkExpressionString,
            DataType watermarkExprOutputType) {
        this.rowtimeAttribute = checkNotNull(rowtimeAttribute);
        this.watermarkExpressionString = checkNotNull(watermarkExpressionString);
        this.watermarkExprOutputType = checkNotNull(watermarkExprOutputType);
    }

    /**
     * 返回行时间属性的名称，它可以是使用点分隔符的嵌套字段。引用属性必须出现在 {@link TableSchema} 中，且类型为
     * {@link org.apache.flink.table.types.logical.LogicalTypeRoot#TIMESTAMP_WITHOUT_TIME_ZONE}。
     *
     * Returns the name of rowtime attribute, it can be a nested field using dot separator. The
     * referenced attribute must be present in the {@link TableSchema} and of type {@link
     * org.apache.flink.table.types.logical.LogicalTypeRoot#TIMESTAMP_WITHOUT_TIME_ZONE}.
     */
    public String getRowtimeAttribute() {
        return rowtimeAttribute;
    }

    /**
     * Returns the string representation of watermark generation expression. The string
     * representation is a qualified SQL expression string (UDFs are expanded).
     */
    public String getWatermarkExpr() {
        return watermarkExpressionString;
    }

    /** Returns the data type of the computation result of watermark generation expression. */
    public DataType getWatermarkExprOutputType() {
        return watermarkExprOutputType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WatermarkSpec that = (WatermarkSpec) o;
        return Objects.equals(rowtimeAttribute, that.rowtimeAttribute)
                && Objects.equals(watermarkExpressionString, that.watermarkExpressionString)
                && Objects.equals(watermarkExprOutputType, that.watermarkExprOutputType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowtimeAttribute, watermarkExpressionString, watermarkExprOutputType);
    }

    @Override
    public String toString() {
        return "rowtime: '"
                + rowtimeAttribute
                + '\''
                + ", watermark: '"
                + watermarkExpressionString
                + '\'';
    }
}
