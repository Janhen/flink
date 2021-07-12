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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Consumer;

/**
 * 如果 {@link #loadClass(String, boolean)} 中发生异常，此类加载器将接受自定义处理程序
 *
 * This class loader accepts a custom handler if an exception occurs in {@link #loadClass(String,
 * boolean)}.
 */
public abstract class FlinkUserCodeClassLoader extends URLClassLoader {
    public static final Consumer<Throwable> NOOP_EXCEPTION_HANDLER = classLoadingException -> {};

    private final Consumer<Throwable> classLoadingExceptionHandler;

    protected FlinkUserCodeClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, NOOP_EXCEPTION_HANDLER);
    }

    protected FlinkUserCodeClassLoader(
            URL[] urls, ClassLoader parent, Consumer<Throwable> classLoadingExceptionHandler) {
        super(urls, parent);
        this.classLoadingExceptionHandler = classLoadingExceptionHandler;
    }

    @Override
    protected final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return loadClassWithoutExceptionHandling(name, resolve);
        } catch (Throwable classLoadingException) {
            classLoadingExceptionHandler.accept(classLoadingException);
            throw classLoadingException;
        }
    }

    /**
     * 与{@link #loadClass(String, boolean)}相同，但没有异常处理。
     *
     * <p>扩展具体的类装入器应该实现这个，而不是{@link #loadClass(String, boolean)}。
     *
     * Same as {@link #loadClass(String, boolean)} but without exception handling.
     *
     * <p>Extending concrete class loaders should implement this instead of {@link
     * #loadClass(String, boolean)}.
     */
    protected Class<?> loadClassWithoutExceptionHandling(String name, boolean resolve)
            throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }
}
