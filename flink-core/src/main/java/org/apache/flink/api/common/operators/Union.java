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

package org.apache.flink.api.common.operators;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.functions.AbstractRichFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.operators.util.UserCodeClassWrapper;

import java.util.ArrayList;
import java.util.List;

/** This operator represents a Union between two inputs. */
// 该运算符表示两个输入之间的联合。
@Internal
public class Union<T> extends DualInputOperator<T, T, T, AbstractRichFunction> {

    /** Creates a new Union operator. */
    public Union(BinaryOperatorInformation<T, T, T> operatorInfo, String unionLocationName) {
        // we pass it an AbstractFunction, because currently all operators expect some form of UDF
        super(
                new UserCodeClassWrapper<AbstractRichFunction>(AbstractRichFunction.class),
                operatorInfo,
                "Union at " + unionLocationName);
    }

    public Union(Operator<T> input1, Operator<T> input2, String unionLocationName) {
        this(
                new BinaryOperatorInformation<T, T, T>(
                        input1.getOperatorInfo().getOutputType(),
                        input1.getOperatorInfo().getOutputType(),
                        input1.getOperatorInfo().getOutputType()),
                unionLocationName);
        setFirstInput(input1);
        setSecondInput(input2);
    }

    @Override
    protected List<T> executeOnCollections(
            List<T> inputData1,
            List<T> inputData2,
            RuntimeContext runtimeContext,
            ExecutionConfig executionConfig) {
        ArrayList<T> result = new ArrayList<T>(inputData1.size() + inputData2.size());
        result.addAll(inputData1);
        result.addAll(inputData2);
        return result;
    }
}
