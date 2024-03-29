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

package org.apache.flink.table.planner.plan.schema

import org.apache.flink.table.catalog.{ObjectIdentifier, ResolvedCatalogTable}
import org.apache.flink.table.connector.source.DynamicTableSource
import org.apache.flink.table.planner.plan.abilities.source.SourceAbilitySpec
import org.apache.flink.table.planner.plan.stats.FlinkStatistic

import com.google.common.collect.ImmutableList
import org.apache.calcite.plan.RelOptSchema
import org.apache.calcite.rel.`type`.RelDataType

import java.util

/**
 * 一个[[FlinkPreparingTableBase]]实现，它定义了翻译方解石所需的上下文变量。使用[[DynamicTableSource]]
 * 将[[org.apache.calcite.plan.RelOptTable]]转换为Flink特定的关系表达式。
 *
 * A [[FlinkPreparingTableBase]] implementation which defines the context variables
 * required to translate the Calcite [[org.apache.calcite.plan.RelOptTable]] to the Flink specific
 * relational expression with [[DynamicTableSource]].
 *
 * @param relOptSchema The RelOptSchema that this table comes from
 * @param tableIdentifier The full path of the table to retrieve.
 * @param rowType The table row type
 * @param statistic The table statistics
 * @param tableSource The [[DynamicTableSource]] for which is converted to a Calcite Table
 * @param isStreamingMode A flag that tells if the current table is in stream mode
 * @param catalogTable Resolved catalog table where this table source table comes from
 * @param extraDigests The extra digests which will be added into `getQualifiedName`
 *                     as a part of table digest
 */
class TableSourceTable(
    relOptSchema: RelOptSchema,
    val tableIdentifier: ObjectIdentifier,
    rowType: RelDataType,
    statistic: FlinkStatistic,
    val tableSource: DynamicTableSource,
    val isStreamingMode: Boolean,
    val catalogTable: ResolvedCatalogTable,
    val extraDigests: Array[String] = Array.empty,
    val abilitySpecs: Array[SourceAbilitySpec] = Array.empty)
  extends FlinkPreparingTableBase(
    relOptSchema,
    rowType,
    util.Arrays.asList(
      tableIdentifier.getCatalogName,
      tableIdentifier.getDatabaseName,
      tableIdentifier.getObjectName),
    statistic) {

  override def getQualifiedName: util.List[String] = {
    val builder = ImmutableList.builder[String]()
        .addAll(super.getQualifiedName)
    extraDigests.foreach(builder.add)
    builder.build()
  }

  /**
   * Creates a copy of this table with specified digest.
   *
   * @param newTableSource tableSource to replace
   * @param newRowType new row type
   * @return added TableSourceTable instance with specified digest
   */
  def copy(
      newTableSource: DynamicTableSource,
      newRowType: RelDataType,
      newExtraDigests: Array[String],
      newAbilitySpecs: Array[SourceAbilitySpec]): TableSourceTable = {
    new TableSourceTable(
      relOptSchema,
      tableIdentifier,
      newRowType,
      statistic,
      newTableSource,
      isStreamingMode,
      catalogTable,
      extraDigests ++ newExtraDigests,
      abilitySpecs ++ newAbilitySpecs
    )
  }

  /**
   * Creates a copy of this table with specified digest and statistic.
   *
   * @param newTableSource tableSource to replace
   * @param newStatistic statistic to replace
   * @return added TableSourceTable instance with specified digest and statistic
   */
  def copy(
      newTableSource: DynamicTableSource,
      newStatistic: FlinkStatistic,
      newExtraDigests: Array[String],
      newAbilitySpecs: Array[SourceAbilitySpec]): TableSourceTable = {
    new TableSourceTable(
      relOptSchema,
      tableIdentifier,
      rowType,
      newStatistic,
      newTableSource,
      isStreamingMode,
      catalogTable,
      extraDigests ++ newExtraDigests,
      abilitySpecs ++ newAbilitySpecs)
  }
}
