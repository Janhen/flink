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

package org.apache.flink.api.common.eventtime;

import org.apache.flink.annotation.Public;

import java.io.Serializable;
import java.time.Duration;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * WatermarkStrategy 定义了如何在流 source 中生成{@link Watermark}。WatermarkStrategy
 * 是{@link WatermarkGenerator} 和 {@link TimestampAssigner} 的构建工厂，前者生成水印，后者分配记录的内部时间戳。
 *
 * 这个接口分为三个部分:
 * 1)方法需要实现这个接口的实现者,
 * 2)生成器的方法构建一个{@code WatermarkStrategy}基本策略,
 * 3)便利的方法构造一个{@code WatermarkStrategy}为常见的内置策略或基于{@link WatermarkGeneratorSupplier}
 *
 * 这个接口的实现者只需要实现{@link #createWatermarkGenerator(WatermarkGeneratorSupplier.Context)}。
 * 还可以实现{@link #createTimestampAssigner(TimestampAssignerSupplier.Context)}。
 *
 * 他的构建方法，如{@link #withIdleness(Duration)}或
 * {@link #createTimestampAssigner(TimestampAssignerSupplier.Context)}
 * 创建了一个新的{@code WatermarkStrategy}，它包装并丰富了一个基本策略。调用方法所依据的策略是基本策略。
 *
 * 方便的方法，例如{@link #forBoundedOutOfOrderness(Duration)}，为公共内建策略创建一个{@code WatermarkStrategy}。
 * 这个接口是{@link Serializable}，因为水印策略可以在分布式执行期间发送给工作器。
 *
 * The WatermarkStrategy defines how to generate {@link Watermark}s in the stream sources. The
 * WatermarkStrategy is a builder/factory for the {@link WatermarkGenerator} that generates the
 * watermarks and the {@link TimestampAssigner} which assigns the internal timestamp of a record.
 *
 * <p>This interface is split into three parts: 1) methods that an implementor of this interface
 * needs to implement, 2) builder methods for building a {@code WatermarkStrategy} on a base
 * strategy, 3) convenience methods for constructing a {@code WatermarkStrategy} for common built-in
 * strategies or based on a {@link WatermarkGeneratorSupplier}
 *
 * <p>Implementors of this interface need only implement {@link
 * #createWatermarkGenerator(WatermarkGeneratorSupplier.Context)}. Optionally, you can implement
 * {@link #createTimestampAssigner(TimestampAssignerSupplier.Context)}.
 *
 * <p>The builder methods, like {@link #withIdleness(Duration)} or {@link
 * #createTimestampAssigner(TimestampAssignerSupplier.Context)} create a new {@code
 * WatermarkStrategy} that wraps and enriches a base strategy. The strategy on which the method is
 * called is the base strategy.
 *
 * <p>The convenience methods, for example {@link #forBoundedOutOfOrderness(Duration)}, create a
 * {@code WatermarkStrategy} for common built in strategies.
 *
 * <p>This interface is {@link Serializable} because watermark strategies may be shipped to workers
 * during distributed execution.
 */
@Public
public interface WatermarkStrategy<T>
        extends TimestampAssignerSupplier<T>, WatermarkGeneratorSupplier<T> {

    // ------------------------------------------------------------------------
    //  Methods that implementors need to implement.
    // ------------------------------------------------------------------------

    // 实例化根据此策略生成水印的WatermarkGenerator。
    /** Instantiates a WatermarkGenerator that generates watermarks according to this strategy. */
    @Override
    WatermarkGenerator<T> createWatermarkGenerator(WatermarkGeneratorSupplier.Context context);

    /**
     * 实例化一个{@link TimestampAssigner}，以便根据此策略分配时间戳。
     *
     * Instantiates a {@link TimestampAssigner} for assigning timestamps according to this strategy.
     */
    @Override
    default TimestampAssigner<T> createTimestampAssigner(
            TimestampAssignerSupplier.Context context) {
        // By default, this is {@link RecordTimestampAssigner},
        // for cases where records come out of a source with valid timestamps, for example from
        // Kafka.
        return new RecordTimestampAssigner<>();
    }

    // ------------------------------------------------------------------------
    //  Builder methods for enriching a base WatermarkStrategy
    // ------------------------------------------------------------------------

    /**
     * 创建一个新的{@code WatermarkStrategy}来包装这个策略，但是使用给定的{@link TimestampAssigner}
     * 通过{@link TimestampAssignerSupplier})。
     * <p>当{@link TimestampAssigner}需要额外的上下文时，可以使用它，例如访问度量系统。
     *
     * Creates a new {@code WatermarkStrategy} that wraps this strategy but instead uses the given
     * {@link TimestampAssigner} (via a {@link TimestampAssignerSupplier}).
     *
     * <p>You can use this when a {@link TimestampAssigner} needs additional context, for example
     * access to the metrics system.
     *
     * <pre>
     * {@code WatermarkStrategy<Object> wmStrategy = WatermarkStrategy
     *   .forMonotonousTimestamps()
     *   .withTimestampAssigner((ctx) -> new MetricsReportingAssigner(ctx));
     * }</pre>
     */
    default WatermarkStrategy<T> withTimestampAssigner(
            TimestampAssignerSupplier<T> timestampAssigner) {
        checkNotNull(timestampAssigner, "timestampAssigner");
        return new WatermarkStrategyWithTimestampAssigner<>(this, timestampAssigner);
    }

    /**
     * 创建一个新的{@code WatermarkStrategy}来包装这个策略，但是使用给定的{@link SerializableTimestampAssigner}。
     * <p>如果你想通过lambda函数指定一个{@link TimestampAssigner}，你可以使用这个
     *
     * Creates a new {@code WatermarkStrategy} that wraps this strategy but instead uses the given
     * {@link SerializableTimestampAssigner}.
     *
     * <p>You can use this in case you want to specify a {@link TimestampAssigner} via a lambda
     * function.
     *
     * <pre>
     * {@code WatermarkStrategy<CustomObject> wmStrategy = WatermarkStrategy
     *   .<CustomObject>forMonotonousTimestamps()
     *   .withTimestampAssigner((event, timestamp) -> event.getTimestamp());
     * }</pre>
     */
    default WatermarkStrategy<T> withTimestampAssigner(
            SerializableTimestampAssigner<T> timestampAssigner) {
        checkNotNull(timestampAssigner, "timestampAssigner");
        return new WatermarkStrategyWithTimestampAssigner<>(
                this, TimestampAssignerSupplier.of(timestampAssigner));
    }

    /**
     * Creates a new enriched {@link WatermarkStrategy} that also does idleness detection in the
     * created {@link WatermarkGenerator}.
     *
     * <p>Add an idle timeout to the watermark strategy. If no records flow in a partition of a
     * stream for that amount of time, then that partition is considered "idle" and will not hold
     * back the progress of watermarks in downstream operators.
     *
     * <p>Idleness can be important if some partitions have little data and might not have events
     * during some periods. Without idleness, these streams can stall the overall event time
     * progress of the application.
     */
    default WatermarkStrategy<T> withIdleness(Duration idleTimeout) {
        checkNotNull(idleTimeout, "idleTimeout");
        checkArgument(
                !(idleTimeout.isZero() || idleTimeout.isNegative()),
                "idleTimeout must be greater than zero");
        return new WatermarkStrategyWithIdleness<>(this, idleTimeout);
    }

    // ------------------------------------------------------------------------
    //  Convenience methods for common watermark strategies
    // ------------------------------------------------------------------------

    /**
     * Creates a watermark strategy for situations with monotonously ascending timestamps.
     *
     * <p>The watermarks are generated periodically and tightly follow the latest timestamp in the
     * data. The delay introduced by this strategy is mainly the periodic interval in which the
     * watermarks are generated.
     *
     * @see AscendingTimestampsWatermarks
     */
    static <T> WatermarkStrategy<T> forMonotonousTimestamps() {
        return (ctx) -> new AscendingTimestampsWatermarks<>();
    }

    /**
     * Creates a watermark strategy for situations where records are out of order, but you can place
     * an upper bound on how far the events are out of order. An out-of-order bound B means that
     * once the an event with timestamp T was encountered, no events older than {@code T - B} will
     * follow any more.
     *
     * <p>The watermarks are generated periodically. The delay introduced by this watermark strategy
     * is the periodic interval length, plus the out of orderness bound.
     *
     * @see BoundedOutOfOrdernessWatermarks
     */
    static <T> WatermarkStrategy<T> forBoundedOutOfOrderness(Duration maxOutOfOrderness) {
        return (ctx) -> new BoundedOutOfOrdernessWatermarks<>(maxOutOfOrderness);
    }

    /** Creates a watermark strategy based on an existing {@link WatermarkGeneratorSupplier}. */
    static <T> WatermarkStrategy<T> forGenerator(WatermarkGeneratorSupplier<T> generatorSupplier) {
        return generatorSupplier::createWatermarkGenerator;
    }

    /**
     * Creates a watermark strategy that generates no watermarks at all. This may be useful in
     * scenarios that do pure processing-time based stream processing.
     */
    static <T> WatermarkStrategy<T> noWatermarks() {
        return (ctx) -> new NoWatermarksGenerator<>();
    }
}
