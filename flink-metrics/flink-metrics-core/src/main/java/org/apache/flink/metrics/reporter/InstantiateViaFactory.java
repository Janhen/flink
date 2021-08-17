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

package org.apache.flink.metrics.reporter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link MetricReporter MetricReporters} 的注释，支持工厂但希望与现有的基于反射的配置保持向后兼容性。
 *
 * <p>当一个带注释的报告器被配置为通过反射使用时，将使用给定的工厂。
 *
 * <p>注意：如果报告器作为插件加载，则此注释不起作用。对于这些情况，请改为使用
 *   {@link InterceptInstantiationViaReflection} 注释工厂。
 *
 * Annotation for {@link MetricReporter MetricReporters} that support factories but want to maintain
 * backwards-compatibility with existing reflection-based configurations.
 *
 * <p>When an annotated reporter is configured to be used via reflection the given factory will be
 * used instead.
 *
 * <p>Attention: This annotation does not work if the reporter is loaded as a plugin. For these
 * cases, annotate the factory with {@link InterceptInstantiationViaReflection} instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InstantiateViaFactory {
    String factoryClassName();
}
