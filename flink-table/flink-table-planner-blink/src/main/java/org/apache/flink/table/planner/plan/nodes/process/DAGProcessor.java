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

package org.apache.flink.table.planner.plan.nodes.process;

import org.apache.flink.table.planner.plan.nodes.exec.ExecNode;

import java.util.List;

/** DAGProcess plugin, use it can set resource of dag or change other node info. */
// DAGProcess 插件，使用它可以设置 dag 的资源或更改其他节点信息。
public interface DAGProcessor {

    /** Given a dag, process it and return the result dag. */
    // 给定一个DAG，处理它并返回结果DAG。
    List<ExecNode<?, ?>> process(List<ExecNode<?, ?>> sinkNodes, DAGProcessContext context);
}
