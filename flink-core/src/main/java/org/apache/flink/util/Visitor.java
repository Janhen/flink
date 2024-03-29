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

package org.apache.flink.util;

import org.apache.flink.annotation.Internal;

/**
 * 访问者封装了在遍历树或DAG过程中应用于每个节点的功能。
 *
 * A visitor encapsulates functionality that is applied to each node in the process of a traversal
 * of a tree or DAG.
 */
@Internal
public interface Visitor<T extends Visitable<T>> {

    /**
     * 方法，在访问子节点或子代节点之前在访问时调用
     *
     * Method that is invoked on the visit before visiting and child nodes or descendant nodes.
     *
     * @return True, if the traversal should continue, false otherwise.
     */
    boolean preVisit(T visitable);

    /** Method that is invoked after all child nodes or descendant nodes were visited. */
    // 方法，该方法在访问所有子节点或子代节点之后调用
    void postVisit(T visitable);
}
