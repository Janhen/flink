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

import org.apache.flink.annotation.Public;

import java.io.IOException;
import java.io.InputStream;

/**
 * 用于在{@link FileSystem}上的文件的数据输入流的接口。
 * <p>这个扩展了{@link java.io.InputStream}的方法用于访问流的{@link #getPos()}当前位置和
 * {@link #seek(long) seek}到期望的位置。
 *
 * Interface for a data input stream to a file on a {@link FileSystem}.
 *
 * <p>This extends the {@link java.io.InputStream} with methods for accessing the stream's {@link
 * #getPos() current position} and {@link #seek(long) seeking} to a desired position.
 */
@Public
public abstract class FSDataInputStream extends InputStream {

    /**
     * 查找从文件开始到给定的偏移量。下一个read()将来自该位置。找不到 stream 的尽头。
     *
     * Seek to the given offset from the start of the file. The next read() will be from that
     * location. Can't seek past the end of the stream.
     *
     * @param desired the desired offset
     * @throws IOException Thrown if an error occurred while seeking inside the input stream.
     */
    public abstract void seek(long desired) throws IOException;

    /**
     * Gets the current position in the input stream.
     *
     * @return current position in the input stream
     * @throws IOException Thrown if an I/O error occurred in the underlying stream implementation
     *     while accessing the stream's position.
     */
    public abstract long getPos() throws IOException;
}
