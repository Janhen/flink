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
import org.apache.flink.table.catalog.exceptions.CatalogException;

/**
 * 此接口用于 {@link Catalog} 侦听临时对象操作。当目录实现此接口时，它会在对属于该目录的临时对象执行某些操作时得到通知。
 *
 * This interface is for a {@link Catalog} to listen on temporary object operations. When a catalog
 * implements this interface, it'll get informed when certain operations are performed on temporary
 * objects belonging to that catalog.
 */
@PublicEvolving
public interface TemporaryOperationListener {

    /**
     * 当要在此目录中创建临时表或视图时调用此方法。目录可以根据需要修改表或视图，并返回修改后的 CatalogBaseTable
     * 实例，该实例将为用户会话存储。
     *
     * This method is called when a temporary table or view is to be created in this catalog. The
     * catalog can modify the table or view according to its needs and return the modified
     * CatalogBaseTable instance, which will be stored for the user session.
     *
     * @param tablePath path of the table or view to be created
     * @param table the table definition
     * @return the modified table definition to be stored
     * @throws CatalogException in case of any runtime exception
     */
    CatalogBaseTable onCreateTemporaryTable(ObjectPath tablePath, CatalogBaseTable table)
            throws CatalogException;

    /**
     * This method is called when a temporary table or view in this catalog is to be dropped.
     *
     * @param tablePath path of the table or view to be dropped
     * @throws CatalogException in case of any runtime exception
     */
    void onDropTemporaryTable(ObjectPath tablePath) throws CatalogException;

    /**
     * 当要在此目录中创建临时函数时调用此方法。目录可以根据需要修改函数并返回修改后的 CatalogFunction 实例，该实例
     * 将为用户会话存储。
     *
     * This method is called when a temporary function is to be created in this catalog. The catalog
     * can modify the function according to its needs and return the modified CatalogFunction
     * instance, which will be stored for the user session.
     *
     * @param functionPath path of the function to be created
     * @param function the function definition
     * @return the modified function definition to be stored
     * @throws CatalogException in case of any runtime exception
     */
    CatalogFunction onCreateTemporaryFunction(ObjectPath functionPath, CatalogFunction function)
            throws CatalogException;

    /**
     * 当要删除此目录中的临时函数时调用此方法。
     *
     * This method is called when a temporary function in this catalog is to be dropped.
     *
     * @param functionPath path of the function to be dropped
     * @throws CatalogException in case of any runtime exception
     */
    void onDropTemporaryFunction(ObjectPath functionPath) throws CatalogException;
}
