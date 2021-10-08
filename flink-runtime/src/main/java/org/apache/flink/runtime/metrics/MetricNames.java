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

package org.apache.flink.runtime.metrics;

/** Collection of metric names. */
public class MetricNames {
    private MetricNames() {}

    public static final String SUFFIX_RATE = "PerSecond";

    // 记录数
    public static final String IO_NUM_RECORDS_IN = "numRecordsIn";
    public static final String IO_NUM_RECORDS_OUT = "numRecordsOut";
    public static final String IO_NUM_RECORDS_IN_RATE = IO_NUM_RECORDS_IN + SUFFIX_RATE;
    public static final String IO_NUM_RECORDS_OUT_RATE = IO_NUM_RECORDS_OUT + SUFFIX_RATE;

    // 记录大小
    public static final String IO_NUM_BYTES_IN = "numBytesIn";
    public static final String IO_NUM_BYTES_OUT = "numBytesOut";
    public static final String IO_NUM_BYTES_IN_RATE = IO_NUM_BYTES_IN + SUFFIX_RATE;
    public static final String IO_NUM_BYTES_OUT_RATE = IO_NUM_BYTES_OUT + SUFFIX_RATE;

    // 缓存
    public static final String IO_NUM_BUFFERS_IN = "numBuffersIn";
    public static final String IO_NUM_BUFFERS_OUT = "numBuffersOut";
    public static final String IO_NUM_BUFFERS_OUT_RATE = IO_NUM_BUFFERS_OUT + SUFFIX_RATE;

    public static final String IO_CURRENT_INPUT_WATERMARK = "currentInputWatermark";
    @Deprecated public static final String IO_CURRENT_INPUT_1_WATERMARK = "currentInput1Watermark";
    @Deprecated public static final String IO_CURRENT_INPUT_2_WATERMARK = "currentInput2Watermark";
    public static final String IO_CURRENT_INPUT_WATERMARK_PATERN = "currentInput%dWatermark";
    public static final String IO_CURRENT_OUTPUT_WATERMARK = "currentOutputWatermark";

    public static final String NUM_RUNNING_JOBS = "numRunningJobs";
    // 当前可用的 task 插槽数量
    public static final String TASK_SLOTS_AVAILABLE = "taskSlotsAvailable";
    public static final String TASK_SLOTS_TOTAL = "taskSlotsTotal";
    public static final String NUM_REGISTERED_TASK_MANAGERS = "numRegisteredTaskManagers";

    public static final String NUM_RESTARTS = "numRestarts";

    @Deprecated public static final String FULL_RESTARTS = "fullRestarts";

    // 当前已使用的内存
    public static final String MEMORY_USED = "Used";
    // 当前 committed 的内存
    public static final String MEMORY_COMMITTED = "Committed";
    public static final String MEMORY_MAX = "Max";

    // 是否产生背压
    public static final String IS_BACK_PRESSURED = "isBackPressured";

    // 检查点对齐时间
    public static final String CHECKPOINT_ALIGNMENT_TIME = "checkpointAlignmentTime";
    public static final String CHECKPOINT_START_DELAY_TIME = "checkpointStartDelayNanos";

    public static final String START_WORKER_FAILURE_RATE = "startWorkFailure" + SUFFIX_RATE;

    public static String currentInputWatermarkName(int index) {
        return String.format(IO_CURRENT_INPUT_WATERMARK_PATERN, index);
    }

    // 每秒钟空闲的毫秒数
    public static final String TASK_IDLE_TIME = "idleTimeMs" + SUFFIX_RATE;
    // 每秒钟busy 的毫秒数
    public static final String TASK_BUSY_TIME = "busyTimeMs" + SUFFIX_RATE;
    // 每秒钟背压的毫秒数
    public static final String TASK_BACK_PRESSURED_TIME = "backPressuredTimeMs" + SUFFIX_RATE;
}
