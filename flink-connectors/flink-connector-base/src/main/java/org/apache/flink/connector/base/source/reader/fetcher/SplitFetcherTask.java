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

package org.apache.flink.connector.base.source.reader.fetcher;

import java.io.IOException;

/** An interface similar to {@link Runnable} but allows throwing exceptions and wakeup. */
// 类似{@link Runnable}的接口，但允许抛出异常和唤醒。
public interface SplitFetcherTask {

    /**
     * 运行逻辑。此方法允许在唤醒时抛出中断的异常，但实现不需要这样做。最好是优雅地完成工作并返回一个布尔值来指示是否
     * 已经完成了所有的工作，还是需要更多的调用。
     *
     * Run the logic. This method allows throwing an interrupted exception on wakeup, but the
     * implementation does not have to. It is preferred to finish the work elegantly and return a
     * boolean to indicate whether all the jobs have been done or more invocation is needed.
     *
     * @return whether the runnable has successfully finished running.
     * @throws IOException when the performed I/O operation fails.
     */
    boolean run() throws IOException;

    /** Wake up the running thread. */
    // 唤醒正在运行的线程。
    void wakeUp();
}
