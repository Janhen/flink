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

package org.apache.flink.table.descriptors;

import org.apache.flink.annotation.PublicEvolving;

/**
 * 可以存在于任意级别的描述符(被其他描述符递归地包含)。
 *
 * A descriptor that may exist in an arbitrary level (be recursively included by other descriptors).
 */
@PublicEvolving
public abstract class HierarchyDescriptor implements Descriptor {

    /**
     * 属性转换的内部方法。所有属性键都将以给定键前缀作为前缀。
     *
     * Internal method for properties conversion. All the property keys will be prefixed with the
     * given key prefix.
     */
    public abstract void addPropertiesWithPrefix(String keyPrefix, DescriptorProperties properties);
}
