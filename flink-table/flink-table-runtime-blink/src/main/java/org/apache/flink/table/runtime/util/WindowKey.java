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

package org.apache.flink.table.runtime.util;

import org.apache.flink.table.data.RowData;

import java.util.Objects;

/**
 * {@link WindowKey} 结构表示键和窗口的组合。这主要用于小批量窗口操作符，窗口由窗口结束时间戳标识。
 *
 * The {@link WindowKey} structure represents a combination of key and window. This is mainly used
 * in the mini-batch window operators and window is identified by window end timestamp.
 */
public final class WindowKey {

    private long window;
    private RowData key;

    public WindowKey(long window, RowData key) {
        this.window = window;
        this.key = key;
    }

    /** Replace the currently stored key and window by the given new key and new window. */
    // 用给定的新密钥和新窗口替换当前存储的密钥和窗口。
    public WindowKey replace(long window, RowData key) {
        this.window = window;
        this.key = key;
        return this;
    }

    public long getWindow() {
        return window;
    }

    public RowData getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WindowKey windowKey = (WindowKey) o;
        return window == windowKey.window && Objects.equals(key, windowKey.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(window, key);
    }
}
