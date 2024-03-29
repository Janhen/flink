/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.	See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.	You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.runtime.operators.join;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.binary.BinaryRowData;
import org.apache.flink.table.runtime.generated.Projection;
import org.apache.flink.table.runtime.generated.RecordComparator;
import org.apache.flink.table.runtime.typeutils.BinaryRowDataSerializer;
import org.apache.flink.table.runtime.util.ResettableExternalBuffer;
import org.apache.flink.util.MutableObjectIterator;

import java.io.IOException;

/** Gets probeRow and match rows for inner join. */
// 获取内部连接的 probeRow 和匹配行。
public class SortMergeInnerJoinIterator extends SortMergeJoinIterator {

    public SortMergeInnerJoinIterator(
            BinaryRowDataSerializer probeSerializer,
            BinaryRowDataSerializer bufferedSerializer,
            Projection probeProjection,
            Projection bufferedProjection,
            RecordComparator keyComparator,
            MutableObjectIterator<RowData> probeIterator,
            MutableObjectIterator<BinaryRowData> bufferedIterator,
            ResettableExternalBuffer buffer,
            boolean[] filterNullKeys)
            throws IOException {
        super(
                probeSerializer,
                bufferedSerializer,
                probeProjection,
                bufferedProjection,
                keyComparator,
                probeIterator,
                bufferedIterator,
                buffer,
                filterNullKeys);
    }

    public boolean nextInnerJoin() throws IOException {
        if (!advanceNextSuitableProbeRow()) {
            return false; // no probe row, over.
        }

        if (matchKey != null && keyComparator.compare(probeKey, matchKey) == 0) {
            return true; // probe has a same key, so same matches.
        }

        if (bufferedRow == null) {
            return false; // no buffered row, bye bye.
        } else {
            // find next equaled key.
            while (true) {
                int cmp = keyComparator.compare(probeKey, bufferedKey);
                if (cmp > 0) {
                    if (!advanceNextSuitableBufferedRow()) {
                        return false;
                    }
                } else if (cmp < 0) {
                    if (!advanceNextSuitableProbeRow()) {
                        return false;
                    }
                } else {
                    bufferMatchingRows();
                    return true;
                }
            }
        }
    }
}
