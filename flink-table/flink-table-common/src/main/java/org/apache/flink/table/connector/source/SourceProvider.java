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

package org.apache.flink.table.connector.source;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.table.data.RowData;

/**
 * {@link Source} 实例的提供者作为 {@link ScanTableSource} 的运行时实现。
 *
 * Provider of a {@link Source} instance as a runtime implementation for {@link ScanTableSource}.
 */
@PublicEvolving
public interface SourceProvider extends ScanTableSource.ScanRuntimeProvider {

    /** Helper method for creating a static provider. */
    static SourceProvider of(Source<RowData, ?, ?> source) {
        return new SourceProvider() {
            @Override
            public Source<RowData, ?, ?> createSource() {
                return source;
            }

            @Override
            public boolean isBounded() {
                return Boundedness.BOUNDED.equals(source.getBoundedness());
            }
        };
    }

    /** Creates a {@link Source} instance. */
    // 创建一个 {@link Source} 实例
    Source<RowData, ?, ?> createSource();
}
