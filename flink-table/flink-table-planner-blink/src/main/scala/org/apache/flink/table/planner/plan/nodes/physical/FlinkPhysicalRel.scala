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

package org.apache.flink.table.planner.plan.nodes.physical

import org.apache.flink.table.planner.plan.nodes.FlinkRelNode
import org.apache.flink.table.planner.plan.nodes.exec.ExecNode

import org.apache.calcite.plan.RelTraitSet
import org.apache.calcite.rel.RelNode

/**
 * Base class for flink physical relational expression.
 */
// flink物理关系表达式的基类。
trait FlinkPhysicalRel extends FlinkRelNode {

  /**
   * 尝试通过当前节点的后代满足所需的特征。如果后代节点可以满足要求的特征，而当前节点不会销毁它，则返回转换后的输入的新节点。
   *
   * Try to satisfy required traits by descendant of current node. If descendant can satisfy
   * required traits, and current node will not destroy it, then returns the new node with
   * converted inputs.
   *
   * @param requiredTraitSet required traits
   * @return A converted node which satisfy required traits by inputs node of current node.
   *         Returns None if required traits cannot be satisfied.
   */
  def satisfyTraits(requiredTraitSet: RelTraitSet): Option[RelNode] = None

  /**
   * 将这个物理的RelNode转换为[[ExecNode]]。
   *
   * Translate this physical RelNode into an [[ExecNode]].
   *
   * NOTE: This method only needs to create the corresponding ExecNode,
   * the connection to its input/output nodes will be done by ExecGraphGenerator.
   * Because some physical rels need not be translated to a real ExecNode,
   * such as Exchange will be translated to edge in the future.
   */
  def translateToExecNode(): ExecNode[_]
}
