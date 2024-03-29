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

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.operators.TimestampedCollector;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Collector;

/**
 * ProcessFunction 将时间戳从 {@link RowData} 字段复制到
 * {@link org.apache.flink.streaming.runtime.streamrecord.StreamRecord}。
 *
 * ProcessFunction to copy a timestamp from a {@link RowData} field into the {@link
 * org.apache.flink.streaming.runtime.streamrecord.StreamRecord}.
 */
public class RowtimeProcessFunction extends ProcessFunction<RowData, RowData>
        implements ResultTypeQueryable<RowData> {

    private static final long serialVersionUID = 1L;

    private final int rowtimeIdx;
    private final int precision;
    private transient TypeInformation<RowData> returnType;

    public RowtimeProcessFunction(
            int rowtimeIdx, TypeInformation<RowData> returnType, int precision) {
        this.rowtimeIdx = rowtimeIdx;
        this.returnType = returnType;
        this.precision = precision;
    }

    @Override
    public void processElement(RowData value, Context ctx, Collector<RowData> out)
            throws Exception {
        long timestamp = value.getTimestamp(rowtimeIdx, precision).getMillisecond();
        ((TimestampedCollector<RowData>) out).setAbsoluteTimestamp(timestamp);
        out.collect(value);
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        return returnType;
    }
}
