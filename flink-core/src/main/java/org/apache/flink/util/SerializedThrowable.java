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

package org.apache.flink.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * 实用程序类，用于处理已序列化的用户定义 Throwable 类型(例如在 RPCActor 通信期间)，但不能用默认的类装入器解析这些类型。
 *
 * <p>此异常在消息和堆栈跟踪方面模仿原始异常，并以序列化的形式包含原始异常。可以通过提供适当的类装入器重新获得原始异常。
 *
 * Utility class for dealing with user-defined Throwable types that are serialized (for example
 * during RPC/Actor communication), but cannot be resolved with the default class loader.
 *
 * <p>This exception mimics the original exception with respect to message and stack trace, and
 * contains the original exception in serialized form. The original exception can be re-obtained by
 * supplying the appropriate class loader.
 */
public class SerializedThrowable extends Exception implements Serializable {

    private static final long serialVersionUID = 7284183123441947635L;

    /** The original exception in serialized form. */
    // 序列化形式的原始异常。
    private final byte[] serializedException;

    /** Name of the original error class. */
    // 原始错误类的名称。
    private final String originalErrorClassName;

    /** The original stack trace, to be printed. */
    // 要打印的原始堆栈跟踪。
    private final String fullStringifiedStackTrace;

    /**
     * 原始异常，不通过序列化传输，因为类可能不是系统类装入器的一部分。另外，我们要确保缓存的引用不会阻止异常类的卸载。
     *
     * The original exception, not transported via serialization, because the class may not be part
     * of the system class loader. In addition, we make sure our cached references to not prevent
     * unloading the exception class.
     */
    private transient WeakReference<Throwable> cachedException;

    /**
     * 创建一个新的 SerializedThrowable
     *
     * Create a new SerializedThrowable.
     *
     * @param exception The exception to serialize.
     */
    public SerializedThrowable(Throwable exception) {
        this(exception, new HashSet<>());
    }

    private SerializedThrowable(Throwable exception, Set<Throwable> alreadySeen) {
        super(getMessageOrError(exception));

        if (!(exception instanceof SerializedThrowable)) {
            // serialize and memoize the original message
            // 序列化并记住原始信息
            byte[] serialized;
            try {
                serialized = InstantiationUtil.serializeObject(exception);
            } catch (Throwable t) {
                serialized = null;
            }
            this.serializedException = serialized;
            this.cachedException = new WeakReference<>(exception);

            // record the original exception's properties (name, stack prints)
            // 记录原始异常的属性(名称、堆栈打印)
            this.originalErrorClassName = exception.getClass().getName();
            this.fullStringifiedStackTrace = ExceptionUtils.stringifyException(exception);

            // mimic the original exception's stack trace
            // 模拟原始异常的堆栈跟踪
            setStackTrace(exception.getStackTrace());

            // mimic the original exception's cause
            // 模仿原始异常的原因
            if (exception.getCause() == null) {
                initCause(null);
            } else {
                // exception causes may by cyclic, so we truncate the cycle when we find it
                if (alreadySeen.add(exception)) {
                    // we are not in a cycle, yet
                    initCause(new SerializedThrowable(exception.getCause(), alreadySeen));
                }
            }

        } else {
            // copy from that serialized throwable
            // 从那个序列化的 throwable 中复制
            SerializedThrowable other = (SerializedThrowable) exception;
            this.serializedException = other.serializedException;
            this.originalErrorClassName = other.originalErrorClassName;
            this.fullStringifiedStackTrace = other.fullStringifiedStackTrace;
            this.cachedException = other.cachedException;
            this.setStackTrace(other.getStackTrace());
            this.initCause(other.getCause());
        }
    }

    public Throwable deserializeError(ClassLoader classloader) {
        if (serializedException == null) {
            // failed to serialize the original exception
            // return this SerializedThrowable as a stand in
            return this;
        }

        Throwable cached = cachedException == null ? null : cachedException.get();
        if (cached == null) {
            try {
                cached = InstantiationUtil.deserializeObject(serializedException, classloader);
                cachedException = new WeakReference<>(cached);
            } catch (Throwable t) {
                // something went wrong
                // return this SerializedThrowable as a stand in
                return this;
            }
        }
        return cached;
    }

    public String getOriginalErrorClassName() {
        return originalErrorClassName;
    }

    public byte[] getSerializedException() {
        return serializedException;
    }

    public String getFullStringifiedStackTrace() {
        return fullStringifiedStackTrace;
    }

    // ------------------------------------------------------------------------
    //  Override the behavior of Throwable
    // ------------------------------------------------------------------------

    @Override
    public void printStackTrace(PrintStream s) {
        s.print(fullStringifiedStackTrace);
        s.flush();
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        s.print(fullStringifiedStackTrace);
        s.flush();
    }

    @Override
    public String toString() {
        String message = getLocalizedMessage();
        return (message != null)
                ? (originalErrorClassName + ": " + message)
                : originalErrorClassName;
    }

    // ------------------------------------------------------------------------
    //  Static utilities
    // ------------------------------------------------------------------------

    public static Throwable get(Throwable serThrowable, ClassLoader loader) {
        if (serThrowable instanceof SerializedThrowable) {
            return ((SerializedThrowable) serThrowable).deserializeError(loader);
        } else {
            return serThrowable;
        }
    }

    private static String getMessageOrError(Throwable error) {
        try {
            return error.getMessage();
        } catch (Throwable t) {
            return "(failed to get message)";
        }
    }
}
