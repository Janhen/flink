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

package org.apache.flink.cep.pattern;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.java.ClosureCleaner;
import org.apache.flink.cep.nfa.NFA;
import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.pattern.Quantifier.ConsumingStrategy;
import org.apache.flink.cep.pattern.Quantifier.Times;
import org.apache.flink.cep.pattern.conditions.BooleanConditions;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichAndCondition;
import org.apache.flink.cep.pattern.conditions.RichOrCondition;
import org.apache.flink.cep.pattern.conditions.SubtypeCondition;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Preconditions;

/**
 * 模式定义的基类。
 *
 * <p>模式定义被{@link org.apache.flink.cep.nfa.compiler.NFACompiler}用来创建一个{@link NFA}。
 *
 * Base class for a pattern definition.
 *
 * <p>A pattern definition is used by {@link org.apache.flink.cep.nfa.compiler.NFACompiler} to
 * create a {@link NFA}.
 *
 * <pre>{@code
 * Pattern<T, F> pattern = Pattern.<T>begin("start")
 *   .next("middle").subtype(F.class)
 *   .followedBy("end").where(new MyCondition());
 * }</pre>
 *
 * @param <T> Base type of the elements appearing in the pattern
 * @param <F> Subtype of T to which the current pattern operator is constrained
 */
public class Pattern<T, F extends T> {

    /** Name of the pattern. */
    private final String name;

    /** Previous pattern. */
    private final Pattern<T, ? extends T> previous;

    /** The condition an event has to satisfy to be considered a matched. */
    // 事件必须满足的条件被认为是匹配的。
    private IterativeCondition<F> condition;

    /** Window length in which the pattern match has to occur. */
    // 模式匹配必须发生的窗口长度。
    private Time windowTime;

    /**
     * 模式的量词。默认设置为{@link Quantifier#one(ConsumingStrategy)}。
     *
     * A quantifier for the pattern. By default set to {@link Quantifier#one(ConsumingStrategy)}.
     */
    private Quantifier quantifier = Quantifier.one(ConsumingStrategy.STRICT);

    /** The condition an event has to satisfy to stop collecting events into looping state. */
    // 事件停止收集事件进入循环状态所必须满足的条件。
    private IterativeCondition<F> untilCondition;

    /** Applicable to a {@code times} pattern, and holds the number of times it has to appear. */
    // 适用于{@code times}模式，并保存它必须出现的次数。
    private Times times;

    // J: 匹配之后的策略    对应  AFTER MATCH SKIP
    //  SKIP PAST LAST ROW
    //  SKIP TO NEXT ROW
    //  SKIP TO LAST variable
    //  SKIP TO FIRST variable
    private final AfterMatchSkipStrategy afterMatchSkipStrategy;

    protected Pattern(
            final String name,
            final Pattern<T, ? extends T> previous,
            final ConsumingStrategy consumingStrategy,
            final AfterMatchSkipStrategy afterMatchSkipStrategy) {
        this.name = name;
        this.previous = previous;
        this.quantifier = Quantifier.one(consumingStrategy);
        this.afterMatchSkipStrategy = afterMatchSkipStrategy;
    }

    public Pattern<T, ? extends T> getPrevious() {
        return previous;
    }

    public Times getTimes() {
        return times;
    }

    public String getName() {
        return name;
    }

    public Time getWindowTime() {
        return windowTime;
    }

    public Quantifier getQuantifier() {
        return quantifier;
    }

    public IterativeCondition<F> getCondition() {
        if (condition != null) {
            return condition;
        } else {
            return BooleanConditions.trueFunction();
        }
    }

    public IterativeCondition<F> getUntilCondition() {
        return untilCondition;
    }

    /**
     * 开始一个新的模式序列。提供的名称是新序列的初始模式之一。此外，还设置了事件序列的基本类型。
     *
     * Starts a new pattern sequence. The provided name is the one of the initial pattern of the new
     * sequence. Furthermore, the base type of the event sequence is set.
     *
     * @param name The name of starting pattern of the new pattern sequence
     * @param <X> Base type of the event pattern
     * @return The first pattern of a pattern sequence
     */
    public static <X> Pattern<X, X> begin(final String name) {
        return new Pattern<>(name, null, ConsumingStrategy.STRICT, AfterMatchSkipStrategy.noSkip());
    }

    /**
     * Starts a new pattern sequence. The provided name is the one of the initial pattern of the new
     * sequence. Furthermore, the base type of the event sequence is set.
     *
     * @param name The name of starting pattern of the new pattern sequence
     * @param afterMatchSkipStrategy the {@link AfterMatchSkipStrategy.SkipStrategy} to use after
     *     each match.
     *     {@link AfterMatchSkipStrategy} 每次匹配后使用SkipStrategy。
     * @param <X> Base type of the event pattern
     * @return The first pattern of a pattern sequence
     */
    public static <X> Pattern<X, X> begin(
            final String name, final AfterMatchSkipStrategy afterMatchSkipStrategy) {
        return new Pattern<X, X>(name, null, ConsumingStrategy.STRICT, afterMatchSkipStrategy);
    }

    /**
     * 添加事件必须满足的条件，以视为匹配。如果已经设置了另一个条件，则新的条件将通过逻辑{@code AND}与前一个条件结合。
     * 在其他情况下，这是唯一的条件。
     *
     * Adds a condition that has to be satisfied by an event in order to be considered a match. If
     * another condition has already been set, the new one is going to be combined with the previous
     * with a logical {@code AND}. In other case, this is going to be the only condition.
     *
     * @param condition The condition as an {@link IterativeCondition}.
     * @return The pattern with the new condition is set.
     */
    public Pattern<T, F> where(IterativeCondition<F> condition) {
        Preconditions.checkNotNull(condition, "The condition cannot be null.");

        ClosureCleaner.clean(condition, ExecutionConfig.ClosureCleanerLevel.RECURSIVE, true);
        if (this.condition == null) {
            this.condition = condition;
        } else {
            this.condition = new RichAndCondition<>(this.condition, condition);
        }
        return this;
    }

    /**
     * 添加事件必须满足的条件，以视为匹配。如果已经设置了另一个条件，则新的条件将通过逻辑{@code OR}与前一个条件结合。
     * 在其他情况下，这是唯一的条件。
     *
     * Adds a condition that has to be satisfied by an event in order to be considered a match. If
     * another condition has already been set, the new one is going to be combined with the previous
     * with a logical {@code OR}. In other case, this is going to be the only condition.
     *
     * @param condition The condition as an {@link IterativeCondition}.
     * @return The pattern with the new condition is set.
     */
    public Pattern<T, F> or(IterativeCondition<F> condition) {
        Preconditions.checkNotNull(condition, "The condition cannot be null.");

        ClosureCleaner.clean(condition, ExecutionConfig.ClosureCleanerLevel.RECURSIVE, true);

        if (this.condition == null) {
            this.condition = condition;
        } else {
            this.condition = new RichOrCondition<>(this.condition, condition);
        }
        return this;
    }

    /**
     * 在当前模式上应用子类型约束。这意味着一个事件必须是给定的子类型才能匹配。
     *
     * Applies a subtype constraint on the current pattern. This means that an event has to be of
     * the given subtype in order to be matched.
     *
     * @param subtypeClass Class of the subtype
     * @param <S> Type of the subtype
     * @return The same pattern with the new subtype constraint
     */
    public <S extends F> Pattern<T, S> subtype(final Class<S> subtypeClass) {
        Preconditions.checkNotNull(subtypeClass, "The class cannot be null.");

        if (condition == null) {
            this.condition = new SubtypeCondition<F>(subtypeClass);
        } else {
            this.condition =
                    new RichAndCondition<>(condition, new SubtypeCondition<F>(subtypeClass));
        }

        @SuppressWarnings("unchecked")
        Pattern<T, S> result = (Pattern<T, S>) this;

        return result;
    }

    /**
     * 为循环状态应用停止条件。它允许清除底层状态。
     *
     * Applies a stop condition for a looping state. It allows cleaning the underlying state.
     *
     * @param untilCondition a condition an event has to satisfy to stop collecting events into
     *     looping state
     * @return The same pattern with applied untilCondition
     */
    public Pattern<T, F> until(IterativeCondition<F> untilCondition) {
        Preconditions.checkNotNull(untilCondition, "The condition cannot be null");

        if (this.untilCondition != null) {
            throw new MalformedPatternException("Only one until condition can be applied.");
        }

        if (!quantifier.hasProperty(Quantifier.QuantifierProperty.LOOPING)) {
            throw new MalformedPatternException(
                    "The until condition is only applicable to looping states.");
        }

        ClosureCleaner.clean(untilCondition, ExecutionConfig.ClosureCleanerLevel.RECURSIVE, true);
        this.untilCondition = untilCondition;

        return this;
    }

    /**
     * 定义匹配模式必须在其中完成才能被认为有效的最大时间间隔。这个间隔对应于第一个事件和最后一个事件之间的最大时间间隔。
     *
     * Defines the maximum time interval in which a matching pattern has to be completed in order to
     * be considered valid. This interval corresponds to the maximum time gap between first and the
     * last event.
     *
     * @param windowTime Time of the matching window
     * @return The same pattern operator with the new window length
     */
    public Pattern<T, F> within(Time windowTime) {
        if (windowTime != null) {
            this.windowTime = windowTime;
        }

        return this;
    }

    /**
     * 向现有模式追加一个新模式。新模式强制严格的时间连续。这意味着，只有当匹配此模式的事件直接跟在前一个匹配事件之后时，
     * 整个模式序列才匹配。因此，在两个匹配事件之间不可能有任何事件。
     *
     * Appends a new pattern to the existing one. The new pattern enforces strict temporal
     * contiguity. This means that the whole pattern sequence matches only if an event which matches
     * this pattern directly follows the preceding matching event. Thus, there cannot be any events
     * in between two matching events.
     *
     * @param name Name of the new pattern
     * @return A new pattern which is appended to this one
     */
    public Pattern<T, T> next(final String name) {
        return new Pattern<>(name, this, ConsumingStrategy.STRICT, afterMatchSkipStrategy);
    }

    /**
     * 向现有模式追加一个新模式。新模式强制在前面匹配的事件之后没有匹配此模式的事件。
     *
     * Appends a new pattern to the existing one. The new pattern enforces that there is no event
     * matching this pattern right after the preceding matched event.
     *
     * @param name Name of the new pattern
     * @return A new pattern which is appended to this one
     */
    public Pattern<T, T> notNext(final String name) {
        if (quantifier.hasProperty(Quantifier.QuantifierProperty.OPTIONAL)) {
            throw new UnsupportedOperationException(
                    "Specifying a pattern with an optional path to NOT condition is not supported yet. "
                            + "You can simulate such pattern with two independent patterns, one with and the other without "
                            + "the optional part.");
        }
        return new Pattern<>(name, this, ConsumingStrategy.NOT_NEXT, afterMatchSkipStrategy);
    }

    /**
     * 向现有模式追加一个新模式。新模式强制非严格的时间连续。这意味着该模式的匹配事件和前面的匹配事件可能与其他被忽略的事件交织在一起。
     *
     * Appends a new pattern to the existing one. The new pattern enforces non-strict temporal
     * contiguity. This means that a matching event of this pattern and the preceding matching event
     * might be interleaved with other events which are ignored.
     *
     * @param name Name of the new pattern
     * @return A new pattern which is appended to this one
     */
    public Pattern<T, T> followedBy(final String name) {
        return new Pattern<>(name, this, ConsumingStrategy.SKIP_TILL_NEXT, afterMatchSkipStrategy);
    }

    /**
     * Appends a new pattern to the existing one. The new pattern enforces that there is no event
     * matching this pattern between the preceding pattern and succeeding this one.
     *
     * <p><b>NOTE:</b> There has to be other pattern after this one.
     *
     * @param name Name of the new pattern
     * @return A new pattern which is appended to this one
     */
    public Pattern<T, T> notFollowedBy(final String name) {
        if (quantifier.hasProperty(Quantifier.QuantifierProperty.OPTIONAL)) {
            throw new UnsupportedOperationException(
                    "Specifying a pattern with an optional path to NOT condition is not supported yet. "
                            + "You can simulate such pattern with two independent patterns, one with and the other without "
                            + "the optional part.");
        }
        return new Pattern<>(name, this, ConsumingStrategy.NOT_FOLLOW, afterMatchSkipStrategy);
    }

    /**
     * 向现有模式追加一个新模式。新模式强制非严格的时间连续。这意味着该模式的匹配事件和前面的匹配事件可能与其他被忽略的事件交织在一起。
     *
     * Appends a new pattern to the existing one. The new pattern enforces non-strict temporal
     * contiguity. This means that a matching event of this pattern and the preceding matching event
     * might be interleaved with other events which are ignored.
     *
     * @param name Name of the new pattern
     * @return A new pattern which is appended to this one
     */
    public Pattern<T, T> followedByAny(final String name) {
        return new Pattern<>(name, this, ConsumingStrategy.SKIP_TILL_ANY, afterMatchSkipStrategy);
    }

    /**
     * 指定该模式对于模式序列的最终匹配是可选的。
     *
     * Specifies that this pattern is optional for a final match of the pattern sequence to happen.
     *
     * @return The same pattern as optional.
     * @throws MalformedPatternException if the quantifier is not applicable to this pattern.
     */
    public Pattern<T, F> optional() {
        checkIfPreviousPatternGreedy();
        quantifier.optional();
        return this;
    }

    /**
     * 指定此模式可以出现{@code一次或多次}次。这意味着至少一个，最多无限个事件可以匹配到这个模式。
     *
     * Specifies that this pattern can occur {@code one or more} times. This means at least one and
     * at most infinite number of events can be matched to this pattern.
     *
     * <p>If this quantifier is enabled for a pattern {@code A.oneOrMore().followedBy(B)} and a
     * sequence of events {@code A1 A2 B} appears, this will generate patterns: {@code A1 B} and
     * {@code A1 A2 B}. See also {@link #allowCombinations()}.
     *
     * @return The same pattern with a {@link Quantifier#looping(ConsumingStrategy)} quantifier
     *     applied.
     * @throws MalformedPatternException if the quantifier is not applicable to this pattern.
     */
    public Pattern<T, F> oneOrMore() {
        checkIfNoNotPattern();
        checkIfQuantifierApplied();
        this.quantifier = Quantifier.looping(quantifier.getConsumingStrategy());
        this.times = Times.of(1);
        return this;
    }

    /**
     * 指定此模式为贪婪模式。这意味着将有尽可能多的事件与此模式匹配。
     *
     * Specifies that this pattern is greedy. This means as many events as possible will be matched
     * to this pattern.
     *
     * @return The same pattern with {@link Quantifier#greedy} set to true.
     * @throws MalformedPatternException if the quantifier is not applicable to this pattern.
     */
    public Pattern<T, F> greedy() {
        checkIfNoNotPattern();
        checkIfNoGroupPattern();
        this.quantifier.greedy();
        return this;
    }

    /**
     * Specifies exact number of times that this pattern should be matched.
     *
     * @param times number of times matching event must appear
     * @return The same pattern with number of times applied
     * @throws MalformedPatternException if the quantifier is not applicable to this pattern.
     */
    public Pattern<T, F> times(int times) {
        checkIfNoNotPattern();
        checkIfQuantifierApplied();
        Preconditions.checkArgument(times > 0, "You should give a positive number greater than 0.");
        this.quantifier = Quantifier.times(quantifier.getConsumingStrategy());
        this.times = Times.of(times);
        return this;
    }

    /**
     * Specifies that the pattern can occur between from and to times.
     *
     * @param from number of times matching event must appear at least
     * @param to number of times matching event must appear at most
     * @return The same pattern with the number of times range applied
     * @throws MalformedPatternException if the quantifier is not applicable to this pattern.
     */
    public Pattern<T, F> times(int from, int to) {
        checkIfNoNotPattern();
        checkIfQuantifierApplied();
        this.quantifier = Quantifier.times(quantifier.getConsumingStrategy());
        if (from == 0) {
            this.quantifier.optional();
            from = 1;
        }
        this.times = Times.of(from, to);
        return this;
    }

    /**
     * Specifies that this pattern can occur the specified times at least. This means at least the
     * specified times and at most infinite number of events can be matched to this pattern.
     *
     * @return The same pattern with a {@link Quantifier#looping(ConsumingStrategy)} quantifier
     *     applied.
     * @throws MalformedPatternException if the quantifier is not applicable to this pattern.
     */
    public Pattern<T, F> timesOrMore(int times) {
        checkIfNoNotPattern();
        checkIfQuantifierApplied();
        this.quantifier = Quantifier.looping(quantifier.getConsumingStrategy());
        this.times = Times.of(times);
        return this;
    }

    /**
     * 这个选项只适用于{@link Quantifier#looping(ConsumingStrategy)}和
     * {@link Quantifier#times(ConsumingStrategy)}模式，它允许更灵活的匹配事件。
     *
     * Applicable only to {@link Quantifier#looping(ConsumingStrategy)} and {@link
     * Quantifier#times(ConsumingStrategy)} patterns, this option allows more flexibility to the
     * matching events.
     *
     * <p>If {@code allowCombinations()} is not applied for a pattern {@code
     * A.oneOrMore().followedBy(B)} and a sequence of events {@code A1 A2 B} appears, this will
     * generate patterns: {@code A1 B} and {@code A1 A2 B}. If this method is applied, we will have
     * {@code A1 B}, {@code A2 B} and {@code A1 A2 B}.
     *
     * @return The same pattern with the updated quantifier. *
     * @throws MalformedPatternException if the quantifier is not applicable to this pattern.
     */
    public Pattern<T, F> allowCombinations() {
        quantifier.combinations();
        return this;
    }

    /**
     * 与{@link Pattern#oneOrMore()}或{@link Pattern#times(int)}一起工作。指定任何不匹配的元素终止循环。
     *
     * Works in conjunction with {@link Pattern#oneOrMore()} or {@link Pattern#times(int)}.
     * Specifies that any not matching element breaks the loop.
     *
     * <p>E.g. a pattern like:
     *
     * <pre>{@code
     * Pattern.<Event>begin("start").where(new SimpleCondition<Event>() {
     *      @Override
     *      public boolean filter(Event value) throws Exception {
     *          return value.getName().equals("c");
     *      }
     * })
     * .followedBy("middle").where(new SimpleCondition<Event>() {
     *      @Override
     *      public boolean filter(Event value) throws Exception {
     *          return value.getName().equals("a");
     *      }
     * }).oneOrMore().consecutive()
     * .followedBy("end1").where(new SimpleCondition<Event>() {
     *      @Override
     *      public boolean filter(Event value) throws Exception {
     *          return value.getName().equals("b");
     *      }
     * });
     * }</pre>
     *
     * <p>for a sequence: C D A1 A2 A3 D A4 B
     *
     * <p>will generate matches: {C A1 B}, {C A1 A2 B}, {C A1 A2 A3 B}
     *
     * <p>By default a relaxed continuity is applied.
     *
     * @return pattern with continuity changed to strict
     */
    public Pattern<T, F> consecutive() {
        quantifier.consecutive();
        return this;
    }

    /**
     * Starts a new pattern sequence. The provided pattern is the initial pattern of the new
     * sequence.
     *
     * @param group the pattern to begin with
     * @param afterMatchSkipStrategy the {@link AfterMatchSkipStrategy.SkipStrategy} to use after
     *     each match.
     * @return The first pattern of a pattern sequence
     */
    public static <T, F extends T> GroupPattern<T, F> begin(
            final Pattern<T, F> group, final AfterMatchSkipStrategy afterMatchSkipStrategy) {
        return new GroupPattern<>(null, group, ConsumingStrategy.STRICT, afterMatchSkipStrategy);
    }

    /**
     * Starts a new pattern sequence. The provided pattern is the initial pattern of the new
     * sequence.
     *
     * @param group the pattern to begin with
     * @return the first pattern of a pattern sequence
     */
    public static <T, F extends T> GroupPattern<T, F> begin(Pattern<T, F> group) {
        return new GroupPattern<>(
                null, group, ConsumingStrategy.STRICT, AfterMatchSkipStrategy.noSkip());
    }

    /**
     * Appends a new group pattern to the existing one. The new pattern enforces non-strict temporal
     * contiguity. This means that a matching event of this pattern and the preceding matching event
     * might be interleaved with other events which are ignored.
     *
     * @param group the pattern to append
     * @return A new pattern which is appended to this one
     */
    public GroupPattern<T, F> followedBy(Pattern<T, F> group) {
        return new GroupPattern<>(
                this, group, ConsumingStrategy.SKIP_TILL_NEXT, afterMatchSkipStrategy);
    }

    /**
     * Appends a new group pattern to the existing one. The new pattern enforces non-strict temporal
     * contiguity. This means that a matching event of this pattern and the preceding matching event
     * might be interleaved with other events which are ignored.
     *
     * @param group the pattern to append
     * @return A new pattern which is appended to this one
     */
    public GroupPattern<T, F> followedByAny(Pattern<T, F> group) {
        return new GroupPattern<>(
                this, group, ConsumingStrategy.SKIP_TILL_ANY, afterMatchSkipStrategy);
    }

    /**
     * Appends a new group pattern to the existing one. The new pattern enforces strict temporal
     * contiguity. This means that the whole pattern sequence matches only if an event which matches
     * this pattern directly follows the preceding matching event. Thus, there cannot be any events
     * in between two matching events.
     *
     * @param group the pattern to append
     * @return A new pattern which is appended to this one
     */
    public GroupPattern<T, F> next(Pattern<T, F> group) {
        return new GroupPattern<>(this, group, ConsumingStrategy.STRICT, afterMatchSkipStrategy);
    }

    private void checkIfNoNotPattern() {
        if (quantifier.getConsumingStrategy() == ConsumingStrategy.NOT_FOLLOW
                || quantifier.getConsumingStrategy() == ConsumingStrategy.NOT_NEXT) {
            throw new MalformedPatternException("Option not applicable to NOT pattern");
        }
    }

    private void checkIfQuantifierApplied() {
        if (!quantifier.hasProperty(Quantifier.QuantifierProperty.SINGLE)) {
            throw new MalformedPatternException(
                    "Already applied quantifier to this Pattern. "
                            + "Current quantifier is: "
                            + quantifier);
        }
    }

    /** @return the pattern's {@link AfterMatchSkipStrategy.SkipStrategy} after match. */
    public AfterMatchSkipStrategy getAfterMatchSkipStrategy() {
        return afterMatchSkipStrategy;
    }

    private void checkIfNoGroupPattern() {
        if (this instanceof GroupPattern) {
            throw new MalformedPatternException("Option not applicable to group pattern");
        }
    }

    private void checkIfPreviousPatternGreedy() {
        if (previous != null
                && previous.getQuantifier().hasProperty(Quantifier.QuantifierProperty.GREEDY)) {
            throw new MalformedPatternException(
                    "Optional pattern cannot be preceded by greedy pattern");
        }
    }

    @Override
    public String toString() {
        return "Pattern{"
                + "name='"
                + name
                + '\''
                + ", previous="
                + previous
                + ", condition="
                + condition
                + ", windowTime="
                + windowTime
                + ", quantifier="
                + quantifier
                + ", untilCondition="
                + untilCondition
                + ", times="
                + times
                + ", afterMatchSkipStrategy="
                + afterMatchSkipStrategy
                + '}';
    }
}
