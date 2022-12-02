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

package org.apache.flink.table.runtime.operators.join.interval;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.operators.join.FlinkJoinType;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;

/** The function to execute row(event) time interval stream inner-join. */
// 执行行（事件）时间间隔流内连接的函数。
public final class RowTimeIntervalJoin extends TimeIntervalJoin {

    private static final long serialVersionUID = -2923709329817468698L;

    private final int leftTimeIdx;
    private final int rightTimeIdx;

    public RowTimeIntervalJoin(
            FlinkJoinType joinType,
            long leftLowerBound,
            long leftUpperBound,
            long allowedLateness,             // J: 运行延迟
            InternalTypeInfo<RowData> leftType,
            InternalTypeInfo<RowData> rightType,
            IntervalJoinFunction joinFunc,
            int leftTimeIdx,
            int rightTimeIdx) {
        super(
                joinType,
                leftLowerBound,
                leftUpperBound,
                allowedLateness,
                leftType,
                rightType,
                joinFunc);
        this.leftTimeIdx = leftTimeIdx;
        this.rightTimeIdx = rightTimeIdx;
    }

    /**
     * 获取接收行和发出行之间的最大间隔（作为连接结果的一部分）。这是水印需要被阻止的时间间隔。
     *
     * Get the maximum interval between receiving a row and emitting it (as part of a joined
     * result). This is the time interval by which watermarks need to be held back.
     *
     * @return the maximum delay for the outputs
     */
    public long getMaxOutputDelay() {
        return Math.max(leftRelativeSize, rightRelativeSize) + allowedLateness;
    }

    @Override
    void updateOperatorTime(Context ctx) {
        leftOperatorTime =
                ctx.timerService().currentWatermark() > 0
                        ? ctx.timerService().currentWatermark()
                        : 0L;
        // We may set different operator times in the future.
        rightOperatorTime = leftOperatorTime;
    }

    @Override
    long getTimeForLeftStream(Context ctx, RowData row) {
        return row.getLong(leftTimeIdx);
    }

    @Override
    long getTimeForRightStream(Context ctx, RowData row) {
        return row.getLong(rightTimeIdx);
    }

    @Override
    void registerTimer(Context ctx, long cleanupTime) {
        // Maybe we can register timers for different streams in the future.
        ctx.timerService().registerEventTimeTimer(cleanupTime);
    }
}
