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

package org.apache.flink.api.common.typeinfo;

import org.apache.flink.annotation.Public;
import org.apache.flink.api.java.typeutils.TypeExtractor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

/**
 * Annotation，用于指定相应的 {@link TypeInfoFactory}，该{@link TypeInformation} 可以为被注释的类型生成
 * {@link TypeInformation}。在类型的层次结构中，向上遍历时将选择距离最近的定义工厂的注释，然而，全局注册的工厂具有
 * 最高的优先级(参见{@link TypeExtractor#registerFactory(Type, Class)})。
 *
 * Annotation for specifying a corresponding {@link TypeInfoFactory} that can produce {@link
 * TypeInformation} for the annotated type. In a hierarchy of types the closest annotation that
 * defines a factory will be chosen while traversing upwards, however, a globally registered factory
 * has highest precedence (see {@link TypeExtractor#registerFactory(Type, Class)}).
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Public
public @interface TypeInfo {

    Class<? extends TypeInfoFactory> value();
}
