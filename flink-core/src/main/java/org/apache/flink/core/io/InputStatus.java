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
 * {@code InputStatus}表示来自异步输入的数据可用性。当要求异步输入产生数据时，它会返回此状态以指示如何继续。
 * <p>当输入返回{@link InputStatus#NOTHING_AVAILABLE}时，这意味着此时没有可用的数据，但将来(很可能)
 *    会有更多可用的数据。异步输入通常提供注册一个<i>Notifier<i>或获得一个<i>Future<i>来表示新数据的可用性。
 *
 * <p>当输入返回{@link InputStatus#MORE_AVAILABLE}时，可以立即再次请求它产生更多的数据。为了提高效率，
 *    来自异步输入的阅读器可以绕过订阅通知程序或Future。
 *
 * <p>当输入返回{@link InputStatus#END_OF_INPUT}时，该输入将不再有数据可用。它已经到达了其有限数据的尽头。
 *
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
     * 指示有更多数据可用，可以立即再次调用输入以产生更多数据。
     *
     * Indicator that more data is available and the input can be called immediately again to
     * produce more data.
     */
    MORE_AVAILABLE,

    /**
     * 指示当前没有数据可用，但将来会有更多数据可用。
     *
     * Indicator that no data is currently available, but more data will be available in the future
     * again.
     */
    NOTHING_AVAILABLE,

    /** Indicator that the input has reached the end of data. */
    // 指示输入已经到达数据的末尾。
    END_OF_INPUT
}
