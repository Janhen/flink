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

package org.apache.flink.table.data;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.types.logical.MapType;
import org.apache.flink.table.types.logical.MultisetType;

/**
 * 表示{@link MapType}或{@link MultisetType}数据的内部数据结构的基接口。
 *
 * <p>注:该数据结构的所有键和值必须是内部数据结构。所有键必须是相同的类型;相同的价值观。有关内部数据结构的更多信息，
 *    请参见{@link RowData}。
 *
 * <p>使用{@link GenericMapData}从普通的Java映射构造这个接口的实例。
 *
 * Base interface of an internal data structure representing data of {@link MapType} or {@link
 * MultisetType}.
 *
 * <p>Note: All keys and values of this data structure must be internal data structures. All keys
 * must be of the same type; same for values. See {@link RowData} for more information about
 * internal data structures.
 *
 * <p>Use {@link GenericMapData} to construct instances of this interface from regular Java maps.
 */
@PublicEvolving
public interface MapData {

    /** Returns the number of key-value mappings in this map. */
    int size();

    /**
     * Returns an array view of the keys contained in this map.
     *
     * <p>A key-value pair has the same index in the key array and value array.
     */
    ArrayData keyArray();

    /**
     * Returns an array view of the values contained in this map.
     *
     * <p>A key-value pair has the same index in the key array and value array.
     */
    ArrayData valueArray();
}
