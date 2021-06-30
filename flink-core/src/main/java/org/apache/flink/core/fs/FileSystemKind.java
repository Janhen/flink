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

package org.apache.flink.core.fs;

import org.apache.flink.annotation.PublicEvolving;

/** An enumeration defining the kind and characteristics of a {@link FileSystem}. */
// 定义{@link FileSystem}的类型和特征的枚举。
@PublicEvolving
public enum FileSystemKind {

    /** An actual file system, with files and directories. */
    // 一个具有文件和目录的实际文件系统。
    FILE_SYSTEM,

    /**
     * 一个对象存储。文件对应于对象。没有真正的目录，但是可以通过文件的分级命名来模仿类似目录的结构。
     *
     * An Object store. Files correspond to objects. There are not really directories, but a
     * directory-like structure may be mimicked by hierarchical naming of files.
     */
    OBJECT_STORE
}
