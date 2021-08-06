/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.state;

import org.apache.flink.annotation.PublicEvolving;

/**
 * 这个接口提供了一个上下文，其中使用托管状态(即由状态后端管理的状态)的用户函数可以参与快照。由于后端本身的快照是由系
 * 统拍摄的，所以该接口主要提供关于检查点的元信息。
 *
 * This interface provides a context in which user functions that use managed state (i.e. state that
 * is managed by state backends) can participate in a snapshot. As snapshots of the backends
 * themselves are taken by the system, this interface mainly provides meta information about the
 * checkpoint.
 */
@PublicEvolving
public interface FunctionSnapshotContext extends ManagedSnapshotContext {}
