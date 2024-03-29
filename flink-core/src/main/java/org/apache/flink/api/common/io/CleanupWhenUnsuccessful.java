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

package org.apache.flink.api.common.io;

import org.apache.flink.annotation.Public;

/**
 * {@link OutputFormat} 可能会实现此接口以在执行不成功时运行清理挂钩。
 *
 * {@link OutputFormat}s may implement this interface to run a cleanup hook when the execution is
 * not successful.
 */
@Public
public interface CleanupWhenUnsuccessful {

    /**
     * Hook that is called upon an unsuccessful execution.
     *
     * @throws Exception The method may forward exceptions when the cleanup fails.
     */
    void tryCleanupOnError() throws Exception;
}
