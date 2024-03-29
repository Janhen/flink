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

package org.apache.flink.api.common.operators.base;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.aggregators.AggregatorRegistry;
import org.apache.flink.api.common.functions.AbstractRichFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.operators.BinaryOperatorInformation;
import org.apache.flink.api.common.operators.DualInputOperator;
import org.apache.flink.api.common.operators.IterationOperator;
import org.apache.flink.api.common.operators.Operator;
import org.apache.flink.api.common.operators.OperatorInformation;
import org.apache.flink.api.common.operators.util.UserCodeClassWrapper;
import org.apache.flink.api.common.operators.util.UserCodeWrapper;
import org.apache.flink.util.Visitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DeltaIteration 类似于 {@link BulkIterationBase}，但在各个迭代步骤中维护状态。该状态称为<i>解决方案集<i>，可以
 * 通过 {@link #getSolutionSet()} 获得，并通过与它连接(或 CoGrouping)来访问。解决方案集通过产生一个增量来更新，这个
 * 增量在每个迭代步骤结束时合并到解决方案集中。
 *
 * <p>必须通过为解决方案集 ({@link #setSolutionSetDelta(org.apache.flink.api.common.operators.Operator)})
 *   和新工作集（将反馈，{@link #setNextWorkset(org.apache.flink.api.common.operators.Operator)})。
 *   DeltaIteration 本身代表迭代终止后的结果。当反馈数据集（工作集）为空时，增量迭代终止。此外，最大步数作为后备
 *   终止保护给出。
 *
 * <p>解决方案集中的元素由一个键唯一标识。合并解决方案集 delta 时，将替换具有相同键的包含元素。
 *
 * <p>这个类是 {@code DualInputOperator} 的子类。解决方案集被视为第一个输入，工作集被视为第二个输入。
 *
 * A DeltaIteration is similar to a {@link BulkIterationBase}, but maintains state across the
 * individual iteration steps. The state is called the <i>solution set</i>, can be obtained via
 * {@link #getSolutionSet()}, and be accessed by joining (or CoGrouping) with it. The solution set
 * is updated by producing a delta for it, which is merged into the solution set at the end of each
 * iteration step.
 *
 * <p>The delta iteration must be closed by setting a delta for the solution set ({@link
 * #setSolutionSetDelta(org.apache.flink.api.common.operators.Operator)}) and the new workset (the
 * data set that will be fed back, {@link
 * #setNextWorkset(org.apache.flink.api.common.operators.Operator)}). The DeltaIteration itself
 * represents the result after the iteration has terminated. Delta iterations terminate when the
 * feed back data set (the workset) is empty. In addition, a maximum number of steps is given as a
 * fall back termination guard.
 *
 * <p>Elements in the solution set are uniquely identified by a key. When merging the solution set
 * delta, contained elements with the same key are replaced.
 *
 * <p>This class is a subclass of {@code DualInputOperator}. The solution set is considered the
 * first input, the workset is considered the second input.
 */
@Internal
public class DeltaIterationBase<ST, WT> extends DualInputOperator<ST, WT, ST, AbstractRichFunction>
        implements IterationOperator {

    private final Operator<ST> solutionSetPlaceholder;

    private final Operator<WT> worksetPlaceholder;

    private Operator<ST> solutionSetDelta;

    private Operator<WT> nextWorkset;

    /** The positions of the keys in the solution tuple. */
    private final int[] solutionSetKeyFields;

    /** The maximum number of iterations. Possibly used only as a safeguard. */
    private int maxNumberOfIterations = -1;

    private final AggregatorRegistry aggregators = new AggregatorRegistry();

    private boolean solutionSetUnManaged;

    // --------------------------------------------------------------------------------------------

    public DeltaIterationBase(BinaryOperatorInformation<ST, WT, ST> operatorInfo, int keyPosition) {
        this(operatorInfo, new int[] {keyPosition});
    }

    public DeltaIterationBase(
            BinaryOperatorInformation<ST, WT, ST> operatorInfo, int[] keyPositions) {
        this(operatorInfo, keyPositions, "<Unnamed Delta Iteration>");
    }

    public DeltaIterationBase(
            BinaryOperatorInformation<ST, WT, ST> operatorInfo, int keyPosition, String name) {
        this(operatorInfo, new int[] {keyPosition}, name);
    }

    public DeltaIterationBase(
            BinaryOperatorInformation<ST, WT, ST> operatorInfo, int[] keyPositions, String name) {
        super(
                new UserCodeClassWrapper<AbstractRichFunction>(AbstractRichFunction.class),
                operatorInfo,
                name);
        this.solutionSetKeyFields = keyPositions;
        solutionSetPlaceholder =
                new SolutionSetPlaceHolder<ST>(
                        this, new OperatorInformation<ST>(operatorInfo.getFirstInputType()));
        worksetPlaceholder =
                new WorksetPlaceHolder<WT>(
                        this, new OperatorInformation<WT>(operatorInfo.getSecondInputType()));
    }

    // --------------------------------------------------------------------------------------------

    public int[] getSolutionSetKeyFields() {
        return this.solutionSetKeyFields;
    }

    public void setMaximumNumberOfIterations(int maxIterations) {
        this.maxNumberOfIterations = maxIterations;
    }

    public int getMaximumNumberOfIterations() {
        return this.maxNumberOfIterations;
    }

    @Override
    public AggregatorRegistry getAggregators() {
        return this.aggregators;
    }

    // --------------------------------------------------------------------------------------------
    // Getting / Setting of the step function input place-holders
    // --------------------------------------------------------------------------------------------

    /**
     * Gets the contract that represents the solution set for the step function.
     *
     * @return The solution set for the step function.
     */
    public Operator<ST> getSolutionSet() {
        return this.solutionSetPlaceholder;
    }

    /**
     * Gets the contract that represents the workset for the step function.
     *
     * @return The workset for the step function.
     */
    public Operator<WT> getWorkset() {
        return this.worksetPlaceholder;
    }

    /**
     * Sets the contract of the step function that represents the next workset. This contract is
     * considered one of the two sinks of the step function (the other one being the solution set
     * delta).
     *
     * @param result The contract representing the next workset.
     */
    public void setNextWorkset(Operator<WT> result) {
        this.nextWorkset = result;
    }

    /**
     * Gets the contract that has been set as the next workset.
     *
     * @return The contract that has been set as the next workset.
     */
    public Operator<WT> getNextWorkset() {
        return this.nextWorkset;
    }

    /**
     * Sets the contract of the step function that represents the solution set delta. This contract
     * is considered one of the two sinks of the step function (the other one being the next
     * workset).
     *
     * @param delta The contract representing the solution set delta.
     */
    public void setSolutionSetDelta(Operator<ST> delta) {
        this.solutionSetDelta = delta;
    }

    /**
     * Gets the contract that has been set as the solution set delta.
     *
     * @return The contract that has been set as the solution set delta.
     */
    public Operator<ST> getSolutionSetDelta() {
        return this.solutionSetDelta;
    }

    // --------------------------------------------------------------------------------------------
    // Getting / Setting the Inputs
    // --------------------------------------------------------------------------------------------

    /**
     * Returns the initial solution set input, or null, if none is set.
     *
     * @return The iteration's initial solution set input.
     */
    public Operator<ST> getInitialSolutionSet() {
        return getFirstInput();
    }

    /**
     * Returns the initial workset input, or null, if none is set.
     *
     * @return The iteration's workset input.
     */
    public Operator<WT> getInitialWorkset() {
        return getSecondInput();
    }

    /**
     * Sets the given input as the initial solution set.
     *
     * @param input The contract to set the initial solution set.
     */
    public void setInitialSolutionSet(Operator<ST> input) {
        setFirstInput(input);
    }

    /**
     * Sets the given input as the initial workset.
     *
     * @param input The contract to set as the initial workset.
     */
    public void setInitialWorkset(Operator<WT> input) {
        setSecondInput(input);
    }

    /**
     * DeltaIteration meta operator cannot have broadcast inputs.
     *
     * @return An empty map.
     */
    public Map<String, Operator<?>> getBroadcastInputs() {
        return Collections.emptyMap();
    }

    /**
     * The DeltaIteration meta operator cannot have broadcast inputs. This method always throws an
     * exception.
     *
     * @param name Ignored.
     * @param root Ignored.
     */
    public void setBroadcastVariable(String name, Operator<?> root) {
        throw new UnsupportedOperationException(
                "The DeltaIteration meta operator cannot have broadcast inputs.");
    }

    /**
     * The DeltaIteration meta operator cannot have broadcast inputs. This method always throws an
     * exception.
     *
     * @param inputs Ignored
     */
    public <X> void setBroadcastVariables(Map<String, Operator<X>> inputs) {
        throw new UnsupportedOperationException(
                "The DeltaIteration meta operator cannot have broadcast inputs.");
    }

    /**
     * Sets whether to keep the solution set in managed memory (safe against heap exhaustion) or
     * unmanaged memory (objects on heap).
     *
     * @param solutionSetUnManaged True to keep the solution set in unmanaged memory, false to keep
     *     it in managed memory.
     * @see #isSolutionSetUnManaged()
     */
    public void setSolutionSetUnManaged(boolean solutionSetUnManaged) {
        this.solutionSetUnManaged = solutionSetUnManaged;
    }

    /**
     * gets whether the solution set is in managed or unmanaged memory.
     *
     * @return True, if the solution set is in unmanaged memory (object heap), false if in managed
     *     memory.
     * @see #setSolutionSetUnManaged(boolean)
     */
    public boolean isSolutionSetUnManaged() {
        return solutionSetUnManaged;
    }

    // --------------------------------------------------------------------------------------------
    // Place-holder Operators
    // --------------------------------------------------------------------------------------------

    /**
     * Specialized operator to use as a recognizable place-holder for the working set input to the
     * step function.
     */
    public static class WorksetPlaceHolder<WT> extends Operator<WT> {

        private final DeltaIterationBase<?, WT> containingIteration;

        public WorksetPlaceHolder(
                DeltaIterationBase<?, WT> container, OperatorInformation<WT> operatorInfo) {
            super(operatorInfo, "Workset");
            this.containingIteration = container;
        }

        public DeltaIterationBase<?, WT> getContainingWorksetIteration() {
            return this.containingIteration;
        }

        @Override
        public void accept(Visitor<Operator<?>> visitor) {
            visitor.preVisit(this);
            visitor.postVisit(this);
        }

        @Override
        public UserCodeWrapper<?> getUserCodeWrapper() {
            return null;
        }
    }

    /**
     * Specialized operator to use as a recognizable place-holder for the solution set input to the
     * step function.
     */
    public static class SolutionSetPlaceHolder<ST> extends Operator<ST> {

        protected final DeltaIterationBase<ST, ?> containingIteration;

        public SolutionSetPlaceHolder(
                DeltaIterationBase<ST, ?> container, OperatorInformation<ST> operatorInfo) {
            super(operatorInfo, "Solution Set");
            this.containingIteration = container;
        }

        public DeltaIterationBase<ST, ?> getContainingWorksetIteration() {
            return this.containingIteration;
        }

        @Override
        public void accept(Visitor<Operator<?>> visitor) {
            visitor.preVisit(this);
            visitor.postVisit(this);
        }

        @Override
        public UserCodeWrapper<?> getUserCodeWrapper() {
            return null;
        }
    }

    @Override
    protected List<ST> executeOnCollections(
            List<ST> inputData1,
            List<WT> inputData2,
            RuntimeContext runtimeContext,
            ExecutionConfig executionConfig) {
        throw new UnsupportedOperationException();
    }
}
