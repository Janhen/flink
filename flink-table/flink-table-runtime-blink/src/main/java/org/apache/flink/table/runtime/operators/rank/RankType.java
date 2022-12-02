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

package org.apache.flink.table.runtime.operators.rank;

/** An enumeration of rank type, usable to show how exactly generate rank number. */
// 等级类型的枚举，可用来显示如何准确地生成等级数。
public enum RankType {

    /**
     * 根据顺序为分区内的每一行返回一个惟一的序列号，从每个分区中的第一行的1开始，不重复或跳过每个分区的排名结果中的数字。
     * 如果行集中有重复的值，排名数字将被任意分配。
     *
     * Returns a unique sequential number for each row within the partition based on the order,
     * starting at 1 for the first row in each partition and without repeating or skipping numbers
     * in the ranking result of each partition. If there are duplicate values within the row set,
     * the ranking numbers will be assigned arbitrarily.
     */
    ROW_NUMBER,

    /**
     * 根据顺序为分区内每个不同的行返回唯一的秩号，每个分区中的第一行从1开始，重复值的秩相同，并在秩之间留出间隔;这个
     * 间隔出现在重复值之后的序列中。
     *
     * Returns a unique rank number for each distinct row within the partition based on the order,
     * starting at 1 for the first row in each partition, with the same rank for duplicate values
     * and leaving gaps between the ranks; this gap appears in the sequence after the duplicate
     * values.
     */
    RANK,

    /**
     * 类似于 RANK，它根据顺序为分区内的每个不同行生成一个唯一的排名号，每个分区中的第一行从 1 开始，对具有相同排名号
     * 的具有相同值的行进行排名，除了它确实不跳过任何行列，行列之间不留空隙。
     *
     * is similar to the RANK by generating a unique rank number for each distinct row within the
     * partition based on the order, starting at 1 for the first row in each partition, ranking the
     * rows with equal values with the same rank number, except that it does not skip any rank,
     * leaving no gaps between the ranks.
     */
    DENSE_RANK
}
