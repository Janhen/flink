/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.runtime.runners.python.table;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.python.env.PythonEnvironmentManager;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.functions.python.PythonFunctionInfo;
import org.apache.flink.table.runtime.typeutils.serializers.python.RowDataSerializer;
import org.apache.flink.table.runtime.utils.PythonTestUtils;
import org.apache.flink.table.types.logical.RowType;

import org.apache.beam.sdk.fn.data.FnDataReceiver;
import org.junit.Test;

import java.util.Collections;

import static org.apache.flink.table.runtime.utils.PythonTestUtils.createTestEnvironmentManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link RowDataPythonTableFunctionRunner}. These test that the input data type and
 * output data type are properly constructed.
 */
public class RowDataPythonTableFunctionRunnerTest
        extends AbstractPythonTableFunctionRunnerTest<RowData> {

    @Test
    public void testInputOutputDataTypeConstructedProperlyForSingleUDTF() throws Exception {
        final AbstractPythonTableFunctionRunner<RowData> runner = createUDTFRunner();

        // check input TypeSerializer
        TypeSerializer inputTypeSerializer = runner.getInputTypeSerializer();
        assertTrue(inputTypeSerializer instanceof RowDataSerializer);

        assertEquals(1, ((RowDataSerializer) inputTypeSerializer).getArity());
    }

    @Override
    public AbstractPythonTableFunctionRunner<RowData> createPythonTableFunctionRunner(
            PythonFunctionInfo pythonFunctionInfo, RowType inputType, RowType outputType)
            throws Exception {
        final FnDataReceiver<byte[]> dummyReceiver =
                input -> {
                    // ignore the execution results
                };

        final PythonEnvironmentManager environmentManager = createTestEnvironmentManager();

        return new RowDataPythonTableFunctionRunner(
                "testPythonRunner",
                dummyReceiver,
                pythonFunctionInfo,
                environmentManager,
                inputType,
                outputType,
                Collections.emptyMap(),
                PythonTestUtils.createMockFlinkMetricContainer());
    }
}
