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

package org.apache.flink.table.runtime.generated;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.operators.sort.BinaryInMemorySortBuffer;

import java.io.Serializable;
import java.util.Comparator;

/**
 * {@link BinaryInMemorySortBuffer} 的记录比较器。为了性能，子类通常通过 CodeGenerator 实现。一个帮助 JVM
 * 内联的新接口。
 *
 * Record comparator for {@link BinaryInMemorySortBuffer}. For performance, subclasses are usually
 * implemented through CodeGenerator. A new interface for helping JVM inline.
 */
public interface RecordComparator extends Comparator<RowData>, Serializable {

    @Override
    int compare(RowData o1, RowData o2);
}
