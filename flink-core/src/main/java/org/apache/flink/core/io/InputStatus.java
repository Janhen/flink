/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.core.io;

import org.apache.flink.annotation.PublicEvolving;

/**
 * An {@code InputStatus} indicates the availability of data from an asynchronous input. When asking
 * an asynchronous input to produce data, it returns this status to indicate how to proceed.
 *
 * <p>When the input returns {@link InputStatus#NOTHING_AVAILABLE} it means that no data is
 * available at this time, but more will (most likely) be available in the future. The asynchronous
 * input will typically offer to register a <i>Notifier</i> or to obtain a <i>Future</i> that will
 * signal the availability of new data.
 *
 * <p>When the input returns {@link InputStatus#MORE_AVAILABLE}, it can be immediately asked again
 * to produce more data. That readers from the asynchronous input can bypass subscribing to a
 * Notifier or a Future for efficiency.
 *
 * <p>When the input returns {@link InputStatus#END_OF_INPUT}, then no data will be available again
 * from this input. It has reached the end of its bounded data.
 */
@PublicEvolving
public enum InputStatus {

    /**
     * 指示有更多数据可用，并且可以立即再次调用输入以产生更多数据。
     *
     * Indicator that more data is available and the input can be called immediately again to
     * produce more data.
     */
    MORE_AVAILABLE,

    /**
     * 指示当前没有可用的数据，但将来会有更多的数据可用。
     *
     * Indicator that no data is currently available, but more data will be available in the future
     * again.
     */
    NOTHING_AVAILABLE,

    /** Indicator that all persisted data of the data exchange has been successfully restored. */
    // 指示数据交换的所有持久化数据已成功恢复。
    END_OF_RECOVERY,

    /** Indicator that the input has reached the end of data. */
    // 指示输入已到达数据的末尾。
    END_OF_INPUT
}
