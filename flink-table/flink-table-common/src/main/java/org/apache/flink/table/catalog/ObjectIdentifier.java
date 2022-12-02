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

import org.apache.flink.util.Preconditions;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.apache.flink.table.utils.EncodingUtils.escapeIdentifier;

/**
 * 标识目录中的对象。它允许标识目录中的表、视图、函数或类型等对象。标识符必须是完全限定的。目录管理器负责将标识符
 * 解析为对象。
 *
 * <p>当 {@link ObjectPath} 在同一个目录中使用时，该类的实例可以跨目录使用。
 *
 * <p>如果两个对象在稳定的会话上下文中共享相同的对象标识符，则认为它们相等。
 *
 * Identifies an object in a catalog. It allows to identify objects such as tables, views, function,
 * or types in a catalog. An identifier must be fully qualified. It is the responsibility of the
 * catalog manager to resolve an identifier to an object.
 *
 * <p>While {@link ObjectPath} is used within the same catalog, instances of this class can be used
 * across catalogs.
 *
 * <p>Two objects are considered equal if they share the same object identifier in a stable session
 * context.
 */
public final class ObjectIdentifier implements Serializable {

    private final String catalogName;

    private final String databaseName;

    private final String objectName;

    public static ObjectIdentifier of(String catalogName, String databaseName, String objectName) {
        return new ObjectIdentifier(catalogName, databaseName, objectName);
    }

    private ObjectIdentifier(String catalogName, String databaseName, String objectName) {
        this.catalogName =
                Preconditions.checkNotNull(catalogName, "Catalog name must not be null.");
        this.databaseName =
                Preconditions.checkNotNull(databaseName, "Database name must not be null.");
        this.objectName = Preconditions.checkNotNull(objectName, "Object name must not be null.");
    }

    public String getCatalogName() {
        return catalogName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getObjectName() {
        return objectName;
    }

    public ObjectPath toObjectPath() {
        return new ObjectPath(databaseName, objectName);
    }

    /** List of the component names of this object identifier. */
    public List<String> toList() {
        return Arrays.asList(getCatalogName(), getDatabaseName(), getObjectName());
    }

    /**
     * 返回完全序列化此实例的字符串。序列化的字符串可用于传输或持久化对象标识符。
     *
     * Returns a string that fully serializes this instance. The serialized string can be used for
     * transmitting or persisting an object identifier.
     */
    public String asSerializableString() {
        return String.format(
                "%s.%s.%s",
                escapeIdentifier(catalogName),
                escapeIdentifier(databaseName),
                escapeIdentifier(objectName));
    }

    /** Returns a string that summarizes this instance for printing to a console or log. */
    // 返回总结此实例的字符串，以便打印到控制台或日志。
    public String asSummaryString() {
        return String.join(".", catalogName, databaseName, objectName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObjectIdentifier that = (ObjectIdentifier) o;
        return catalogName.equals(that.catalogName)
                && databaseName.equals(that.databaseName)
                && objectName.equals(that.objectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(catalogName, databaseName, objectName);
    }

    @Override
    public String toString() {
        return asSerializableString();
    }
}
