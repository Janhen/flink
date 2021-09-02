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

package org.apache.flink.table.api;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.expressions.ApiExpressionUtils;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.expressions.ExpressionParser;

/** Tumbling window on time. */
// 按时翻滚窗口。
@PublicEvolving
public final class TumbleWithSizeOnTime {

    private final Expression time;
    private final Expression size;

    TumbleWithSizeOnTime(Expression time, Expression size) {
        this.time = ApiExpressionUtils.unwrapFromApi(time);
        this.size = ApiExpressionUtils.unwrapFromApi(size);
    }

    /**
     * 为此窗口分配一个别名，以下 {@code groupBy()} 和 {@code select()} 子句可以引用该别名。 {@code select()}
     * 语句可以访问窗口属性，例如窗口开始或结束时间。
     *
     * Assigns an alias for this window that the following {@code groupBy()} and {@code select()}
     * clause can refer to. {@code select()} statement can access window properties such as window
     * start or end time.
     *
     * @param alias alias for this window
     * @return this window
     */
    public TumbleWithSizeOnTimeWithAlias as(Expression alias) {
        return new TumbleWithSizeOnTimeWithAlias(alias, time, size);
    }

    /**
     * 为此窗口分配一个别名，以下 {@code groupBy()} 和 {@code select()} 子句可以引用该别名。 {@code select()}
     * 语句可以访问窗口属性，例如窗口开始或结束时间。
     *
     * Assigns an alias for this window that the following {@code groupBy()} and {@code select()}
     * clause can refer to. {@code select()} statement can access window properties such as window
     * start or end time.
     *
     * @param alias alias for this window
     * @return this window
     */
    public TumbleWithSizeOnTimeWithAlias as(String alias) {
        return as(ExpressionParser.parseExpression(alias));
    }
}
