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

package org.apache.flink.api.connector.sink2;

import org.apache.flink.annotation.PublicEvolving;

import java.io.IOException;
import java.util.Collection;

/**
 * {@code Committer}负责提交由{@link TwoPhaseCommittingSink.PrecommittingSinkWriter}。在两阶段提交协议的
 * 第二步预提交sinkwriter}。
 *
 * <p>提交必须是幂等的:如果Flink在提交阶段发生了一些失败，Flink将从之前的检查点重新开始，并重新尝试提交所有的提交。
 * 因此，一些或所有可提交事项可能已经提交。这些{@link CommitRequest}不能改变外部系统，实现者被要求发送
 * {@link CommitRequest#signalAlreadyCommitted()}信号。
 *
 * The {@code Committer} is responsible for committing the data staged by the {@link
 * TwoPhaseCommittingSink.PrecommittingSinkWriter} in the second step of a two-phase commit
 * protocol.
 *
 * <p>A commit must be idempotent: If some failure occurs in Flink during commit phase, Flink will
 * restart from previous checkpoint and re-attempt to commit all committables. Thus, some or all
 * committables may have already been committed. These {@link CommitRequest}s must not change the
 * external system and implementers are asked to signal {@link
 * CommitRequest#signalAlreadyCommitted()}.
 *
 * @param <CommT> The type of information needed to commit the staged data
 */
@PublicEvolving
public interface Committer<CommT> extends AutoCloseable {
    /**
     * 提交给定的{@link CommT}列表。
     *
     * Commit the given list of {@link CommT}.
     *
     * @param committables A list of commit requests staged by the sink writer.
     * @throws IOException for reasons that may yield a complete restart of the job.
     */
    void commit(Collection<CommitRequest<CommT>> committables)
            throws IOException, InterruptedException;

    /**
     * A request to commit a specific committable.
     *
     * @param <CommT>
     */
    @PublicEvolving
    interface CommitRequest<CommT> {

        /** Returns the committable. */
        CommT getCommittable();

        /**
         * Returns how many times this particular committable has been retried. Starts at 0 for the
         * first attempt.
         */
        int getNumberOfRetries();

        /**
         * The commit failed for known reason and should not be retried.
         *
         * <p>Currently calling this method only logs the error, discards the comittable and
         * continues. In the future the behaviour might be configurable.
         */
        void signalFailedWithKnownReason(Throwable t);

        /**
         * The commit failed for unknown reason and should not be retried.
         *
         * <p>Currently calling this method fails the job. In the future the behaviour might be
         * configurable.
         */
        void signalFailedWithUnknownReason(Throwable t);

        /**
         * The commit failed for a retriable reason. If the sink supports a retry maximum, this may
         * permanently fail after reaching that maximum. Else the committable will be retried as
         * long as this method is invoked after each attempt.
         */
        void retryLater();

        /**
         * Updates the underlying committable and retries later (see {@link #retryLater()} for a
         * description). This method can be used if a committable partially succeeded.
         */
        void updateAndRetryLater(CommT committable);

        /**
         * Signals that a committable is skipped as it was committed already in a previous run.
         * Using this method is optional but eases bookkeeping and debugging. It also serves as a
         * code documentation for the branches dealing with recovery.
         */
        void signalAlreadyCommitted();
    }
}
