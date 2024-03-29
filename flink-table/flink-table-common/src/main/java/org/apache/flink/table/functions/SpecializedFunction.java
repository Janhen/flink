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

package org.apache.flink.table.functions;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.catalog.DataTypeFactory;
import org.apache.flink.table.types.inference.CallContext;

/**
 * 一个 {@link FunctionDefinition}，它可以提供一个运行时实现(即函数体)，专门针对给定的调用和会话。
 *
 * <p>规划器试图将专门化推迟到代码生成之前不久，此时 {@link FunctionDefinition} 给出的信息已经不够了，运行时需要
 *   {@link UserDefinedFunction} 的子类。
 *
 * <p>当运行时代码应该知道仅在计划后可用的信息时（例如本地会话时区或十进制返回类型的精度标度），此接口很有用。
 *
 * <p>在 API 中注册的 {@link UserDefinedFunction} 是隐式特化的，但也可以实现此接口以在运行前重新配置自身。
 *
 * <p>注意：此接口是一种相当低级的功能，但对高级用户很有用。
 *
 * A {@link FunctionDefinition} that can provide a runtime implementation (i.e. the function's body)
 * that is specialized for the given call and session.
 *
 * <p>The planner tries to defer the specialization until shortly before code generation, where the
 * information given by a {@link FunctionDefinition} is not enough anymore and a subclass of {@link
 * UserDefinedFunction} is required for runtime.
 *
 * <p>This interface is useful when the runtime code should know about information that is only
 * available after planning (e.g. local session time zone or precision/scale of decimal return
 * type).
 *
 * <p>A {@link UserDefinedFunction} that is registered in the API is implicitly specialized but can
 * also implement this interface to reconfigure itself before runtime.
 *
 * <p>Note: This interface is a rather low level kind of function but useful for advanced users.
 */
@PublicEvolving
public interface SpecializedFunction extends FunctionDefinition {

    /**
     * Provides a runtime implementation that is specialized for the given call and session.
     *
     * <p>The method must return an instance of {@link UserDefinedFunction} or throw a {@link
     * TableException} if the given call is not supported. The returned instance must have the same
     * {@link FunctionDefinition} semantics but can have a different {@link
     * #getTypeInference(DataTypeFactory)} implementation.
     */
    UserDefinedFunction specialize(SpecializedContext context);

    /** Provides call and session information for the specialized function. */
    interface SpecializedContext {

        /** Returns the context of the current call. */
        CallContext getCallContext();

        /** Gives read-only access to the configuration of the current session. */
        ReadableConfig getConfiguration();

        /** Returns the classloader used to resolve built-in functions. */
        ClassLoader getBuiltInClassLoader();
    }
}
