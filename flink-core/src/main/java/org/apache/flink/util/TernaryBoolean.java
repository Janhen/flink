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

import org.apache.flink.annotation.PublicEvolving;

import javax.annotation.Nullable;

/**
 * 一个三元布尔值，它的值可以是'true'， 'false'或'undefined'。
 *
 * <p>一个三元布尔值可以用于配置开关，可能没有配置(未定义)，在这种情况下，应该假定一个默认值。
 *
 * A ternary boolean, which can have the values 'true', 'false', or 'undefined'.
 *
 * <p>A ternary boolean can for example be used to configuration switches that may be not configured
 * (undefined), in which case a default value should be assumed.
 */
@PublicEvolving
public enum TernaryBoolean {

    /** The value for 'true'. */
    TRUE,

    /** The value for 'false'. */
    FALSE,

    /**
     * “未定义”的值。在配置设置中，这通常意味着将使用默认值，或来自部署范围配置的值。
     *
     * The value for 'undefined'. In a configuration setting, this typically means that the default
     * value will be used, or the value from a deployment-wide configuration.
     */
    UNDEFINED;

    // ------------------------------------------------------------------------

    /**
     * Gets the boolean value corresponding to this value. If this is the 'undefined' value, the
     * method returns the given default.
     *
     * @param defaultValue The value to be returned in case this ternary value is 'undefined'.
     */
    public boolean getOrDefault(boolean defaultValue) {
        return this == UNDEFINED ? defaultValue : (this == TRUE);
    }

    /**
     * Gets the boolean value corresponding to this value. If this is the 'UNDEFINED' value, the
     * method returns the given valueForUndefined.
     *
     * @param valueForUndefined The value to be returned in case this ternary value is 'undefined'.
     */
    public TernaryBoolean resolveUndefined(boolean valueForUndefined) {
        return this != UNDEFINED ? this : fromBoolean(valueForUndefined);
    }

    /** Gets this ternary boolean as a boxed boolean. The value 'undefined' results in 'null. */
    @Nullable
    public Boolean getAsBoolean() {
        return this == UNDEFINED ? null : (this == TRUE ? Boolean.TRUE : Boolean.FALSE);
    }

    // ------------------------------------------------------------------------

    /**
     * Converts the given boolean to a TernaryBoolean, {@link #TRUE} or {@link #FALSE} respectively.
     */
    public static TernaryBoolean fromBoolean(boolean bool) {
        return bool ? TRUE : FALSE;
    }

    /**
     * Converts the given boxed Boolean to a TernaryBoolean. A null value results in {@link
     * #UNDEFINED}, while a non-null value results in {@link #TRUE} or {@link #FALSE} respectively.
     */
    public static TernaryBoolean fromBoxedBoolean(@Nullable Boolean bool) {
        return bool == null ? UNDEFINED : fromBoolean(bool);
    }
}
