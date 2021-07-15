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

package org.apache.flink.runtime.state;

import java.io.Serializable;

/**
 * Base of all handles that represent checkpointed state in some form. The object may hold the
 * (small) state directly, or contain a file path (state is in the file), or contain the metadata to
 * access the state stored in some external database.
 *
 * <p>State objects define how to {@link #discardState() discard state} and how to access the {@link
 * #getStateSize() size of the state}.
 *
 * <p>State Objects are transported via RPC between <i>JobManager</i> and <i>TaskManager</i> and
 * must be {@link java.io.Serializable serializable} to support that.
 *
 * <p>Some State Objects are stored in the checkpoint/savepoint metadata. For long-term
 * compatibility, they are not stored via {@link java.io.Serializable Java Serialization}, but
 * through custom serializers.
 */
public interface StateObject extends Serializable {

    /**
     * 丢弃此句柄所引用和庄严拥有的状态，以释放持久存储中的资源。当此对象所表示的状态不再被使用时，将调用此方法。
     *
     * Discards the state referred to and solemnly owned by this handle, to free up resources in the
     * persistent storage. This method is called when the state represented by this object will not
     * be used any more.
     */
    void discardState() throws Exception;

    /**
     * 以字节为单位返回状态的大小。如果不知道大小，这个方法应该返回{@code 0}。
     *
     * <p>此方法产生的值仅用于信息目的和度量监视。如果这个方法返回错误的值，检查点和恢复仍然会正常运行。但是，效率可能会
     * 受到影响(错误的空间预分配)，依赖于指标的功能(比如监视)也会受到影响。
     *
     * <p>实现注意:此方法在获取状态大小时不应该执行任何IO操作(因此它没有声明抛出{@code IOException})。相反，状态大
     * 小应该存储在状态对象中，或者应该从存储在该对象中的状态进行计算。原因是这个方法经常被检查点的几个部分调用，并且从这
     * 个方法发出IO请求会在更高的规模上给存储系统带来沉重的IO负载。
     *
     * Returns the size of the state in bytes. If the size is not known, this method should return
     * {@code 0}.
     *
     * <p>The values produced by this method are only used for informational purposes and for
     * metrics/monitoring. If this method returns wrong values, the checkpoints and recovery will
     * still behave correctly. However, efficiency may be impacted (wrong space pre-allocation) and
     * functionality that depends on metrics (like monitoring) will be impacted.
     *
     * <p>Note for implementors: This method should not perform any I/O operations while obtaining
     * the state size (hence it does not declare throwing an {@code IOException}). Instead, the
     * state size should be stored in the state object, or should be computable from the state
     * stored in this object. The reason is that this method is called frequently by several parts
     * of the checkpointing and issuing I/O requests from this method accumulates a heavy I/O load
     * on the storage system at higher scale.
     *
     * @return Size of the state in bytes.
     */
    long getStateSize();
}
