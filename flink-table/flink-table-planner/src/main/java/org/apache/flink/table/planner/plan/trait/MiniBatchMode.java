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

package org.apache.flink.table.planner.plan.trait;

/** The type of mini-batch interval: rowtime or proctime. */
public enum MiniBatchMode {

    /**
     * {@code ProcTime}模式下的操作符需要在ProcTime间隔中发出水印，即启用了mini-batch的无界组agg。
     *
     * An operator in {@code #ProcTime} mode requires watermarks emitted in proctime interval, i.e.,
     * unbounded group agg with mini-batch enabled.
     */
    ProcTime,

    /**
     * {@code RowTime}模式下的操作符需要从元素中提取水印，并在RowTime间隔内发出，例如，window, window join…
     *
     * An operator in {@code #RowTime} mode requires watermarks extracted from elements, and emitted
     * in rowtime interval, e.g., window, window join...
     */
    RowTime,

    /** Default value, meaning no mini-batch interval is required. */
    None
}
