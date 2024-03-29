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

package org.apache.flink.table.runtime.operators.match;

import org.apache.flink.api.common.functions.util.FunctionUtils;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.generated.GeneratedFunction;
import org.apache.flink.util.Collector;

import java.util.List;
import java.util.Map;

/**
 * 一个 {@link PatternProcessFunction} 包装器，用于将调用委托给生成的代码 {@link PatternProcessFunction}。
 *
 * A {@link PatternProcessFunction} wrapper to delegate invocation to the code generated {@link
 * PatternProcessFunction}.
 */
public class PatternProcessFunctionRunner extends PatternProcessFunction<RowData, RowData> {
    private static final long serialVersionUID = 1L;

    private final GeneratedFunction<PatternProcessFunction<RowData, RowData>> generatedFunction;
    private transient PatternProcessFunction<RowData, RowData> function;

    public PatternProcessFunctionRunner(
            GeneratedFunction<PatternProcessFunction<RowData, RowData>> generatedFunction) {
        this.generatedFunction = generatedFunction;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        this.function = generatedFunction.newInstance(getRuntimeContext().getUserCodeClassLoader());
        FunctionUtils.setFunctionRuntimeContext(function, getRuntimeContext());
        FunctionUtils.openFunction(function, parameters);
    }

    @Override
    public void processMatch(Map<String, List<RowData>> match, Context ctx, Collector<RowData> out)
            throws Exception {
        // J: 模式处理
        function.processMatch(match, ctx, out);
    }

    @Override
    public void close() throws Exception {
        FunctionUtils.closeFunction(function);
    }
}
