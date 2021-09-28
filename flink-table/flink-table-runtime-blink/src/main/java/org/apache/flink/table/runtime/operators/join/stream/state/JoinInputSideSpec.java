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

package org.apache.flink.table.runtime.operators.join.stream.state;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;

import javax.annotation.Nullable;

import java.io.Serializable;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * {@link JoinInputSideSpec}是ap规范，它描述了Join的输入端信息。
 *
 * J: 根据输入侧不同的特点(是否包含唯一键、关联键是否包含唯一键), 设计几种不同的状态存储结构，
 *   JoinKeyContainsUniqueKey, InputSideHasUniqueKey 和 InputSideHasNoUniqueKey
 *
 * The {@link JoinInputSideSpec} is ap specification which describes input side information of a
 * Join.
 */
public class JoinInputSideSpec implements Serializable {
    private static final long serialVersionUID = 3178408082297179959L;

    // 输入端 hash 唯一键
    private final boolean inputSideHasUniqueKey;
    // join 的键包含唯一键
    private final boolean joinKeyContainsUniqueKey;
    @Nullable private final InternalTypeInfo<RowData> uniqueKeyType;
    // 唯一键选择器
    @Nullable private final KeySelector<RowData, RowData> uniqueKeySelector;

    private JoinInputSideSpec(
            boolean joinKeyContainsUniqueKey,
            @Nullable InternalTypeInfo<RowData> uniqueKeyType,
            @Nullable KeySelector<RowData, RowData> uniqueKeySelector) {
        this.inputSideHasUniqueKey = uniqueKeyType != null && uniqueKeySelector != null;
        this.joinKeyContainsUniqueKey = joinKeyContainsUniqueKey;
        this.uniqueKeyType = uniqueKeyType;
        this.uniqueKeySelector = uniqueKeySelector;
    }

    /** Returns true if the input has unique key, otherwise false. */
    public boolean hasUniqueKey() {
        return inputSideHasUniqueKey;
    }

    /** Returns true if the join key contains the unique key of the input. */
    public boolean joinKeyContainsUniqueKey() {
        return joinKeyContainsUniqueKey;
    }

    /**
     * Returns the {@link TypeInformation} of the unique key. Returns null if the input hasn't
     * unique key.
     */
    @Nullable
    public InternalTypeInfo<RowData> getUniqueKeyType() {
        return uniqueKeyType;
    }

    /**
     * Returns the {@link KeySelector} to extract unique key from the input row. Returns null if the
     * input hasn't unique key.
     */
    @Nullable
    public KeySelector<RowData, RowData> getUniqueKeySelector() {
        return uniqueKeySelector;
    }

    /**
     * Creates a {@link JoinInputSideSpec} that the input has an unique key.
     *
     * @param uniqueKeyType type information of the unique key
     * @param uniqueKeySelector key selector to extract unique key from the input row
     */
    public static JoinInputSideSpec withUniqueKey(
            InternalTypeInfo<RowData> uniqueKeyType,
            KeySelector<RowData, RowData> uniqueKeySelector) {
        checkNotNull(uniqueKeyType);
        checkNotNull(uniqueKeySelector);
        return new JoinInputSideSpec(false, uniqueKeyType, uniqueKeySelector);
    }

    /**
     * Creates a {@link JoinInputSideSpec} that input has an unique key and the unique key is
     * contained by the join key.
     *
     * @param uniqueKeyType type information of the unique key
     * @param uniqueKeySelector key selector to extract unique key from the input row
     */
    public static JoinInputSideSpec withUniqueKeyContainedByJoinKey(
            InternalTypeInfo<RowData> uniqueKeyType,
            KeySelector<RowData, RowData> uniqueKeySelector) {
        checkNotNull(uniqueKeyType);
        checkNotNull(uniqueKeySelector);
        return new JoinInputSideSpec(true, uniqueKeyType, uniqueKeySelector);
    }

    /** Creates a {@link JoinInputSideSpec} that input hasn't any unique keys. */
    public static JoinInputSideSpec withoutUniqueKey() {
        return new JoinInputSideSpec(false, null, null);
    }

    @Override
    public String toString() {
        if (inputSideHasUniqueKey) {
            if (joinKeyContainsUniqueKey) {
                return "JoinKeyContainsUniqueKey";
            } else {
                return "HasUniqueKey";
            }
        } else {
            return "NoUniqueKey";
        }
    }
}
