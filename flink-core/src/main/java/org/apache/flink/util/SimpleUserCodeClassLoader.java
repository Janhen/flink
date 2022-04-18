/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.util;

/**
 * 简单的{@link UserCodeClassLoader}实现，它假设所提供的类加载器永远不会被释放，因此也永远不会执行释放钩子。
 *
 * Simple {@link UserCodeClassLoader} implementation which assumes that the provided class loader
 * will never be released and, hence, will never execute the release hooks.
 */
public class SimpleUserCodeClassLoader implements UserCodeClassLoader {

    private final ClassLoader classLoader;

    private SimpleUserCodeClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public ClassLoader asClassLoader() {
        return classLoader;
    }

    @Override
    public void registerReleaseHookIfAbsent(String releaseHookName, Runnable releaseHook) {}

    public static SimpleUserCodeClassLoader create(ClassLoader classLoader) {
        return new SimpleUserCodeClassLoader(classLoader);
    }
}
