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

package org.apache.flink.runtime.throwable;

/** Enum for the classification of {@link Throwable} objects into failure/recovery classes. */
// 将{@link Throwable}对象分类为故障恢复类的Enum
public enum ThrowableType {

    /**
     * 表示即使重试也不会成功的错误，例如DivideZeroException。对于这样的错误，不应该发生恢复尝试。相反，
     * 这个 job 应该立即失败
     *
     * This indicates error that would not succeed even with retry, such as DivideZeroException. No
     * recovery attempt should happen for such an error. Instead, the job should fail immediately.
     */
    NonRecoverableError,

    /** Data consumption error, which indicates that we should revoke the producer. */
    // 数据消费错误，指示我们应该撤销生产者
    PartitionDataMissingError,

    /**
     * 这表示与运行环境相关的错误，如硬件错误、服务问题，在这种情况下，我们应该考虑将机器列入黑名单
     *
     * This indicates an error related to the running environment, such as hardware error, service
     * issue, in which case we should consider blacklisting the machine.
     */
    EnvironmentError,

    /** This indicates a problem that is recoverable. */
    // 这表明问题是可恢复的
    RecoverableError
}
