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
 *
 */

package org.apache.flink.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 标记类以供实验使用的注释。
 *
 * <p>带有此注释的类既不经过战斗测试，也不稳定，可能在未来的版本中更改或删除。
 *
 * <p>该注释还排除了使用{@link Public}和{@link PublicEvolving}注释的进化接口签名的类。
 *
 * Annotation to mark classes for experimental use.
 *
 * <p>Classes with this annotation are neither battle-tested nor stable, and may be changed or
 * removed in future versions.
 *
 * <p>This annotation also excludes classes with evolving interfaces / signatures annotated with
 * {@link Public} and {@link PublicEvolving}.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Public
public @interface Experimental {}
