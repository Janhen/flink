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
import org.apache.flink.api.common.ExecutionConfig.GlobalJobParameters;
import org.apache.flink.api.common.externalresource.ExternalResourceInfo;
import org.apache.flink.api.common.functions.Function;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.metrics.MetricGroup;

import java.io.File;
import java.util.Set;

/**
 * {@link FunctionContext} 允许获取关于用户定义函数执行上下文的全局运行时信息。
 *
 * <p>包括指标组、分布式缓存文件、全局作业参数。
 *
 * A {@link FunctionContext} allows to obtain global runtime information about the context in which
 * the user-defined function is executed.
 *
 * <p>The information includes the metric group, distributed cache files, and global job parameters.
 */
@PublicEvolving
public class FunctionContext {

    private RuntimeContext context;

    /**
     * 包装底层 {@link RuntimeContext}。
     *
     * Wraps the underlying {@link RuntimeContext}.
     *
     * @param context the runtime context in which Flink's {@link Function} is executed.
     */
    public FunctionContext(RuntimeContext context) {
        this.context = context;
    }

    /**
     * 返回此并行子任务的指标组。
     *
     * Returns the metric group for this parallel subtask.
     *
     * @return metric group for this parallel subtask.
     */
    public MetricGroup getMetricGroup() {
        return context.getMetricGroup();
    }

    /**
     * 获取分布式缓存文件的本地临时文件副本。
     *
     * Gets the local temporary file copy of a distributed cache files.
     *
     * @param name distributed cache file name
     * @return local temporary file copy of a distributed cache file.
     */
    public File getCachedFile(String name) {
        return context.getDistributedCache().getFile(name);
    }

    /**
     * 获取与给定键关联的全局作业参数值作为字符串。
     *
     * Gets the global job parameter value associated with the given key as a string.
     *
     * @param key key pointing to the associated value
     * @param defaultValue default value which is returned in case global job parameter is null or
     *     there is no value associated with the given key
     * @return (default) value associated with the given key
     */
    public String getJobParameter(String key, String defaultValue) {
        final GlobalJobParameters conf = context.getExecutionConfig().getGlobalJobParameters();
        if (conf != null) {
            return conf.toMap().getOrDefault(key, defaultValue);
        }
        return defaultValue;
    }

    /** Get the external resource information. */
    public Set<ExternalResourceInfo> getExternalResourceInfos(String resourceName) {
        return context.getExternalResourceInfos(resourceName);
    }
}
