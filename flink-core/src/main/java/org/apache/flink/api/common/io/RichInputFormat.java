/*
c * Licensed to the Apache Software Foundation (ASF) under one
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

package org.apache.flink.api.common.io;

import org.apache.flink.annotation.Public;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.core.io.InputSplit;

import java.io.IOException;

/**
 * 丰富输入格式的抽象存根实现。丰富的格式可以通过 {@link #getRuntimeContext()} 访问它们的运行时执行上下文。
 *
 * An abstract stub implementation for Rich input formats. Rich formats have access to their runtime
 * execution context via {@link #getRuntimeContext()}.
 */
@Public
public abstract class RichInputFormat<OT, T extends InputSplit> implements InputFormat<OT, T> {

    private static final long serialVersionUID = 1L;

    // --------------------------------------------------------------------------------------------
    //  Runtime context access
    // --------------------------------------------------------------------------------------------

    // J: transient
    private transient RuntimeContext runtimeContext;

    public void setRuntimeContext(RuntimeContext t) {
        this.runtimeContext = t;
    }

    public RuntimeContext getRuntimeContext() {
        if (this.runtimeContext != null) {
            return this.runtimeContext;
        } else {
            throw new IllegalStateException(
                    "The runtime context has not been initialized yet. Try accessing "
                            + "it in one of the other life cycle methods.");
        }
    }

    /**
     * 打开此 InputFormat 实例。每个并行实例调用此方法一次。应以这种方法分配资源。 （例如数据库连接、缓存等）
     *
     * Opens this InputFormat instance. This method is called once per parallel instance. Resources
     * should be allocated in this method. (e.g. database connections, cache, etc.)
     *
     * @see InputFormat
     * @throws IOException in case allocating the resources failed.
     */
    @PublicEvolving
    public void openInputFormat() throws IOException {
        // do nothing here, just for subclasses
    }

    /**
     * 关闭此 InputFormat 实例。每个并行实例调用此方法一次。 {@link #openInputFormat()} 期间分配的资源应在此
     * 方法中关闭。
     *
     * Closes this InputFormat instance. This method is called once per parallel instance. Resources
     * allocated during {@link #openInputFormat()} should be closed in this method.
     *
     * @see InputFormat
     * @throws IOException in case closing the resources failed
     */
    @PublicEvolving
    public void closeInputFormat() throws IOException {
        // do nothing here, just for subclasses
    }
}
