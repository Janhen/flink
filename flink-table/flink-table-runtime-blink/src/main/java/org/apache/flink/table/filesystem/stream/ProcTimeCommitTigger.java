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

package org.apache.flink.table.filesystem.stream;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.OperatorStateStore;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.api.common.typeutils.base.MapSerializer;
import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.flink.table.filesystem.FileSystemOptions.SINK_PARTITION_COMMIT_DELAY;

/**
 * Partition commit trigger by creation time and processing time service, if 'current processing
 * time' > 'partition creation time' + 'delay', will commit the partition.
 */
public class ProcTimeCommitTigger implements PartitionCommitTrigger {

    private static final ListStateDescriptor<Map<String, Long>> PENDING_PARTITIONS_STATE_DESC =
            new ListStateDescriptor<>(
                    "pending-partitions-with-time",
                    new MapSerializer<>(StringSerializer.INSTANCE, LongSerializer.INSTANCE));

    private final ListState<Map<String, Long>> pendingPartitionsState;
    private final Map<String, Long> pendingPartitions;
    private final long commitDelay;
    private final ProcessingTimeService procTimeService;

    public ProcTimeCommitTigger(
            boolean isRestored,
            OperatorStateStore stateStore,
            Configuration conf,
            ProcessingTimeService procTimeService)
            throws Exception {
        this.pendingPartitionsState = stateStore.getListState(PENDING_PARTITIONS_STATE_DESC);
        this.pendingPartitions = new HashMap<>();
        if (isRestored) {
            pendingPartitions.putAll(pendingPartitionsState.get().iterator().next());
        }

        this.procTimeService = procTimeService;
        this.commitDelay = conf.get(SINK_PARTITION_COMMIT_DELAY).toMillis();
    }

    @Override
    public void addPartition(String partition) {
        if (!StringUtils.isNullOrWhitespaceOnly(partition)) {
            this.pendingPartitions.putIfAbsent(
                    partition, procTimeService.getCurrentProcessingTime());
        }
    }

    @Override
    public List<String> committablePartitions(long checkpointId) {
        List<String> needCommit = new ArrayList<>();
        long currentProcTime = procTimeService.getCurrentProcessingTime();
        Iterator<Map.Entry<String, Long>> iter = pendingPartitions.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Long> entry = iter.next();
            long creationTime = entry.getValue();
            if (commitDelay == 0 || currentProcTime > creationTime + commitDelay) {
                needCommit.add(entry.getKey());
                iter.remove();
            }
        }
        return needCommit;
    }

    @Override
    public void snapshotState(long checkpointId, long watermark) throws Exception {
        pendingPartitionsState.clear();
        pendingPartitionsState.add(new HashMap<>(pendingPartitions));
    }

    @Override
    public List<String> endInput() {
        ArrayList<String> partitions = new ArrayList<>(pendingPartitions.keySet());
        pendingPartitions.clear();
        return partitions;
    }
}
