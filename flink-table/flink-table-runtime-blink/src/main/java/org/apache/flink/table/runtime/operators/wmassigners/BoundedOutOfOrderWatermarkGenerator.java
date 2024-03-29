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

package org.apache.flink.table.runtime.operators.wmassigners;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.generated.WatermarkGenerator;

import javax.annotation.Nullable;

/**
 * 行时间属性的水印生成器，这些属性在有限的时间间隔内乱序。
 *
 * <p>发出水印，它是观察到的时间戳减去指定的延迟。
 *
 * A watermark generator for rowtime attributes which are out-of-order by a bounded time interval.
 *
 * <p>Emits watermarks which are the observed timestamp minus the specified delay.
 */
public class BoundedOutOfOrderWatermarkGenerator extends WatermarkGenerator {

    private static final long serialVersionUID = 1L;
    private final long delay;
    private final int rowtimeIndex;

    /**
     * @param rowtimeIndex the field index of rowtime attribute, the value of rowtime should never
     *     be null.
     * @param delay The delay by which watermarks are behind the observed timestamp.
     */
    public BoundedOutOfOrderWatermarkGenerator(int rowtimeIndex, long delay) {
        this.delay = delay;
        this.rowtimeIndex = rowtimeIndex;
    }

    @Nullable
    @Override
    public Long currentWatermark(RowData row) {
        return row.getLong(rowtimeIndex) - delay;
    }
}
