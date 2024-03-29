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

package org.apache.flink.util;

import java.util.function.Consumer;

/**
 * 这个类捕获包装的可运行对象中的所有{@link Throwable Throwables}。当包装的可运行对象提交给
 * UncaughtExceptionHandler不可用的执行器时，这个类很有用。
 *
 * This class catches all the {@link Throwable Throwables} from the wrapped runnable. This class is
 * useful when the wrapped runnable is submitted to an executor where the UncaughtExceptionHandler
 * is not usable.
 */
public class ThrowableCatchingRunnable implements Runnable {
    private final Consumer<Throwable> exceptionHandler;
    private final Runnable runnable;

    public ThrowableCatchingRunnable(Consumer<Throwable> exceptionHandler, Runnable runnable) {
        this.exceptionHandler = exceptionHandler;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } catch (Throwable t) {
            exceptionHandler.accept(t);
        }
    }
}
