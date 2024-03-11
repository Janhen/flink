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

package org.apache.flink.core.classloading;

import org.apache.flink.configuration.CoreOptions;

import java.net.URL;
import java.util.Collections;

/**
 * 从子模块jar中加载所有类，除了显式列入白名单的包。
 *
 * <p>确保子模块中的类总是通过子模块classloader加载。(因此从子模块jar)，即使类也在类路径上(例如，在测试期间)，
 * org.apache.flink 中的所有类 包是子优先加载的。
 *
 * Loads all classes from the submodule jar, except for explicitly white-listed packages.
 *
 * <p>To ensure that classes from the submodule are always loaded through the submodule classloader
 * (and thus from the submodule jar), even if the classes are also on the classpath (e.g., during
 * tests), all classes from the "org.apache.flink" package are loaded child-first.
 *
 * <p>Classes related to logging (e.g., log4j) are loaded parent-first.
 *
 * <p>All other classes can only be loaded if they are either available in the submodule jar or the
 * bootstrap/app classloader (i.e., provided by the JDK).
 */
public class SubmoduleClassLoader extends ComponentClassLoader {
    public SubmoduleClassLoader(URL[] classpath, ClassLoader parentClassLoader) {
        super(
                classpath,
                parentClassLoader,
                CoreOptions.PARENT_FIRST_LOGGING_PATTERNS,
                new String[] {"org.apache.flink"},
                Collections.emptyMap());
    }
}
