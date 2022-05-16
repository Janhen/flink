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

package org.apache.flink.table.runtime.functions.scalar;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.connector.source.abilities.SupportsSourceWatermark;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.functions.BuiltInFunctionDefinitions;
import org.apache.flink.table.functions.SpecializedFunction.SpecializedContext;

import javax.annotation.Nullable;

/** Implementation of {@link BuiltInFunctionDefinitions#SOURCE_WATERMARK}. */
// {@link BuiltInFunctionDefinitions#SOURCE_WATERMARK}的实现。
@Internal
public class SourceWatermarkFunction extends BuiltInScalarFunction {

    // SOURCE_WATERMARK()是一个声明性标记函数，没有具体的运行时实现。它只能作为一个单独的表达式在
    // CREATE TABLE DDL中的WATERMARK FOR子句中使用。声明将被下推到实现'%s'接口的表源中。
    // 之后源将发出系统定义的水印。请检查文档连接器是否支持源水印。
    public static final String ERROR_MESSAGE =
            String.format(
                    "SOURCE_WATERMARK() is a declarative marker function and doesn't have concrete "
                            + "runtime implementation. It can only be used as a single expression in a "
                            + "WATERMARK FOR clause in the CREATE TABLE DDL. The declaration will be "
                            + "pushed down into a table source that implements the '%s' interface. "
                            + "The source will emit system-defined watermarks afterwards. Please "
                            + "check the documentation whether the connector supports source watermarks.",
                    SupportsSourceWatermark.class.getName());

    private static final long serialVersionUID = 1L;

    public SourceWatermarkFunction(SpecializedContext context) {
        super(BuiltInFunctionDefinitions.SOURCE_WATERMARK, context);
    }

    public @Nullable TimestampData eval() {
        throw new TableException(ERROR_MESSAGE);
    }
}
