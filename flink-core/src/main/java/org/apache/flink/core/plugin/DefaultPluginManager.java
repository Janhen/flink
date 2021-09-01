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

package org.apache.flink.core.plugin;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.VisibleForTesting;

import org.apache.flink.shaded.guava18.com.google.common.collect.Iterators;

import javax.annotation.concurrent.ThreadSafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/** Default implementation of {@link PluginManager}. */
/** J: 一些类加载的问题可参考 ...   flinkx 对于 yarn session 执行需要配置加载 。。。  */
@Internal
@ThreadSafe
public class DefaultPluginManager implements PluginManager {

    /**
     * 父类加载器到所有用于插件加载的类加载器。我们希望这是线程安全的。
     *
     * Parent-classloader to all classloader that are used for plugin loading. We expect that this
     * is thread-safe.
     */
    private final ClassLoader parentClassLoader;

    /** A collection of descriptions of all plugins known to this plugin manager. */
    // 这个插件管理器已知的所有插件的描述集合。
    private final Collection<PluginDescriptor> pluginDescriptors;

    // 应该总是从父ClassLoader解析的类的模式列表。
    /** List of patterns for classes that should always be resolved from the parent ClassLoader. */
    private final String[] alwaysParentFirstPatterns;

    @VisibleForTesting
    DefaultPluginManager() {
        parentClassLoader = null;
        pluginDescriptors = null;
        alwaysParentFirstPatterns = null;
    }

    public DefaultPluginManager(
            Collection<PluginDescriptor> pluginDescriptors, String[] alwaysParentFirstPatterns) {
        this(
                pluginDescriptors,
                DefaultPluginManager.class.getClassLoader(),
                alwaysParentFirstPatterns);
    }

    public DefaultPluginManager(
            Collection<PluginDescriptor> pluginDescriptors,
            ClassLoader parentClassLoader,
            String[] alwaysParentFirstPatterns) {
        this.pluginDescriptors = pluginDescriptors;
        this.parentClassLoader = parentClassLoader;
        this.alwaysParentFirstPatterns = alwaysParentFirstPatterns;
    }

    @Override
    public <P> Iterator<P> load(Class<P> service) {
        ArrayList<Iterator<P>> combinedIterators = new ArrayList<>(pluginDescriptors.size());
        for (PluginDescriptor pluginDescriptor : pluginDescriptors) {
            PluginLoader pluginLoader =
                    PluginLoader.create(
                            pluginDescriptor, parentClassLoader, alwaysParentFirstPatterns);
            combinedIterators.add(pluginLoader.load(service));
        }
        return Iterators.concat(combinedIterators.iterator());
    }

    @Override
    public String toString() {
        return "PluginManager{"
                + "parentClassLoader="
                + parentClassLoader
                + ", pluginDescriptors="
                + pluginDescriptors
                + ", alwaysParentFirstPatterns="
                + Arrays.toString(alwaysParentFirstPatterns)
                + '}';
    }
}
