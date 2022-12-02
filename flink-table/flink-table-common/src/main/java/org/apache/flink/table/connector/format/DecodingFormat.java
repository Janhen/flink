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
import org.apache.flink.table.connector.source.abilities.SupportsReadingMetadata;
import org.apache.flink.table.types.DataType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link DynamicTableSource} 的 {@link Format} 用于读取行。
 *
 * A {@link Format} for a {@link DynamicTableSource} for reading rows.
 *
 * @param <I> runtime interface needed by the table source
 */
@PublicEvolving
public interface DecodingFormat<I> extends Format {

    /**
     * 创建配置为生成给定数据类型的数据的运行时解码器实现。
     *
     * Creates runtime decoder implementation that is configured to produce data of the given data
     * type.
     */
    I createRuntimeDecoder(DynamicTableSource.Context context, DataType physicalDataType);

    /**
     * 返回元数据键及其对应的数据类型的映射，这些数据类型可由这种格式生成用于读取。默认情况下，此方法返回一个空映射。
     *
     * <p>元数据列向表的模式添加额外的列。解码格式负责在生成的行末尾添加请求的元数据列。
     *
     * <p>请参见{@link SupportsReadingMetadata}了解更多信息。
     *
     * <p>注意:该方法仅在外部{@link DynamicTableSource}实现了{@link SupportsReadingMetadata}并在
     *   {@link SupportsReadingMetadata#listReadableMetadata()}中调用此方法时使用。
     *
     * Returns the map of metadata keys and their corresponding data types that can be produced by
     * this format for reading. By default, this method returns an empty map.
     *
     * <p>Metadata columns add additional columns to the table's schema. A decoding format is
     * responsible to add requested metadata columns at the end of produced rows.
     *
     * <p>See {@link SupportsReadingMetadata} for more information.
     *
     * <p>Note: This method is only used if the outer {@link DynamicTableSource} implements {@link
     * SupportsReadingMetadata} and calls this method in {@link
     * SupportsReadingMetadata#listReadableMetadata()}.
     */
    default Map<String, DataType> listReadableMetadata() {
        return Collections.emptyMap();
    }

    /**
     * 提供生成的行必须包含作为附加元数据列的元数据键的列表。默认情况下，如果定义了元数据键，此方法将抛出异常。
     *
     * <p>请参见{@link SupportsReadingMetadata}了解更多信息。
     *
     * Provides a list of metadata keys that the produced row must contain as appended metadata
     * columns. By default, this method throws an exception if metadata keys are defined.
     *
     * <p>See {@link SupportsReadingMetadata} for more information.
     *
     * <p>Note: This method is only used if the outer {@link DynamicTableSource} implements {@link
     * SupportsReadingMetadata} and calls this method in {@link
     * SupportsReadingMetadata#applyReadableMetadata(List, DataType)}.
     */
    @SuppressWarnings("unused")
    default void applyReadableMetadata(List<String> metadataKeys) {
        throw new UnsupportedOperationException(
                "A decoding format must override this method to apply metadata keys.");
    }
}
