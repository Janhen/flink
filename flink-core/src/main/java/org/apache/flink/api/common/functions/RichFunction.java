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

package org.apache.flink.api.common.functions;

import org.apache.flink.annotation.Public;
import org.apache.flink.configuration.Configuration;

/**
 * 所有富用户定义函数的基接口。该类定义了函数生命周期的方法，以及用于访问执行函数的上下文的方法。
 *
 * An base interface for all rich user-defined functions. This class defines methods for the life
 * cycle of the functions, as well as methods to access the context in which the functions are
 * executed.
 */
@Public
public interface RichFunction extends Function {

    /**
     * 函数的初始化方法。它在实际工作方法（如 <i>map<i> 或 <i>join<i>）之前调用，因此适用于一次性设置工作。对于
     * 作为迭代一部分的函数，将在每个迭代超级步开始时调用此方法。
     *
     * <p>传递给函数的配置对象可用于配置和初始化。配置包含在程序组合中对功能进行配置的所有参数。
     *
     * Initialization method for the function. It is called before the actual working methods (like
     * <i>map</i> or <i>join</i>) and thus suitable for one time setup work. For functions that are
     * part of an iteration, this method will be invoked at the beginning of each iteration
     * superstep.
     *
     * <p>The configuration object passed to the function can be used for configuration and
     * initialization. The configuration contains all parameters that were configured on the
     * function in the program composition.
     *
     * <pre>{@code
     * public class MyFilter extends RichFilterFunction<String> {
     *
     *     private String searchString;
     *
     *     public void open(Configuration parameters) {
     *         this.searchString = parameters.getString("foo");
     *     }
     *
     *     public boolean filter(String value) {
     *         return value.equals(searchString);
     *     }
     * }
     * }</pre>
     *
     * <p>By default, this method does nothing.
     *
     * @param parameters The configuration containing the parameters attached to the contract.
     * @throws Exception Implementations may forward exceptions, which are caught by the runtime.
     *     When the runtime catches an exception, it aborts the task and lets the fail-over logic
     *     decide whether to retry the task execution.
     * @see org.apache.flink.configuration.Configuration
     */
    void open(Configuration parameters) throws Exception;

    /**
     * Tear-down method for the user code. It is called after the last call to the main working
     * methods (e.g. <i>map</i> or <i>join</i>). For functions that are part of an iteration, this
     * method will be invoked after each iteration superstep.
     *
     * <p>This method can be used for clean up work.
     *
     * @throws Exception Implementations may forward exceptions, which are caught by the runtime.
     *     When the runtime catches an exception, it aborts the task and lets the fail-over logic
     *     decide whether to retry the task execution.
     */
    void close() throws Exception;

    // ------------------------------------------------------------------------
    //  Runtime context
    // ------------------------------------------------------------------------

    /**
     * 获取包含有关 UDF 运行时信息的上下文，例如函数的并行度、函数的子任务索引或执行该函数的任务的名称。
     *
     * <p> RuntimeContext 还允许访问 {@link org.apache.flink.api.common.accumulators.Accumulator}
     * 和 {@link org.apache.flink.api.common.cache.DistributedCache}
     *
     * Gets the context that contains information about the UDF's runtime, such as the parallelism
     * of the function, the subtask index of the function, or the name of the of the task that
     * executes the function.
     *
     * <p>The RuntimeContext also gives access to the {@link
     * org.apache.flink.api.common.accumulators.Accumulator}s and the {@link
     * org.apache.flink.api.common.cache.DistributedCache}.
     *
     * @return The UDF's runtime context.
     */
    RuntimeContext getRuntimeContext();

    /**
     * 获取 {@link RuntimeContext} 的专一版本，其中包含关于执行函数的迭代的额外信息。
     * 此 IterationRuntimeContext 仅在函数是迭代的一部分时可用。否则，此方法将抛出异常。
     *
     * Gets a specialized version of the {@link RuntimeContext}, which has additional information
     * about the iteration in which the function is executed. This IterationRuntimeContext is only
     * available if the function is part of an iteration. Otherwise, this method throws an
     * exception.
     *
     * @return The IterationRuntimeContext.
     * @throws java.lang.IllegalStateException Thrown, if the function is not executed as part of an
     *     iteration.
     */
    IterationRuntimeContext getIterationRuntimeContext();

    /**
     * Sets the function's runtime context. Called by the framework when creating a parallel
     * instance of the function.
     *
     * @param t The runtime context.
     */
    void setRuntimeContext(RuntimeContext t);
}
