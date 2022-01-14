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

package org.apache.flink.streaming.api.functions.async;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.functions.Function;

import java.io.Serializable;
import java.util.concurrent.TimeoutException;

/**
 * 用于实现异步 I/O 操作的函数
 *
 * <p>对于每个#asyncInvoke，一个async io操作可以被触发，并且一旦它被触发，结果可以通过调用
 *   {@link ResultFuture#complete}来收集。对于每个异步操作，它的上下文在调用#asyncInvoke之后立即存储在操作符中，
 *   避免在内部缓冲区未满时阻塞每个流输入。
 *
 * <p>{@link ResultFuture}可以传递到回调函数或future函数中来收集结果数据。错误也可以通过
 *   {@link ResultFuture# completeexception (Throwable)}传播到async IO操作符。
 *
 * A function to trigger Async I/O operation.
 *
 * <p>For each #asyncInvoke, an async io operation can be triggered, and once it has been done, the
 * result can be collected by calling {@link ResultFuture#complete}. For each async operation, its
 * context is stored in the operator immediately after invoking #asyncInvoke, avoiding blocking for
 * each stream input as long as the internal buffer is not full.
 *
 * <p>{@link ResultFuture} can be passed into callbacks or futures to collect the result data. An
 * error can also be propagate to the async IO operator by {@link
 * ResultFuture#completeExceptionally(Throwable)}.
 *
 * <p>Callback example usage:
 *
 * <pre>{@code
 * public class HBaseAsyncFunc implements AsyncFunction<String, String> {
 *
 *   public void asyncInvoke(String row, ResultFuture<String> result) throws Exception {
 *     HBaseCallback cb = new HBaseCallback(result);
 *     Get get = new Get(Bytes.toBytes(row));
 *     hbase.asyncGet(get, cb);
 *   }
 * }
 * }</pre>
 *
 * <p>Future example usage:
 *
 * <pre>{@code
 * public class HBaseAsyncFunc implements AsyncFunction<String, String> {
 *
 *   public void asyncInvoke(String row, final ResultFuture<String> result) throws Exception {
 *     Get get = new Get(Bytes.toBytes(row));
 *     ListenableFuture<Result> future = hbase.asyncGet(get);
 *     Futures.addCallback(future, new FutureCallback<Result>() {
 *       public void onSuccess(Result result) {
 *         List<String> ret = process(result);
 *         result.complete(ret);
 *       }
 *       public void onFailure(Throwable thrown) {
 *         result.completeExceptionally(thrown);
 *       }
 *     });
 *   }
 * }
 * }</pre>
 *
 * @param <IN> The type of the input elements.
 * @param <OUT> The type of the returned elements.
 */
@PublicEvolving
public interface AsyncFunction<IN, OUT> extends Function, Serializable {

    /**
     * 为每个流输入触发异步操作。
     *
     * Trigger async operation for each stream input.
     *
     * @param input element coming from an upstream task
     * @param resultFuture to be completed with the result data
     * @exception Exception in case of a user code error. An exception will make the task fail and
     *     trigger fail-over process.
     */
    void asyncInvoke(IN input, ResultFuture<OUT> resultFuture) throws Exception;

    /**
     * {@link AsyncFunction#asyncInvoke}发生超时。默认情况下，结果future将异常完成，并带有一个超时异常。
     *
     * {@link AsyncFunction#asyncInvoke} timeout occurred. By default, the result future is
     * exceptionally completed with a timeout exception.
     *
     * @param input element coming from an upstream task
     * @param resultFuture to be completed with the result data
     */
    default void timeout(IN input, ResultFuture<OUT> resultFuture) throws Exception {
        // J: 完成并可抛出异常调用，指定抛出受检的超时异常
        resultFuture.completeExceptionally(
                new TimeoutException("Async function call has timed out."));
    }
}
