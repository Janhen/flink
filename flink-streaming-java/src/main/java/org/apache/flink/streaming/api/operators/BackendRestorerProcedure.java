/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.operators;

import org.apache.flink.core.fs.CloseableRegistry;
import org.apache.flink.runtime.state.StateObject;
import org.apache.flink.util.Disposable;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.FlinkException;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.function.FunctionWithException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 这个类实现创建(并可能恢复)状态后端的逻辑。恢复逻辑考虑多个要进行恢复的快照的优先选项，其中所有选项都应该为后端重新
 * 创建相同的状态。当我们无法从具有最高优先级的快照(通常是“最快”恢复的快照)进行恢复时，我们将退回到具有下一个最高
 * 优先级的快照。我们还负责清除失败的恢复尝试。我们只有在恢复调用期间出现问题时才会重新尝试，并且只有在所有快照替代方案
 * 都耗尽且全部失败后才会停止。
 *
 * This class implements the logic that creates (and potentially restores) a state backend. The
 * restore logic considers multiple, prioritized options of snapshots to restore from, where all of
 * the options should recreate the same state for the backend. When we fail to restore from the
 * snapshot with the highest priority (typically the "fastest" to restore), we fallback to the next
 * snapshot with the next highest priority. We also take care of cleaning up from failed restore
 * attempts. We only reattempt when the problem occurs during the restore call and will only stop
 * after all snapshot alternatives are exhausted and all failed.
 *
 * @param <T> type of the restored backend.
 * @param <S> type of the supplied snapshots from which the backend restores.
 */
public class BackendRestorerProcedure<T extends Closeable & Disposable, S extends StateObject> {

    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(BackendRestorerProcedure.class);

    /** Factory for new, fresh backends without state. */
    private final FunctionWithException<Collection<S>, T, Exception> instanceSupplier;

    /**
     * This registry is used so that recovery can participate in the task lifecycle, i.e. can be
     * canceled.
     */
    private final CloseableRegistry backendCloseableRegistry;

    /** Description of this instance for logging. */
    private final String logDescription;

    /**
     * Creates a new backend restorer using the given backend supplier and the closeable registry.
     *
     * @param instanceSupplier factory function for new, empty backend instances.
     * @param backendCloseableRegistry registry to allow participation in task lifecycle, e.g. react
     *     to cancel.
     */
    public BackendRestorerProcedure(
            @Nonnull FunctionWithException<Collection<S>, T, Exception> instanceSupplier,
            @Nonnull CloseableRegistry backendCloseableRegistry,
            @Nonnull String logDescription) {

        this.instanceSupplier = Preconditions.checkNotNull(instanceSupplier);
        this.backendCloseableRegistry = Preconditions.checkNotNull(backendCloseableRegistry);
        this.logDescription = logDescription;
    }

    /**
     * Creates a new state backend and restores it from the provided set of state snapshot
     * alternatives.
     *
     * @param restoreOptions list of prioritized state snapshot alternatives for recovery.
     * @return the created (and restored) state backend.
     * @throws Exception if the backend could not be created or restored.
     */
    @Nonnull
    public T createAndRestore(@Nonnull List<? extends Collection<S>> restoreOptions)
            throws Exception {

        if (restoreOptions.isEmpty()) {
            restoreOptions = Collections.singletonList(Collections.emptyList());
        }

        int alternativeIdx = 0;

        Exception collectedException = null;

        while (alternativeIdx < restoreOptions.size()) {

            Collection<S> restoreState = restoreOptions.get(alternativeIdx);

            ++alternativeIdx;

            // IMPORTANT: please be careful when modifying the log statements because they are used
            // for validation in
            // the automatic end-to-end tests. Those tests might fail if they are not aligned with
            // the log message!
            if (restoreState.isEmpty()) {
                LOG.debug("Creating {} with empty state.", logDescription);
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(
                            "Creating {} and restoring with state {} from alternative ({}/{}).",
                            logDescription,
                            restoreState,
                            alternativeIdx,
                            restoreOptions.size());
                } else {
                    LOG.debug(
                            "Creating {} and restoring with state from alternative ({}/{}).",
                            logDescription,
                            alternativeIdx,
                            restoreOptions.size());
                }
            }

            try {
                return attemptCreateAndRestore(restoreState);
            } catch (Exception ex) {

                collectedException = ExceptionUtils.firstOrSuppressed(ex, collectedException);

                LOG.warn(
                        "Exception while restoring {} from alternative ({}/{}), will retry while more "
                                + "alternatives are available.",
                        logDescription,
                        alternativeIdx,
                        restoreOptions.size(),
                        ex);

                if (backendCloseableRegistry.isClosed()) {
                    throw new FlinkException(
                            "Stopping restore attempts for already cancelled task.",
                            collectedException);
                }
            }
        }

        throw new FlinkException(
                "Could not restore "
                        + logDescription
                        + " from any of the "
                        + restoreOptions.size()
                        + " provided restore options.",
                collectedException);
    }

    private T attemptCreateAndRestore(Collection<S> restoreState) throws Exception {

        // create a new backend with necessary initialization.
        final T backendInstance = instanceSupplier.apply(restoreState);

        try {
            // register the backend with the registry to participate in task lifecycle w.r.t.
            // cancellation.
            backendCloseableRegistry.registerCloseable(backendInstance);
            return backendInstance;
        } catch (Exception ex) {
            // dispose the backend, e.g. to release native resources, if failed to register it into
            // registry.
            try {
                backendInstance.dispose();
            } catch (Exception disposeEx) {
                ex = ExceptionUtils.firstOrSuppressed(disposeEx, ex);
            }

            throw ex;
        }
    }
}
