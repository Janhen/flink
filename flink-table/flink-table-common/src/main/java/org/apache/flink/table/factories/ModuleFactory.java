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

package org.apache.flink.table.factories;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.module.Module;

import java.util.Map;

/**
 * 基于基于字符串的属性创建已配置模块实例的工厂。更多信息请参见{@link TableFactory}。
 *
 * A factory to create configured module instances based on string-based properties. See also {@link
 * TableFactory} for more information.
 */
@PublicEvolving
public interface ModuleFactory extends TableFactory {

    /**
     * Creates and configures a {@link Module} using the given properties.
     *
     * @param properties normalized properties describing a module.
     * @return the configured module.
     */
    Module createModule(Map<String, String> properties);
}
