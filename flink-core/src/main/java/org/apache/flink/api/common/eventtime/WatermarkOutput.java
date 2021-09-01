/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.apache.flink.api.common.eventtime;

import org.apache.flink.annotation.Public;

/** An output for watermarks. The output accepts watermarks and idleness (inactivity) status. */
// 水印的输出。输出接受水印和闲置(不活动)状态。
@Public
public interface WatermarkOutput {

    /**
     * 发出给定的水印。
     *
     * <p>发出水印还隐式地将流标记为<i>活动<i>，以先前标记为闲置结束。
     *
     * Emits the given watermark.
     *
     * <p>Emitting a watermark also implicitly marks the stream as <i>active</i>, ending previously
     * marked idleness.
     */
    void emitWatermark(Watermark watermark);

    /**
     * 将此输出标记为空闲，意味着下游操作不等待此输出的水印。
     *
     * <p>一旦发出下一个水印，输出就再次激活。
     *
     * Marks this output as idle, meaning that downstream operations do not wait for watermarks from
     * this output.
     *
     * <p>An output becomes active again as soon as the next watermark is emitted.
     */
    void markIdle();
}
