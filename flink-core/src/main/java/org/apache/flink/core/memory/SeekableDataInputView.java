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

package org.apache.flink.core.memory;

import org.apache.flink.annotation.PublicEvolving;

/**
 * 将{@link DataInputView}标记为可搜索的接口。可搜索视图可以设置读取的位置。
 *
 * Interface marking a {@link DataInputView} as seekable. Seekable views can set the position where
 * they read from.
 */
@PublicEvolving
public interface SeekableDataInputView extends DataInputView {

    /**
     * Sets the read pointer to the given position.
     *
     * @param position The new read position.
     */
    void setReadPosition(long position);
}
