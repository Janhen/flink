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

package org.apache.flink.runtime.io.disk.iomanager;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/** A {@link RequestDoneCallback} that adds the memory segments to a blocking queue. */
// 将内存段添加到阻塞队列的 {@link RequestDoneCallback}。
public class QueuingCallback<T> implements RequestDoneCallback<T> {

    private final LinkedBlockingQueue<T> queue;

    public QueuingCallback(LinkedBlockingQueue<T> queue) {
        this.queue = queue;
    }

    @Override
    public void requestSuccessful(T buffer) {
        queue.add(buffer);
    }

    @Override
    public void requestFailed(T buffer, IOException e) {
        // the I/O error is recorded in the writer already
        queue.add(buffer);
    }
}
