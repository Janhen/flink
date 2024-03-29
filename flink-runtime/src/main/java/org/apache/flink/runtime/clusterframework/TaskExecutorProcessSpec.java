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

package org.apache.flink.runtime.clusterframework;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.resources.CPUResource;
import org.apache.flink.api.common.resources.ExternalResource;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.runtime.externalresource.ExternalResourceUtils;
import org.apache.flink.runtime.util.config.memory.CommonProcessMemorySpec;
import org.apache.flink.runtime.util.config.memory.JvmMetaspaceAndOverhead;
import org.apache.flink.runtime.util.config.memory.taskmanager.TaskExecutorFlinkMemory;
import org.apache.flink.util.Preconditions;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 描述TaskExecutor进程的不同资源维度的细节。
 *
 *   <li>框架堆内存
 *   <li>框架非堆内存
 *   <li>Task堆内存
 *   <li>Task 非堆内存
 *   <li>网络内存
 *   <li>管理的内存
 *   <li>JVM 元空间
 *   <li>JVM 负载
 *
 * Describe the specifics of different resource dimensions of the TaskExecutor process.
 *
 * <p>A TaskExecutor's memory consists of the following components.
 *
 * <ul>
 *   <li>Framework Heap Memory
 *   <li>Framework Off-Heap Memory
 *   <li>Task Heap Memory
 *   <li>Task Off-Heap Memory
 *   <li>Network Memory
 *   <li>Managed Memory
 *   <li>JVM Metaspace
 *   <li>JVM Overhead
 * </ul>
 *
 * Among all the components, Framework Heap Memory and Task Heap Memory use on heap memory, while
 * the rest use off heap memory. We use Total Process Memory to refer to all the memory components,
 * while Total Flink Memory refering to all the components except JVM Metaspace and JVM Overhead.
 *
 * <p>The relationships of TaskExecutor memory components are shown below.
 *
 * <pre>
 *               ┌ ─ ─ Total Process Memory  ─ ─ ┐
 *                ┌ ─ ─ Total Flink Memory  ─ ─ ┐
 *               │ ┌───────────────────────────┐ │
 *                ││   Framework Heap Memory   ││  ─┐
 *               │ └───────────────────────────┘ │  │
 *               │ ┌───────────────────────────┐ │  │
 *            ┌─  ││ Framework Off-Heap Memory ││   ├─ On-Heap
 *            │  │ └───────────────────────────┘ │  │
 *            │   │┌───────────────────────────┐│   │
 *            │  │ │     Task Heap Memory      │ │ ─┘
 *            │   │└───────────────────────────┘│
 *            │  │ ┌───────────────────────────┐ │
 *            ├─  ││   Task Off-Heap Memory    ││
 *            │  │ └───────────────────────────┘ │
 *            │   │┌───────────────────────────┐│
 *            ├─ │ │      Network Memory       │ │
 *            │   │└───────────────────────────┘│
 *            │  │ ┌───────────────────────────┐ │
 *  Off-Heap ─┼─   │      Managed Memory       │
 *            │  ││└───────────────────────────┘││
 *            │   └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *            │  │┌─────────────────────────────┐│
 *            ├─  │        JVM Metaspace        │
 *            │  │└─────────────────────────────┘│
 *            │   ┌─────────────────────────────┐
 *            └─ ││        JVM Overhead         ││
 *                └─────────────────────────────┘
 *               └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 * </pre>
 */
public class TaskExecutorProcessSpec extends CommonProcessMemorySpec<TaskExecutorFlinkMemory> {
    private static final long serialVersionUID = 1L;

    private final CPUResource cpuCores;

    private final int numSlots;

    private final Map<String, ExternalResource> extendedResources;

    @VisibleForTesting
    public TaskExecutorProcessSpec(
            CPUResource cpuCores,
            MemorySize frameworkHeapSize,
            MemorySize frameworkOffHeapSize,
            MemorySize taskHeapSize,
            MemorySize taskOffHeapSize,
            MemorySize networkMemSize,
            MemorySize managedMemorySize,
            MemorySize jvmMetaspaceSize,
            MemorySize jvmOverheadSize,
            Collection<ExternalResource> extendedResources) {

        this(
                cpuCores,
                new TaskExecutorFlinkMemory(
                        frameworkHeapSize,
                        frameworkOffHeapSize,
                        taskHeapSize,
                        taskOffHeapSize,
                        networkMemSize,
                        managedMemorySize),
                new JvmMetaspaceAndOverhead(jvmMetaspaceSize, jvmOverheadSize),
                1,
                extendedResources);
    }

    protected TaskExecutorProcessSpec(
            CPUResource cpuCores,
            TaskExecutorFlinkMemory flinkMemory,
            JvmMetaspaceAndOverhead jvmMetaspaceAndOverhead,
            int numSlots,
            Collection<ExternalResource> extendedResources) {

        super(flinkMemory, jvmMetaspaceAndOverhead);
        this.cpuCores = cpuCores;
        this.numSlots = numSlots;
        this.extendedResources =
                Preconditions.checkNotNull(extendedResources).stream()
                        .filter(resource -> !resource.isZero())
                        .collect(Collectors.toMap(ExternalResource::getName, Function.identity()));
        Preconditions.checkArgument(
                this.extendedResources.size() == extendedResources.size(),
                "Duplicate resource name encountered in external resources.");
    }

    public CPUResource getCpuCores() {
        return cpuCores;
    }

    MemorySize getFrameworkHeapSize() {
        return getFlinkMemory().getFrameworkHeap();
    }

    MemorySize getFrameworkOffHeapMemorySize() {
        return getFlinkMemory().getFrameworkOffHeap();
    }

    public MemorySize getTaskHeapSize() {
        return getFlinkMemory().getTaskHeap();
    }

    public MemorySize getTaskOffHeapSize() {
        return getFlinkMemory().getTaskOffHeap();
    }

    public MemorySize getNetworkMemSize() {
        return getFlinkMemory().getNetwork();
    }

    public MemorySize getManagedMemorySize() {
        return getFlinkMemory().getManaged();
    }

    public int getNumSlots() {
        return numSlots;
    }

    public Map<String, ExternalResource> getExtendedResources() {
        return Collections.unmodifiableMap(extendedResources);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof TaskExecutorProcessSpec) {
            TaskExecutorProcessSpec that = (TaskExecutorProcessSpec) obj;
            return Objects.equals(this.cpuCores, that.cpuCores)
                    && Objects.equals(
                            this.getJvmMetaspaceAndOverhead(), that.getJvmMetaspaceAndOverhead())
                    && Objects.equals(this.getFlinkMemory(), that.getFlinkMemory())
                    && Objects.equals(this.numSlots, that.numSlots)
                    && Objects.equals(this.getExtendedResources(), that.getExtendedResources());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getJvmMetaspaceAndOverhead(),
                getFlinkMemory(),
                cpuCores,
                numSlots,
                extendedResources);
    }

    @Override
    public String toString() {
        return "TaskExecutorProcessSpec {"
                + "cpuCores="
                + cpuCores.getValue().doubleValue()
                + ", frameworkHeapSize="
                + getFrameworkHeapSize().toHumanReadableString()
                + ", frameworkOffHeapSize="
                + getFrameworkOffHeapMemorySize().toHumanReadableString()
                + ", taskHeapSize="
                + getTaskHeapSize().toHumanReadableString()
                + ", taskOffHeapSize="
                + getTaskOffHeapSize().toHumanReadableString()
                + ", networkMemSize="
                + getNetworkMemSize().toHumanReadableString()
                + ", managedMemorySize="
                + getManagedMemorySize().toHumanReadableString()
                + ", jvmMetaspaceSize="
                + getJvmMetaspaceSize().toHumanReadableString()
                + ", jvmOverheadSize="
                + getJvmOverheadSize().toHumanReadableString()
                + ", numSlots="
                + numSlots
                + (extendedResources.isEmpty()
                        ? ""
                        : (", "
                                + ExternalResourceUtils.generateExternalResourcesString(
                                        extendedResources.values())))
                + '}';
    }
}
