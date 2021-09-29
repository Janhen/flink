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

package org.apache.flink.table.catalog;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.api.Schema;

import javax.annotation.Nullable;

import java.util.Map;

/**
 * 表示{@link Catalog}中视图的未解析元数据。
 *
 * <p>包含SQL {@code CREATE VIEW}语句中可以表达的所有特征。在使用之前，框架会将这个接口的实例解析到
 *   {@link ResolvedCatalogView}。
 *
 * <p>目录实现者可以使用{@link #of(Schema, String, String, String, Map)}作为这个接口的基本实现，也可以创建
 *   一个自定义类，允许传递特定于目录的对象(如果需要)。
 *
 * Represents the unresolved metadata of a view in a {@link Catalog}.
 *
 * <p>It contains all characteristics that can be expressed in a SQL {@code CREATE VIEW} statement.
 * The framework will resolve instances of this interface to a {@link ResolvedCatalogView} before
 * usage.
 *
 * <p>A catalog implementer can either use {@link #of(Schema, String, String, String, Map)} for a
 * basic implementation of this interface or create a custom class that allows passing
 * catalog-specific objects (if necessary).
 */
@PublicEvolving
public interface CatalogView extends CatalogBaseTable {

    /**
     * 创建此接口的基本实现。
     *
     * <p>签名类似于SQL {@code CREATE VIEW}语句。
     *
     * Creates a basic implementation of this interface.
     *
     * <p>The signature is similar to a SQL {@code CREATE VIEW} statement.
     *
     * @param schema unresolved schema
     * @param comment optional comment
     * @param originalQuery original text of the view definition
     * @param expandedQuery expanded text of the original view definition with materialized
     *     identifiers
     * @param options options to configure the connector
     */
    static CatalogView of(
            Schema schema,
            @Nullable String comment,
            String originalQuery,
            String expandedQuery,
            Map<String, String> options) {
        return new DefaultCatalogView(schema, comment, originalQuery, expandedQuery, options);
    }

    @Override
    default TableKind getTableKind() {
        return TableKind.VIEW;
    }

    /**
     * 视图定义的原始文本，它也保留了原始格式。
     *
     * Original text of the view definition that also preserves the original formatting.
     *
     * @return the original string literal provided by the user.
     */
    String getOriginalQuery();

    /**
     * 这是必需的，因为在定义视图的会话结束后，context(例如当前DB)会丢失。作为一个例子，扩展查询文本将处理这一点。
     *
     * Expanded text of the original view definition This is needed because the context such as
     * current DB is lost after the session, in which view is defined, is gone. Expanded query text
     * takes care of this, as an example.
     *
     * <p>For example, for a view that is defined in the context of "default" database with a query
     * {@code select * from test1}, the expanded query text might become {@code select
     * `test1`.`name`, `test1`.`value` from `default`.`test1`}, where table test1 resides in
     * database "default" and has two columns ("name" and "value").
     *
     * @return the view definition in expanded text.
     */
    String getExpandedQuery();
}
