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

package org.apache.flink.util;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.Properties;

/** Simple utilities for getting typed values from Properties. */
// 用于从属性获取类型值的简单实用程序。
public class PropertiesUtil {

    /**
     * 从属性中获取整数。如果整数无效，此方法将引发异常。
     *
     * Get integer from properties. This method throws an exception if the integer is not valid.
     *
     * @param config Properties
     * @param key key in Properties
     * @param defaultValue default value if value is not set
     * @return default or value of key
     */
    public static int getInt(Properties config, String key, int defaultValue) {
        String val = config.getProperty(key);
        if (val == null) {
            return defaultValue;
        } else {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                        "Value for configuration key='"
                                + key
                                + "' is not set correctly. "
                                + "Entered value='"
                                + val
                                + "'. Default value='"
                                + defaultValue
                                + "'");
            }
        }
    }

    /**
     * 从属性中获取长。如果 long 无效，则此方法将引发异常。
     *
     * Get long from properties. This method throws an exception if the long is not valid.
     *
     * @param config Properties
     * @param key key in Properties
     * @param defaultValue default value if value is not set
     * @return default or value of key
     */
    public static long getLong(Properties config, String key, long defaultValue) {
        String val = config.getProperty(key);
        if (val == null) {
            return defaultValue;
        } else {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                        "Value for configuration key='"
                                + key
                                + "' is not set correctly. "
                                + "Entered value='"
                                + val
                                + "'. Default value='"
                                + defaultValue
                                + "'");
            }
        }
    }

    /**
     * 从属性中获取长。此方法仅在 long 无效时记录。
     *
     * Get long from properties. This method only logs if the long is not valid.
     *
     * @param config Properties
     * @param key key in Properties
     * @param defaultValue default value if value is not set
     * @return default or value of key
     */
    public static long getLong(Properties config, String key, long defaultValue, Logger logger) {
        try {
            return getLong(config, key, defaultValue);
        } catch (IllegalArgumentException iae) {
            logger.warn(iae.getMessage());
            return defaultValue;
        }
    }

    /**
     * 从属性获取布尔值。如果解析值为 “true”，这个方法将返回{@code true}。
     *
     * Get boolean from properties. This method returns {@code true} iff the parsed value is "true".
     *
     * @param config Properties
     * @param key key in Properties
     * @param defaultValue default value if value is not set
     * @return default or value of key
     */
    public static boolean getBoolean(Properties config, String key, boolean defaultValue) {
        String val = config.getProperty(key);
        if (val == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(val);
        }
    }

    /**
     * 将递归 {@link Properties} 展平为一级属性映射。
     *
     * <p>在某些情况下，例如 {@code KafkaProducer#propsToMap}，Properties 纯粹用作 HashTable，而没有考虑其默
     * 认属性。
     *
     * Flatten a recursive {@link Properties} to a first level property map.
     *
     * <p>In some cases, {@code KafkaProducer#propsToMap} for example, Properties is used purely as
     * a HashTable without considering its default properties.
     *
     * @param config Properties to be flattened
     * @return Properties without defaults; all properties are put in the first-level
     */
    public static Properties flatten(Properties config) {
        final Properties flattenProperties = new Properties();

        Collections.list(config.propertyNames()).stream()
                .forEach(
                        name -> {
                            Preconditions.checkArgument(name instanceof String);
                            flattenProperties.setProperty(
                                    (String) name, config.getProperty((String) name));
                        });

        return flattenProperties;
    }

    // ------------------------------------------------------------------------

    /** Private default constructor to prevent instantiation. */
    private PropertiesUtil() {}
}
