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

package org.apache.flink.api.common.serialization;

import org.apache.flink.annotation.Public;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.util.UserCodeClassLoader;

import java.io.Serializable;

/**
 * 序列化模式描述了如何将数据对象转换为不同的序列化表示。大多数数据接收器（例如 Apache Kafka）需要以特定格式
 * （例如作为字节字符串）将数据传递给它们。
 *
 * The serialization schema describes how to turn a data object into a different serialized
 * representation. Most data sinks (for example Apache Kafka) require the data to be handed to them
 * in a specific format (for example as byte strings).
 *
 * @param <T> The type to be serialized.
 */
@Public
public interface SerializationSchema<T> extends Serializable {
    /**
     * Initialization method for the schema. It is called before the actual working methods {@link
     * #serialize(Object)} and thus suitable for one time setup work.
     *
     * <p>The provided {@link InitializationContext} can be used to access additional features such
     * as e.g. registering user metrics.
     *
     * @param context Contextual information that can be used during initialization.
     */
    @PublicEvolving
    default void open(InitializationContext context) throws Exception {}

    /**
     * Serializes the incoming element to a specified type.
     *
     * @param element The incoming element to be serialized
     * @return The serialized element.
     */
    byte[] serialize(T element);

    /**
     * A contextual information provided for {@link #open(InitializationContext)} method. It can be
     * used to:
     *
     * <ul>
     *   <li>Register user metrics via {@link InitializationContext#getMetricGroup()}
     *   <li>Access the user code class loader.
     * </ul>
     */
    @PublicEvolving
    interface InitializationContext {
        /**
         * Returns the metric group for the parallel subtask of the source that runs this {@link
         * SerializationSchema}.
         *
         * <p>Instances of this class can be used to register new metrics with Flink and to create a
         * nested hierarchy based on the group names. See {@link MetricGroup} for more information
         * for the metrics system.
         *
         * @see MetricGroup
         */
        MetricGroup getMetricGroup();

        /**
         * Gets the {@link UserCodeClassLoader} to load classes that are not in system's classpath,
         * but are part of the jar file of a user job.
         *
         * @see UserCodeClassLoader
         */
        UserCodeClassLoader getUserCodeClassLoader();
    }
}
