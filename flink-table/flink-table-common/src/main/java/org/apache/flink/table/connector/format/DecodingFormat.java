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

package org.apache.flink.table.connector.format;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.types.DataType;

/**
 * A {@link Format} for a {@link DynamicTableSource} for reading rows.
 *
 * @param <I> runtime interface needed by the table source
 */
@PublicEvolving
public interface DecodingFormat<I> extends Format {

    /**
     * 创建运行时解码器实现，该实现配置为生成给定数据类型的数据。
     *
     * Creates runtime decoder implementation that is configured to produce data of the given data
     * type.
     */
    I createRuntimeDecoder(DynamicTableSource.Context context, DataType producedDataType);
}
