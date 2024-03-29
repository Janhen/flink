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
import org.apache.flink.table.functions.BuiltInFunctionDefinitions;
import org.apache.flink.table.functions.SpecializedFunction.SpecializedContext;

import javax.annotation.Nullable;

/** Implementation of {@link BuiltInFunctionDefinitions#IF_NULL}. */
// {@link BuiltInFunctionDefinitions#IF_NULL} 的实现。
@Internal
public class IfNullFunction extends BuiltInScalarFunction {

    public IfNullFunction(SpecializedContext context) {
        super(BuiltInFunctionDefinitions.IF_NULL, context);
    }

    public @Nullable Object eval(Object input, Object nullReplacement) {
        // 依赖于通过输入类型策略的类型转换功能来确定公共数据类型
        // we rely on the casting functionality via input type strategy
        // to determine the common data type
        if (input == null) {
            return nullReplacement;
        }
        return input;
    }
}
