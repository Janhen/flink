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

package org.apache.flink.table.functions.aggfunctions

import java.lang.{Long => JLong}
import java.lang.{Iterable => JIterable}
import java.util.{Map => JMap}

import org.apache.flink.table.api.dataview.MapView
import org.apache.flink.types.Row

/**
 * 包装一个累加器并添加一个映射来过滤不同的值。
 *
  * Wraps an accumulator and adds a map to filter distinct values.
  *
  * @param distinctValueMap the [[MapView]] that stores the distinct filter hash map.
  */
class DistinctAccumulator(var distinctValueMap: MapView[Row, JLong]) {

  def this() {
    this(new MapView[Row, JLong]())
  }

  def canEqual(a: Any): Boolean = a.isInstanceOf[DistinctAccumulator]

  override def equals(that: Any): Boolean =
    that match {
      case that: DistinctAccumulator => that.canEqual(this) &&
        this.distinctValueMap == that.distinctValueMap
      case _ => false
    }

  /**
    * 检查参数是否唯一并将参数添加到不同的映射中。如果参数是唯一的（尚未出现在地图中），则返回 true，否则返回 false。
    *
    * Checks if the parameters are unique and adds the parameters to the distinct map.
    * Returns true if the parameters are unique (haven't been in the map yet), false otherwise.
    *
    * @param params the parameters to check.
    * @return true if the parameters are unique (haven't been in the map yet), false otherwise.
    */
  def add(params: Row): Boolean = {
    val currentCnt = distinctValueMap.get(params)
    if (currentCnt != null) {
      distinctValueMap.put(params, currentCnt + 1L)
      false
    } else {
      distinctValueMap.put(params, 1L)
      true
    }
  }

  /**
    * 检查参数是否唯一并将参数添加到不同的映射中。如果参数是唯一的（尚未出现在地图中），则返回 true，否则返回 false。
    *
    * Checks if the parameters are unique and adds the parameters to the distinct map.
    * Returns true if the parameters are unique (haven't been in the map yet), false otherwise.
    *
    * @param params the parameters to check.
    * @return true if the parameters are unique (haven't been in the map yet), false otherwise.
    */
  def add(params: Row, count: JLong): Boolean = {
    val currentCnt = distinctValueMap.get(params)
    if (currentCnt != null) {
      distinctValueMap.put(params, currentCnt + count)
      false
    } else {
      distinctValueMap.put(params, count)
      true
    }
  }

  /**
    * 从不同的映射中删除参数的一个实例并检查这是否是最后一个实例。
    * 如果映射中没有参数的实例，则返回 true，否则返回 false。
    *
    * Removes one instance of the parameters from the distinct map and checks if this was the last
    * instance.
    * Returns true if no instances of the parameters remain in the map, false otherwise.
    *
    * @param params the parameters to check.
    * @return true if no instances of the parameters remain in the map, false otherwise.
    */
  def remove(params: Row): Boolean = {
    val currentCnt = distinctValueMap.get(params)
    if (currentCnt == 1) {
      distinctValueMap.remove(params)
      true
    } else {
      distinctValueMap.put(params, currentCnt - 1L)
      false
    }
  }

  def reset(): Unit = {
    distinctValueMap.clear()
  }

  def elements(): JIterable[JMap.Entry[Row, JLong]] = {
    distinctValueMap.map.entrySet()
  }
}
