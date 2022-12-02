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

package org.apache.flink.table.functions;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.annotation.DataTypeHint;
import org.apache.flink.table.annotation.FunctionHint;
import org.apache.flink.table.catalog.DataTypeFactory;
import org.apache.flink.table.connector.source.LookupTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.extraction.TypeInferenceExtractor;
import org.apache.flink.table.types.inference.TypeInference;
import org.apache.flink.types.Row;

import java.util.concurrent.CompletableFuture;

/**
 * 用户定义的异步表函数的基类。用户定义的异步表函数将 0、1 或多个标量值映射为 0、1 或多行(或结构化类型)。
 *
 * <p>这种函数类似于{@link TableFunction}，但是是异步执行的。
 *
 * <p> {@link AsyncTableFunction}的行为可以通过实现自定义的求值方法来定义。求值方法必须是公开的，而不是静态的，
 *   并且命名为<code> eval<code>。求值方法也可以通过实现多个名为<code>eval<code>的方法来重载。
 *
 * <p>默认情况下，使用反射自动提取输入和输出数据类型。这包括类的泛型参数{@code T}，用于确定输出数据类型。输入参数
 *   派生自一个或多个{@code eval()}方法。如果反射信息不够，可以使用{@link DataTypeHint}和{@link FunctionHint}
 *   注释来支持和丰富反射信息。更多关于如何注释实现类的例子，请参见{@link TableFunction}。
 *
 * <p>注意:目前，异步表函数仅支持{@link LookupTableSource}执行时态连接的运行时实现。默认情况下，
 *   {@link AsyncTableFunction}的输入和输出{@link DataType}与其他{@link UserDefinedFunction}的逻辑类似。
 *   但是，为了方便起见，在{@link LookupTableSource}中，输出类型可以简单地是{@link Row}或{@link RowData}，
 *   在这种情况下，输入和输出类型是由表的模式和默认转换派生出来的。
 *
 * <p>求值方法的第一个参数必须是{@link CompletableFuture}。其他参数指定用户定义的输入参数，比如
 *   {@link TableFunction}的"eval"方法。泛型{@link CompletableFuture}必须是{@link java.util.Collection}
 *   收集多个可能的结果值。
 *
 * <p>对于<code>eval()<code>的每次调用，一个异步IO操作可以被触发，并且一旦操作完成，结果可以通过调用
 *   {@link CompletableFuture#complete}来收集。对于每个异步操作，它的上下文在调用<code>eval()<code>后立即
 *   存储在 operator 中，避免在内部缓冲区未满的情况下阻塞每个流输入。
 *
 * <p>{@link CompletableFuture}可以被传递到回调函数或future函数中来收集结果数据。错误也可以通过调用
 *   {@link CompletableFuture#completeExceptionally(Throwable)}传播到async IO操作符。
 *
 * <p>为了在编目中存储用户定义函数，类必须有一个默认构造函数，并且在运行时必须是可实例化的。
 *
 * <p>向Apache HBase执行异步请求的示例如下:
 *
 * <pre>{@code
 *  public class HBaseAsyncTableFunction extends AsyncTableFunction<Row> {
 *    // implement an "eval" method that takes a CompletableFuture as the first parameter
 *    // and ends with as many parameters as you want
 *    public void eval(CompletableFuture<Collection<Row>> result, String rowkey) {
 *      Get get = new Get(Bytes.toBytes(rowkey));
 *      ListenableFuture<Result> future = hbase.asyncGet(get);
 *      Futures.addCallback(future, new FutureCallback<Result>() {
 *        public void onSuccess(Result result) {
 *          List<Row> ret = process(result);
 *          result.complete(ret);
 *        }
 *        public void onFailure(Throwable thrown) {
 *          result.completeExceptionally(thrown);
 *        }
 *      });
 *    }
 *    // you can overload the eval method here ...
 *  }
 *  }</pre>
 *
 * Base class for a user-defined asynchronous table function. A user-defined asynchronous table
 * function maps zero, one, or multiple scalar values to zero, one, or multiple rows (or structured
 * types).
 *
 * <p>This kind of function is similar to {@link TableFunction} but is executed asynchronously.
 *
 * <p>The behavior of a {@link AsyncTableFunction} can be defined by implementing a custom
 * evaluation method. An evaluation method must be declared publicly, not static, and named <code>
 * eval</code>. Evaluation methods can also be overloaded by implementing multiple methods named
 * <code>eval</code>.
 *
 * <p>By default, input and output data types are automatically extracted using reflection. This
 * includes the generic argument {@code T} of the class for determining an output data type. Input
 * arguments are derived from one or more {@code eval()} methods. If the reflective information is
 * not sufficient, it can be supported and enriched with {@link DataTypeHint} and {@link
 * FunctionHint} annotations. See {@link TableFunction} for more examples how to annotate an
 * implementation class.
 *
 * <p>Note: Currently, asynchronous table functions are only supported as the runtime implementation
 * of {@link LookupTableSource}s for performing temporal joins. By default, input and output {@link
 * DataType}s of {@link AsyncTableFunction} are derived similar to other {@link
 * UserDefinedFunction}s using the logic above. However, for convenience, in a {@link
 * LookupTableSource} the output type can simply be a {@link Row} or {@link RowData} in which case
 * the input and output types are derived from the table's schema with default conversion.
 *
 * <p>The first parameter of the evaluation method must be a {@link CompletableFuture}. Other
 * parameters specify user-defined input parameters like the "eval" method of {@link TableFunction}.
 * The generic type of {@link CompletableFuture} must be {@link java.util.Collection} to collect
 * multiple possible result values.
 *
 * <p>For each call to <code>eval()</code>, an async IO operation can be triggered, and once the
 * operation has been done, the result can be collected by calling {@link
 * CompletableFuture#complete}. For each async operation, its context is stored in the operator
 * immediately after invoking <code>eval()</code>, avoiding blocking for each stream input as long
 * as the internal buffer is not full.
 *
 * <p>{@link CompletableFuture} can be passed into callbacks or futures to collect the result data.
 * An error can also be propagated to the async IO operator by calling {@link
 * CompletableFuture#completeExceptionally(Throwable)}.
 *
 * <p>For storing a user-defined function in a catalog, the class must have a default constructor
 * and must be instantiable during runtime.
 *
 * <p>The following example shows how to perform an asynchronous request to Apache HBase:
 *
 * <pre>{@code
 * public class HBaseAsyncTableFunction extends AsyncTableFunction<Row> {
 *
 *   // implement an "eval" method that takes a CompletableFuture as the first parameter
 *   // and ends with as many parameters as you want
 *   public void eval(CompletableFuture<Collection<Row>> result, String rowkey) {
 *     Get get = new Get(Bytes.toBytes(rowkey));
 *     ListenableFuture<Result> future = hbase.asyncGet(get);
 *     Futures.addCallback(future, new FutureCallback<Result>() {
 *       public void onSuccess(Result result) {
 *         List<Row> ret = process(result);
 *         result.complete(ret);
 *       }
 *       public void onFailure(Throwable thrown) {
 *         result.completeExceptionally(thrown);
 *       }
 *     });
 *   }
 *
 *   // you can overload the eval method here ...
 * }
 * }</pre>
 *
 * @param <T> The type of the output row used during reflective extraction.
 */
@PublicEvolving
public abstract class AsyncTableFunction<T> extends UserDefinedFunction {

    @Override
    public final FunctionKind getKind() {
        return FunctionKind.ASYNC_TABLE;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public TypeInference getTypeInference(DataTypeFactory typeFactory) {
        // J: 抽取异步方法的类型引用
        return TypeInferenceExtractor.forAsyncTableFunction(typeFactory, (Class) getClass());
    }
}
