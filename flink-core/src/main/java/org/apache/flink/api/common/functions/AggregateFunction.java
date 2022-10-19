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

package org.apache.flink.api.common.functions;

import org.apache.flink.annotation.PublicEvolving;

import java.io.Serializable;

/**
 * {@code AggregateFunction} 是一个灵活的聚合函数，具有以下特性：
 *
 *   <li>聚合可以为输入值、中间聚合和结果类型使用不同的类型，以支持广泛的聚合类型。
 *   <li>支持分布式聚合：可以将不同的中间聚合合并在一起，以实现预聚合最终聚合优化。
 *
 * <p>{@code AggregateFunction} 的中间聚合（进行中的聚合状态）称为 <i>accumulator<i>。将值添加到累加器中，并
 *   通过最终确定累加器状态来获得最终聚合。这支持聚合函数，其中中间状态需要不同于聚合值和最终结果类型，例如 <i>average<i>
 *   （通常保留计数和总和）。合并中间聚合（部分聚合）意味着合并累加器。
 *
 * <p>AggregationFunction 本身是无状态的。为了允许单个 AggregationFunction 实例维护多个聚合（例如每个键一个聚合），
 *   每当启动新聚合时，AggregationFunction 都会创建一个新累加器。
 *
 * <p>聚合函数必须是{@link Serializable}，因为它们在分布式执行期间在分布式进程之间发送。
 *
 * The {@code AggregateFunction} is a flexible aggregation function, characterized by the following
 * features:
 *
 * <ul>
 *   <li>The aggregates may use different types for input values, intermediate aggregates, and
 *       result type, to support a wide range of aggregation types.
 *   <li>Support for distributive aggregations: Different intermediate aggregates can be merged
 *       together, to allow for pre-aggregation/final-aggregation optimizations.
 * </ul>
 *
 * <p>The {@code AggregateFunction}'s intermediate aggregate (in-progress aggregation state) is
 * called the <i>accumulator</i>. Values are added to the accumulator, and final aggregates are
 * obtained by finalizing the accumulator state. This supports aggregation functions where the
 * intermediate state needs to be different than the aggregated values and the final result type,
 * such as for example <i>average</i> (which typically keeps a count and sum). Merging intermediate
 * aggregates (partial aggregates) means merging the accumulators.
 *
 * <p>The AggregationFunction itself is stateless. To allow a single AggregationFunction instance to
 * maintain multiple aggregates (such as one aggregate per key), the AggregationFunction creates a
 * new accumulator whenever a new aggregation is started.
 *
 * <p>Aggregation functions must be {@link Serializable} because they are sent around between
 * distributed processes during distributed execution.
 *
 * <h1>Example: Average and Weighted Average</h1>
 *
 * <pre>{@code
 * // the accumulator, which holds the state of the in-flight aggregate
 * public class AverageAccumulator {
 *     long count;
 *     long sum;
 * }
 *
 * // implementation of an aggregation function for an 'average'
 * public class Average implements AggregateFunction<Integer, AverageAccumulator, Double> {
 *
 *     public AverageAccumulator createAccumulator() {
 *         return new AverageAccumulator();
 *     }
 *
 *     public AverageAccumulator merge(AverageAccumulator a, AverageAccumulator b) {
 *         a.count += b.count;
 *         a.sum += b.sum;
 *         return a;
 *     }
 *
 *     public AverageAccumulator add(Integer value, AverageAccumulator acc) {
 *         acc.sum += value;
 *         acc.count++;
 *         return acc;
 *     }
 *
 *     public Double getResult(AverageAccumulator acc) {
 *         return acc.sum / (double) acc.count;
 *     }
 * }
 *
 * // implementation of a weighted average
 * // this reuses the same accumulator type as the aggregate function for 'average'
 * public class WeightedAverage implements AggregateFunction<Datum, AverageAccumulator, Double> {
 *
 *     public AverageAccumulator createAccumulator() {
 *         return new AverageAccumulator();
 *     }
 *
 *     public AverageAccumulator merge(AverageAccumulator a, AverageAccumulator b) {
 *         a.count += b.count;
 *         a.sum += b.sum;
 *         return a;
 *     }
 *
 *     public AverageAccumulator add(Datum value, AverageAccumulator acc) {
 *         acc.count += value.getWeight();
 *         acc.sum += value.getValue();
 *         return acc;
 *     }
 *
 *     public Double getResult(AverageAccumulator acc) {
 *         return acc.sum / (double) acc.count;
 *     }
 * }
 * }</pre>
 *
 * @param <IN> The type of the values that are aggregated (input values)
 * @param <ACC> The type of the accumulator (intermediate aggregate state).
 * @param <OUT> The type of the aggregated result
 */
@PublicEvolving
public interface AggregateFunction<IN, ACC, OUT> extends Function, Serializable {

    /**
     * 创建一个新的累加器，开始一个新的聚合。
     *
     * <p>除非通过 {@link #add(Object, Object)} 添加一个值，否则新的累加器通常是没有意义的。
     *
     * <p>累加器是正在运行的聚合的状态。当一个程序有多个正在进行的聚合（例如每个键和窗口）时，状态（每个键和窗口）是
     *   累加器的大小。
     *
     * Creates a new accumulator, starting a new aggregate.
     *
     * <p>The new accumulator is typically meaningless unless a value is added via {@link
     * #add(Object, Object)}.
     *
     * <p>The accumulator is the state of a running aggregation. When a program has multiple
     * aggregates in progress (such as per key and window), the state (per key and window) is the
     * size of the accumulator.
     *
     * @return A new accumulator, corresponding to an empty aggregate.
     */
    ACC createAccumulator();

    /**
     * 将给定的输入值添加到给定的累加器，返回新的累加器值。
     *
     * <p>为了提高效率，输入累加器可能会被修改和返回。
     *
     * Adds the given input value to the given accumulator, returning the new accumulator value.
     *
     * <p>For efficiency, the input accumulator may be modified and returned.
     *
     * @param value The value to add
     * @param accumulator The accumulator to add the value to
     * @return The accumulator with the updated state
     */
    ACC add(IN value, ACC accumulator);

    /**
     * Gets the result of the aggregation from the accumulator.
     *
     * @param accumulator The accumulator of the aggregation
     * @return The final aggregation result.
     */
    OUT getResult(ACC accumulator);

    /**
     * 合并两个累加器，返回具有合并状态的累加器。
     *
     * <p>这个函数可以重用任何给定的累加器作为合并的目标并返回它。假设是给定的累加器在传递给此函数后将不再使用。
     *
     * Merges two accumulators, returning an accumulator with the merged state.
     *
     * <p>This function may reuse any of the given accumulators as the target for the merge and
     * return that. The assumption is that the given accumulators will not be used any more after
     * having been passed to this function.
     *
     * @param a An accumulator to merge
     * @param b Another accumulator to merge
     * @return The accumulator with the merged state
     */
    ACC merge(ACC a, ACC b);
}
