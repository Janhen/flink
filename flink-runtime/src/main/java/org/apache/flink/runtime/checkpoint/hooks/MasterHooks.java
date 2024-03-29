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

package org.apache.flink.runtime.checkpoint.hooks;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.runtime.checkpoint.MasterState;
import org.apache.flink.runtime.checkpoint.MasterTriggerRestoreHook;
import org.apache.flink.runtime.concurrent.FutureUtils;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.FlinkException;
import org.apache.flink.util.LambdaUtil;
import org.apache.flink.util.Preconditions;

import org.slf4j.Logger;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/** Collection of methods to deal with checkpoint master hooks. */
// 处理检查点主钩子的方法集合。
public class MasterHooks {

    // ------------------------------------------------------------------------
    //  lifecycle
    // ------------------------------------------------------------------------

    /**
     * Resets the master hooks.
     *
     * @param hooks The hooks to reset
     * @throws FlinkException Thrown, if the hooks throw an exception.
     */
    public static void reset(
            final Collection<MasterTriggerRestoreHook<?>> hooks,
            @SuppressWarnings("unused") final Logger log)
            throws FlinkException {

        for (MasterTriggerRestoreHook<?> hook : hooks) {
            final String id = hook.getIdentifier();
            try {
                hook.reset();
            } catch (Throwable t) {
                ExceptionUtils.rethrowIfFatalErrorOrOOM(t);
                throw new FlinkException(
                        "Error while resetting checkpoint master hook '" + id + '\'', t);
            }
        }
    }

    /**
     * Closes the master hooks.
     *
     * @param hooks The hooks to close
     */
    public static void close(
            final Collection<MasterTriggerRestoreHook<?>> hooks, final Logger log) {

        for (MasterTriggerRestoreHook<?> hook : hooks) {
            try {
                hook.close();
            } catch (Throwable t) {
                log.warn(
                        "Failed to cleanly close a checkpoint master hook ("
                                + hook.getIdentifier()
                                + ")",
                        t);
            }
        }
    }

    // ------------------------------------------------------------------------
    //  checkpoint triggering
    // ------------------------------------------------------------------------

    /**
     * 触发主控钩和返回一个可完成的未来与状态。
     *
     * Trigger master hook and return a completable future with state.
     *
     * @param hook The master hook given
     * @param checkpointId The checkpoint ID of the triggering checkpoint
     * @param timestamp The (informational) timestamp for the triggering checkpoint
     * @param executor An executor that can be used for asynchronous I/O calls
     * @param <T> The type of data produced by the hook
     * @return the completable future with state
     */
    public static <T> CompletableFuture<MasterState> triggerHook(
            MasterTriggerRestoreHook<T> hook,
            long checkpointId,
            long timestamp,
            Executor executor) {

        final String id = hook.getIdentifier();
        final SimpleVersionedSerializer<T> serializer = hook.createCheckpointDataSerializer();

        try {
            // call the hook!
            final CompletableFuture<T> resultFuture =
                    hook.triggerCheckpoint(checkpointId, timestamp, executor);

            if (resultFuture == null) {
                return CompletableFuture.completedFuture(null);
            }

            return resultFuture
                    .thenApply(
                            result -> {
                                // if the result of the future is not null, return it as state
                                if (result == null) {
                                    return null;
                                } else if (serializer != null) {
                                    try {
                                        final int version = serializer.getVersion();
                                        final byte[] bytes = serializer.serialize(result);

                                        return new MasterState(id, bytes, version);
                                    } catch (Throwable t) {
                                        ExceptionUtils.rethrowIfFatalErrorOrOOM(t);
                                        throw new CompletionException(
                                                new FlinkException(
                                                        "Failed to serialize state of master hook '"
                                                                + id
                                                                + '\'',
                                                        t));
                                    }
                                } else {
                                    throw new CompletionException(
                                            new FlinkException(
                                                    "Checkpoint hook '"
                                                            + id
                                                            + " is stateful but creates no serializer"));
                                }
                            })
                    // J: 异常处理
                    .exceptionally(
                            (throwable) -> {
                                throw new CompletionException(
                                        new FlinkException(
                                                "Checkpoint master hook '"
                                                        + id
                                                        + "' produced an exception",
                                                throwable.getCause()));
                            });
        } catch (Throwable t) {
            return FutureUtils.completedExceptionally(
                    new FlinkException(
                            "Error while triggering checkpoint master hook '" + id + '\'', t));
        }
    }

    // ------------------------------------------------------------------------
    //  checkpoint restoring
    // ------------------------------------------------------------------------

    /**
     * 调用给定检查点主钩子的恢复方法，并将给定的主状态传递给它们，在找到具有匹配名称的状态的地方。
     *
     * 如果找到state，但没有找到同名的钩子，该方法抛出一个异常，除非设置了{@code allowUnmatchedState}标志。
     *
     * Calls the restore method given checkpoint master hooks and passes the given master state to
     * them where state with a matching name is found.
     *
     * <p>If state is found and no hook with the same name is found, the method throws an exception,
     * unless the {@code allowUnmatchedState} flag is set.
     *
     * @param masterHooks The hooks to call restore on
     * @param states The state to pass to the hooks
     * @param checkpointId The checkpoint ID of the restored checkpoint
     * @param allowUnmatchedState If true, the method fails if not all states are picked up by a
     *     hook.
     * @param log The logger for log messages
     * @throws FlinkException Thrown, if the hooks throw an exception, or the state+ deserialization
     *     fails.
     */
    public static void restoreMasterHooks(
            final Map<String, MasterTriggerRestoreHook<?>> masterHooks,
            final Collection<MasterState> states,
            final long checkpointId,
            final boolean allowUnmatchedState,
            final Logger log)
            throws FlinkException {

        // early out
        if (states == null || states.isEmpty() || masterHooks == null || masterHooks.isEmpty()) {
            log.info("No master state to restore");
            return;
        }

        log.info("Calling master restore hooks");

        // collect the hooks
        final LinkedHashMap<String, MasterTriggerRestoreHook<?>> allHooks =
                new LinkedHashMap<>(masterHooks);

        // first, deserialize all hook state
        final ArrayList<Tuple2<MasterTriggerRestoreHook<?>, Object>> hooksAndStates =
                new ArrayList<>();

        for (MasterState state : states) {
            if (state != null) {
                final String name = state.name();
                final MasterTriggerRestoreHook<?> hook = allHooks.remove(name);

                if (hook != null) {
                    log.debug("Found state to restore for hook '{}'", name);

                    Object deserializedState = deserializeState(state, hook);
                    hooksAndStates.add(new Tuple2<>(hook, deserializedState));
                } else if (!allowUnmatchedState) {
                    throw new IllegalStateException(
                            "Found state '" + state.name() + "' which is not resumed by any hook.");
                } else {
                    log.info("Dropping unmatched state from '{}'", name);
                }
            }
        }

        // now that all is deserialized, call the hooks
        for (Tuple2<MasterTriggerRestoreHook<?>, Object> hookAndState : hooksAndStates) {
            restoreHook(hookAndState.f1, hookAndState.f0, checkpointId);
        }

        // trigger the remaining hooks without checkpointed state
        for (MasterTriggerRestoreHook<?> hook : allHooks.values()) {
            restoreHook(null, hook, checkpointId);
        }
    }

    private static <T> T deserializeState(MasterState state, MasterTriggerRestoreHook<?> hook)
            throws FlinkException {
        @SuppressWarnings("unchecked")
        final MasterTriggerRestoreHook<T> typedHook = (MasterTriggerRestoreHook<T>) hook;
        final String id = hook.getIdentifier();

        try {
            final SimpleVersionedSerializer<T> deserializer =
                    typedHook.createCheckpointDataSerializer();
            if (deserializer == null) {
                throw new FlinkException(
                        "null serializer for state of hook " + hook.getIdentifier());
            }

            return deserializer.deserialize(state.version(), state.bytes());
        } catch (Throwable t) {
            throw new FlinkException("Cannot deserialize state for master hook '" + id + '\'', t);
        }
    }

    private static <T> void restoreHook(
            final Object state, final MasterTriggerRestoreHook<?> hook, final long checkpointId)
            throws FlinkException {

        @SuppressWarnings("unchecked")
        final T typedState = (T) state;

        @SuppressWarnings("unchecked")
        final MasterTriggerRestoreHook<T> typedHook = (MasterTriggerRestoreHook<T>) hook;

        try {
            typedHook.restoreCheckpoint(checkpointId, typedState);
        } catch (FlinkException e) {
            throw e;
        } catch (Throwable t) {
            // catch all here, including Errors that may come from dependency and classpath issues
            ExceptionUtils.rethrowIfFatalError(t);
            throw new FlinkException(
                    "Error while calling restoreCheckpoint on checkpoint hook '"
                            + hook.getIdentifier()
                            + '\'',
                    t);
        }
    }

    // ------------------------------------------------------------------------
    //  hook management
    // ------------------------------------------------------------------------

    /**
     * 包装一个钩子，以便在钩子被调用时应用用户代码类加载器。
     *
     * Wraps a hook such that the user-code classloader is applied when the hook is invoked.
     *
     * @param hook the hook to wrap
     * @param userClassLoader the classloader to use
     */
    public static <T> MasterTriggerRestoreHook<T> wrapHook(
            MasterTriggerRestoreHook<T> hook, ClassLoader userClassLoader) {

        return new WrappedMasterHook<>(hook, userClassLoader);
    }

    private static class WrappedMasterHook<T> implements MasterTriggerRestoreHook<T> {

        private final MasterTriggerRestoreHook<T> hook;
        private final ClassLoader userClassLoader;

        WrappedMasterHook(MasterTriggerRestoreHook<T> hook, ClassLoader userClassLoader) {
            this.hook = Preconditions.checkNotNull(hook);
            this.userClassLoader = Preconditions.checkNotNull(userClassLoader);
        }

        @Override
        public void reset() throws Exception {
            LambdaUtil.withContextClassLoader(userClassLoader, hook::reset);
        }

        @Override
        public void close() throws Exception {
            LambdaUtil.withContextClassLoader(userClassLoader, hook::close);
        }

        @Override
        public String getIdentifier() {
            return LambdaUtil.withContextClassLoader(userClassLoader, hook::getIdentifier);
        }

        @Nullable
        @Override
        public CompletableFuture<T> triggerCheckpoint(
                long checkpointId, long timestamp, final Executor executor) throws Exception {
            final Executor wrappedExecutor =
                    command -> executor.execute(new WrappedCommand(userClassLoader, command));

            return LambdaUtil.withContextClassLoader(
                    userClassLoader,
                    () -> hook.triggerCheckpoint(checkpointId, timestamp, wrappedExecutor));
        }

        @Override
        public void restoreCheckpoint(long checkpointId, @Nullable T checkpointData)
                throws Exception {
            LambdaUtil.withContextClassLoader(
                    userClassLoader, () -> hook.restoreCheckpoint(checkpointId, checkpointData));
        }

        @Nullable
        @Override
        public SimpleVersionedSerializer<T> createCheckpointDataSerializer() {
            return LambdaUtil.withContextClassLoader(
                    userClassLoader, hook::createCheckpointDataSerializer);
        }

        private static class WrappedCommand implements Runnable {

            private final ClassLoader userClassLoader;
            private final Runnable command;

            WrappedCommand(ClassLoader userClassLoader, Runnable command) {
                this.userClassLoader = Preconditions.checkNotNull(userClassLoader);
                this.command = Preconditions.checkNotNull(command);
            }

            @Override
            public void run() {
                LambdaUtil.withContextClassLoader(userClassLoader, command::run);
            }
        }
    }

    // ------------------------------------------------------------------------

    /** This class is not meant to be instantiated. */
    private MasterHooks() {}
}
