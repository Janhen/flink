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

package org.apache.flink.table.sinks;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.operators.collect.CollectResultIterator;
import org.apache.flink.streaming.api.operators.collect.CollectSinkOperator;
import org.apache.flink.streaming.api.operators.collect.CollectSinkOperatorFactory;
import org.apache.flink.streaming.api.operators.collect.CollectStreamSink;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.internal.SelectTableSink;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;

import java.util.UUID;

/**
 * A {@link SelectTableSink} for streaming select job.
 *
 * <p><strong>NOTES:</strong> Currently, only insert changes is supported. Once FLINK-16998 is
 * finished, all kinds of changes will be supported.
 */
public class StreamSelectTableSink implements AppendStreamTableSink<Row>, SelectTableSink {

    private final TableSchema tableSchema;
    private final CollectSinkOperatorFactory<Row> factory;
    private final CollectResultIterator<Row> iterator;

    public StreamSelectTableSink(TableSchema tableSchema) {
        this.tableSchema =
                SelectTableSinkSchemaConverter.convertTimeAttributeToRegularTimestamp(tableSchema);

        TypeSerializer<Row> typeSerializer =
                this.tableSchema.toRowType().createSerializer(new ExecutionConfig());
        String accumulatorName = "tableResultCollect_" + UUID.randomUUID();

        this.factory = new CollectSinkOperatorFactory<>(typeSerializer, accumulatorName);
        CollectSinkOperator<Row> operator = (CollectSinkOperator<Row>) factory.getOperator();
        this.iterator =
                new CollectResultIterator<>(
                        operator.getOperatorIdFuture(), typeSerializer, accumulatorName);
    }

    @Override
    public DataType getConsumedDataType() {
        return tableSchema.toRowDataType();
    }

    @Override
    public TableSchema getTableSchema() {
        return tableSchema;
    }

    @Override
    public DataStreamSink<?> consumeDataStream(DataStream<Row> dataStream) {
        CollectStreamSink<Row> sink = new CollectStreamSink<>(dataStream, factory);
        dataStream.getExecutionEnvironment().addOperator(sink.getTransformation());
        return sink.name("Streaming select table sink");
    }

    @Override
    public void setJobClient(JobClient jobClient) {
        iterator.setJobClient(jobClient);
    }

    @Override
    public CloseableIterator<Row> getResultIterator() {
        return iterator;
    }
}
