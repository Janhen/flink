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

package org.apache.flink.api.common.io;

import org.apache.flink.annotation.Public;
import org.apache.flink.core.io.IOReadableWritable;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;

import java.io.IOException;

/**
 * 一个 24 字节的块，写入二进制文件中块的 <i>end<i>，包含
 * i) 块中的记录数，
 * ii) 累积记录数，以及
 * iii) 块中的第一条记录。
 *
 * A block of 24 bytes written at the <i>end</i> of a block in a binary file, and containing i) the
 * number of records in the block, ii) the accumulated number of records, and iii) the offset of the
 * first record in the block.
 */
@Public
public class BlockInfo implements IOReadableWritable {

    private long recordCount;

    private long accumulatedRecordCount;

    private long firstRecordStart;

    public int getInfoSize() {
        return 8 + 8 + 8;
    }

    /**
     * Returns the firstRecordStart.
     *
     * @return the firstRecordStart
     */
    public long getFirstRecordStart() {
        return this.firstRecordStart;
    }

    /**
     * Sets the firstRecordStart to the specified value.
     *
     * @param firstRecordStart the firstRecordStart to set
     */
    public void setFirstRecordStart(long firstRecordStart) {
        this.firstRecordStart = firstRecordStart;
    }

    @Override
    public void write(DataOutputView out) throws IOException {
        out.writeLong(this.recordCount);
        out.writeLong(this.accumulatedRecordCount);
        out.writeLong(this.firstRecordStart);
    }

    @Override
    public void read(DataInputView in) throws IOException {
        this.recordCount = in.readLong();
        this.accumulatedRecordCount = in.readLong();
        this.firstRecordStart = in.readLong();
    }

    /**
     * Returns the recordCount.
     *
     * @return the recordCount
     */
    public long getRecordCount() {
        return this.recordCount;
    }

    /**
     * Returns the accumulated record count.
     *
     * @return the accumulated record count
     */
    public long getAccumulatedRecordCount() {
        return this.accumulatedRecordCount;
    }

    /**
     * Sets the accumulatedRecordCount to the specified value.
     *
     * @param accumulatedRecordCount the accumulatedRecordCount to set
     */
    public void setAccumulatedRecordCount(long accumulatedRecordCount) {
        this.accumulatedRecordCount = accumulatedRecordCount;
    }

    /**
     * Sets the recordCount to the specified value.
     *
     * @param recordCount the recordCount to set
     */
    public void setRecordCount(long recordCount) {
        this.recordCount = recordCount;
    }
}
