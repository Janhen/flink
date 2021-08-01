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

package org.apache.flink.table.sources;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 定义外部流表并提供对其数据的读访问。
 *
 * Defines an external stream table and provides read access to its data.
 *
 * @param <T> Type of the {@link DataStream} created by this {@link TableSource}.
 */
public interface StreamTableSource<T> extends TableSource<T> {

    /**
     * 如果这是一个有界源返回 true，如果这是一个无界源返回 false。默认是不受兼容性限制的。
     *
     * Returns true if this is a bounded source, false if this is an unbounded source. Default is
     * unbounded for compatibility.
     */
    default boolean isBounded() {
        return false;
    }

    /**
     * 返回表的数据作为一个{@link DataStream}。
     *
     * <p>注意:此方法只用于内部定义{@link TableSource}。不要在表API程序中使用它。
     *
     * Returns the data of the table as a {@link DataStream}.
     *
     * <p>NOTE: This method is for internal use only for defining a {@link TableSource}. Do not use
     * it in Table API programs.
     */
    DataStream<T> getDataStream(StreamExecutionEnvironment execEnv);
}
