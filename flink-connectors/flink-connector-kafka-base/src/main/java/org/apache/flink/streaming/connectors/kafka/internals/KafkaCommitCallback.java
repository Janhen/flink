/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.connectors.kafka.internals;

import org.apache.flink.annotation.Internal;

/**
 * 源操作符可以实现的回调接口，在提交请求完成时触发自定义操作，通常应该从检查点完成事件触发。
 *
 * A callback interface that the source operator can implement to trigger custom actions when a
 * commit request completes, which should normally be triggered from checkpoint complete event.
 */
@Internal
public interface KafkaCommitCallback {

    /**
     * 用户可以实现的回调方法，以提供提交请求完成的异步处理。当发送到服务器的提交请求被确认无误时，将调用此方法。
     *
     * A callback method the user can implement to provide asynchronous handling of commit request
     * completion. This method will be called when the commit request sent to the server has been
     * acknowledged without error.
     */
    void onSuccess();

    /**
     * 用户可以实现的回调方法，以提供提交请求失败的异步处理。当提交请求失败时，将调用此方法。
     *
     * A callback method the user can implement to provide asynchronous handling of commit request
     * failure. This method will be called when the commit request failed.
     *
     * @param cause Kafka commit failure cause returned by kafka client
     */
    void onException(Throwable cause);
}
