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

package org.apache.flink.table.plan.logical.rel

import java.util.Collections

import org.apache.calcite.plan.{RelOptCluster, RelTraitSet}
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.core._
import org.apache.calcite.rex.{RexBuilder, RexCall, RexNode}
import org.apache.calcite.sql.`type`.{OperandTypes, ReturnTypes}
import org.apache.calcite.sql.{SqlFunction, SqlFunctionCategory, SqlKind}
import org.apache.flink.util.Preconditions.checkArgument

/**
 * 表示表与[[org.apache.flink.table.functions.TemporalTableFunction]]之间的连接
 *
  * Represents a join between a table and [[org.apache.flink.table.functions.TemporalTableFunction]]
  *
  * @param cluster
  * @param traitSet
  * @param left
  * @param right     table scan (or other more complex table expression) of underlying
  *                  [[org.apache.flink.table.functions.TemporalTableFunction]]
 *                  底层[[org.apache.flink.table.functions.TemporalTableFunction]]的表扫描(或其他更
 *                  复杂的表表达式)
  * @param condition must contain [[LogicalTemporalTableJoin#TEMPORAL_JOIN_CONDITION]] with
  *                  correctly defined references to rightTimeAttribute,
  *                  rightPrimaryKeyExpression and leftTimeAttribute. We can not implement
  *                  those references as separate fields, because of problems with Calcite's
  *                  optimization rules like projections push downs, column
  *                  pruning/renaming/reordering, etc. Later rightTimeAttribute,
  *                  rightPrimaryKeyExpression and leftTimeAttribute will be extracted from
  *                  the condition.
 *                  必须包含[[LogicalTemporalTableJoin#TEMPORAL_JOIN_CONDITION]]和正确定义的对
 *                  rightTimeAttribute, rightPrimaryKeyExpression和leftTimeAttribute的引用。我们不能
 *                  将这些引用实现为单独的字段，因为方解石的优化规则存在问题，如投影下推、列修剪重命名排序等。稍后将
 *                  从条件中提取rightTimeAttribute、rightPrimaryKeyExpression和leftTimeAttribute。
  */
class LogicalTemporalTableJoin private(
    cluster: RelOptCluster,
    traitSet: RelTraitSet,
    left: RelNode,
    right: RelNode,
    condition: RexNode)
  extends Join(
    cluster,
    traitSet,
    left,
    right,
    condition,
    Collections.emptySet().asInstanceOf[java.util.Set[CorrelationId]],
    JoinRelType.INNER) {

  override def copy(
       traitSet: RelTraitSet,
       condition: RexNode,
       left: RelNode,
       right: RelNode,
       joinType: JoinRelType,
       semiJoinDone: Boolean): LogicalTemporalTableJoin = {
    checkArgument(joinType == this.getJoinType,
      "Can not change join type".asInstanceOf[Object])
    checkArgument(semiJoinDone == this.isSemiJoinDone,
      "Can not change semiJoinDone".asInstanceOf[Object])
    new LogicalTemporalTableJoin(
      cluster,
      traitSet,
      left,
      right,
      condition)
  }
}

object LogicalTemporalTableJoin {
  /**
    * See [[LogicalTemporalTableJoin#condition]]
    */
  val TEMPORAL_JOIN_CONDITION = new SqlFunction(
    "__TEMPORAL_JOIN_CONDITION",
    SqlKind.OTHER_FUNCTION,
    ReturnTypes.BOOLEAN_NOT_NULL,
    null,
    OperandTypes.or(
      OperandTypes.sequence(
        "'(LEFT_TIME_ATTRIBUTE, RIGHT_TIME_ATTRIBUTE, PRIMARY_KEY)'",
        OperandTypes.DATETIME,
        OperandTypes.DATETIME,
        OperandTypes.ANY),
      OperandTypes.sequence(
        "'(LEFT_TIME_ATTRIBUTE, PRIMARY_KEY)'",
        OperandTypes.DATETIME,
        OperandTypes.ANY)),
    SqlFunctionCategory.SYSTEM)

  def isRowtimeCall(call: RexCall): Boolean = {
    checkArgument(call.getOperator == TEMPORAL_JOIN_CONDITION)
    call.getOperands.size() == 3
  }

  def isProctimeCall(call: RexCall): Boolean = {
    checkArgument(call.getOperator == TEMPORAL_JOIN_CONDITION)
    call.getOperands.size() == 2
  }

  def makeRowTimeTemporalJoinConditionCall(
      rexBuilder: RexBuilder,
      leftTimeAttribute: RexNode,
      rightTimeAttribute: RexNode,
      rightPrimaryKeyExpression: RexNode): RexNode = {
    rexBuilder.makeCall(
      TEMPORAL_JOIN_CONDITION,
      leftTimeAttribute,
      rightTimeAttribute,
      rightPrimaryKeyExpression)
  }

  def makeProcTimeTemporalJoinConditionCall(
      rexBuilder: RexBuilder,
      leftTimeAttribute: RexNode,
      rightPrimaryKeyExpression: RexNode): RexNode = {
    rexBuilder.makeCall(
      TEMPORAL_JOIN_CONDITION,
      leftTimeAttribute,
      rightPrimaryKeyExpression)
  }

  /**
    * See [[LogicalTemporalTableJoin]]
    */
  def createRowtime(
      rexBuilder: RexBuilder,
      cluster: RelOptCluster,
      traitSet: RelTraitSet,
      left: RelNode,
      right: RelNode,
      leftTimeAttribute: RexNode,
      rightTimeAttribute: RexNode,
      rightPrimaryKeyExpression: RexNode)
    : LogicalTemporalTableJoin = {
    new LogicalTemporalTableJoin(
      cluster,
      traitSet,
      left,
      right,
      makeRowTimeTemporalJoinConditionCall(
        rexBuilder,
        leftTimeAttribute,
        rightTimeAttribute,
        rightPrimaryKeyExpression))
  }

  /**
    * See [[LogicalTemporalTableJoin]]
    *
    * @param leftTimeAttribute is needed because otherwise,
    *                          [[LogicalTemporalTableJoin#TEMPORAL_JOIN_CONDITION]] could be pushed
    *                          down below [[LogicalTemporalTableJoin]], since it wouldn't have any
    *                          references to the left node.
    */
  def createProctime(
      rexBuilder: RexBuilder,
      cluster: RelOptCluster,
      traitSet: RelTraitSet,
      left: RelNode,
      right: RelNode,
      leftTimeAttribute: RexNode,
      rightPrimaryKeyExpression: RexNode)
    : LogicalTemporalTableJoin = {
    new LogicalTemporalTableJoin(
      cluster,
      traitSet,
      left,
      right,
      makeProcTimeTemporalJoinConditionCall(
        rexBuilder,
        leftTimeAttribute,
        rightPrimaryKeyExpression))
  }
}
