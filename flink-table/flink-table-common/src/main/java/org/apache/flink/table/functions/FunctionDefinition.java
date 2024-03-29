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
import org.apache.flink.table.catalog.DataTypeFactory;
import org.apache.flink.table.types.inference.TypeInference;

import java.util.Collections;
import java.util.Set;

/**
 * 函数的定义。该类的实例提供了验证函数调用和执行计划所需的所有细节。
 *
 * <p>纯函数定义不能包含运行时实现。这可以在后面的阶段由计划者提供。
 *
 * Definition of a function. Instances of this class provide all details necessary to validate a
 * function call and perform planning.
 *
 * <p>A pure function definition must not contain a runtime implementation. This can be provided by
 * the planner at later stages.
 *
 * @see UserDefinedFunction
 */
@PublicEvolving
public interface FunctionDefinition {

    /** Returns the kind of function this definition describes. */
    FunctionKind getKind();

    /**
     * 返回执行对此函数定义的调用的类型推断的逻辑。
     *
     * <p>类型推断过程负责推断未知类型的输入参数、验证输入参数并生成结果类型。类型推断过程独立于函数体发生。类型推断
     * 的输出用于搜索相应的运行时实现。
     *
     * <p>可以使用 {@link TypeInference#newBuilder()} 创建类型推断的实例。
     *
     * <p>有关具体使用示例，请参阅 {@link BuiltInFunctionDefinitions}。
     *
     * Returns the logic for performing type inference of a call to this function definition.
     *
     * <p>The type inference process is responsible for inferring unknown types of input arguments,
     * validating input arguments, and producing result types. The type inference process happens
     * independent of a function body. The output of the type inference is used to search for a
     * corresponding runtime implementation.
     *
     * <p>Instances of type inference can be created by using {@link TypeInference#newBuilder()}.
     *
     * <p>See {@link BuiltInFunctionDefinitions} for concrete usage examples.
     */
    TypeInference getTypeInference(DataTypeFactory typeFactory);

    /** Returns the set of requirements this definition demands. */
    // 返回此定义要求的要求集。
    default Set<FunctionRequirement> getRequirements() {
        return Collections.emptySet();
    }

    /**
     * 返回有关函数结果的确定性的信息。
     *
     * <p>它返回 <code>true<code> 当且仅当保证调用此函数总是返回相同的结果给定相同的参数。 <code>true<code>
     *   默认采用。如果函数不像 <code>random(), date(), now(), ... <code> 那样是纯粹的函数，则此方法必须返回
     *   <code>false<code>。
     *
     * <p>此外，如果规划器应始终在集群端执行此功能，则返回 <code>false<code>。换句话说：计划者在计划对该函数的常量
     * 调用期间不应执行常量表达式缩减。
     *
     * Returns information about the determinism of the function's results.
     *
     * <p>It returns <code>true</code> if and only if a call to this function is guaranteed to
     * always return the same result given the same parameters. <code>true</code> is assumed by
     * default. If the function is not purely functional like <code>random(), date(), now(), ...
     * </code> this method must return <code>false</code>.
     *
     * <p>Furthermore, return <code>false</code> if the planner should always execute this function
     * on the cluster side. In other words: the planner should not perform constant expression
     * reduction during planning for constant calls to this function.
     */
    default boolean isDeterministic() {
        return true;
    }
}
