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

package org.apache.flink.configuration;

import org.apache.flink.annotation.docs.Documentation;

/** A collection of all configuration options that relate to checkpoints and savepoints. */
// 与检查点和保存点相关的所有配置选项的集合
public class CheckpointingOptions {

    // ------------------------------------------------------------------------
    //  general checkpoint and state backend options
    // ------------------------------------------------------------------------

    /** The state backend to be used to store and checkpoint state. */
    // 用于存储和检查点状态的状态后端
    @Documentation.Section(value = Documentation.Sections.COMMON_STATE_BACKENDS, position = 1)
    public static final ConfigOption<String> STATE_BACKEND =
            ConfigOptions.key("state.backend")
                    .noDefaultValue()
                    .withDescription("The state backend to be used to store and checkpoint state.");

    /** The maximum number of completed checkpoints to retain. */
    // 要保留的检查点的最大数量, 默认为 1
    @Documentation.Section(Documentation.Sections.COMMON_STATE_BACKENDS)
    public static final ConfigOption<Integer> MAX_RETAINED_CHECKPOINTS =
            ConfigOptions.key("state.checkpoints.num-retained")
                    .defaultValue(1)
                    .withDescription("The maximum number of completed checkpoints to retain.");

    /**
     * 选项状态后端是否应该在可能和可配置的情况下使用异步快照方法。
     *
     * <p>有些状态后端可能不支持异步快照，或者只支持异步快照，请忽略此选项。
     *
     * Option whether the state backend should use an asynchronous snapshot method where possible
     * and configurable.
     *
     * <p>Some state backends may not support asynchronous snapshots, or only support asynchronous
     * snapshots, and ignore this option.
     */
    @Documentation.Section(Documentation.Sections.EXPERT_STATE_BACKENDS)
    public static final ConfigOption<Boolean> ASYNC_SNAPSHOTS =
            ConfigOptions.key("state.backend.async")
                    .defaultValue(true)
                    .withDescription(
                            "Option whether the state backend should use an asynchronous snapshot method where"
                                    + " possible and configurable. Some state backends may not support asynchronous snapshots, or only support"
                                    + " asynchronous snapshots, and ignore this option.");

    /**
     * 选择状态后端是否应该创建增量检查点(如果可能的话)。对于增量检查点，只存储与前一个检查点的差异，而不是完整的检查点状态。
     *
     * <p>一旦启用，web UI中显示的状态大小或从rest API中获取的状态大小只代表增量检查点大小，而不是完整检查点大小。
     *
     * <p>一些状态后端可能不支持增量检查点，因此忽略此选项。
     *
     * Option whether the state backend should create incremental checkpoints, if possible. For an
     * incremental checkpoint, only a diff from the previous checkpoint is stored, rather than the
     * complete checkpoint state.
     *
     * <p>Once enabled, the state size shown in web UI or fetched from rest API only represents the
     * delta checkpoint size instead of full checkpoint size.
     *
     * <p>Some state backends may not support incremental checkpoints and ignore this option.
     */
    @Documentation.Section(Documentation.Sections.COMMON_STATE_BACKENDS)
    public static final ConfigOption<Boolean> INCREMENTAL_CHECKPOINTS =
            ConfigOptions.key("state.backend.incremental")
                    .defaultValue(false)
                    .withDescription(
                            "Option whether the state backend should create incremental checkpoints, if possible. For"
                                    + " an incremental checkpoint, only a diff from the previous checkpoint is stored, rather than the"
                                    + " complete checkpoint state. Once enabled, the state size shown in web UI or fetched from rest API"
                                    + " only represents the delta checkpoint size instead of full checkpoint size."
                                    + " Some state backends may not support incremental checkpoints and ignore this option.");

    /**
     * 此选项为此状态后端配置本地恢复。缺省情况下，本地恢复处于去激活状态。
     *
     * <p>本地恢复目前只覆盖键控状态后端。目前，MemoryStateBackend 不支持本地恢复，忽略此选项。
     *
     * This option configures local recovery for this state backend. By default, local recovery is
     * deactivated.
     *
     * <p>Local recovery currently only covers keyed state backends. Currently, MemoryStateBackend
     * does not support local recovery and ignore this option.
     */
    @Documentation.Section(Documentation.Sections.COMMON_STATE_BACKENDS)
    public static final ConfigOption<Boolean> LOCAL_RECOVERY =
            ConfigOptions.key("state.backend.local-recovery")
                    .defaultValue(false)
                    .withDescription(
                            "This option configures local recovery for this state backend. By default, local recovery is "
                                    + "deactivated. Local recovery currently only covers keyed state backends. Currently, MemoryStateBackend does "
                                    + "not support local recovery and ignore this option.");

    /**
     * config参数定义了根目录，用于存储基于文件的本地恢复状态。
     *
     * <p>本地恢复目前只覆盖键控状态后端。目前，MemoryStateBackend不支持本地恢复，忽略此选项。
     *
     * The config parameter defining the root directories for storing file-based state for local
     * recovery.
     *
     * <p>Local recovery currently only covers keyed state backends. Currently, MemoryStateBackend
     * does not support local recovery and ignore this option.
     */
    @Documentation.Section(Documentation.Sections.COMMON_STATE_BACKENDS)
    public static final ConfigOption<String> LOCAL_RECOVERY_TASK_MANAGER_STATE_ROOT_DIRS =
            ConfigOptions.key("taskmanager.state.local.root-dirs")
                    .noDefaultValue()
                    .withDescription(
                            "The config parameter defining the root directories for storing file-based state for local "
                                    + "recovery. Local recovery currently only covers keyed state backends. Currently, MemoryStateBackend does "
                                    + "not support local recovery and ignore this option");

    // ------------------------------------------------------------------------
    //  Options specific to the file-system-based state backends
    // ------------------------------------------------------------------------

    /**
     * 保存点的默认目录。由写保存点到文件系统的状态后端使用(MemoryStateBackend, FsStateBackend,
     * RocksDBStateBackend)。
     *
     * The default directory for savepoints. Used by the state backends that write savepoints to
     * file systems (MemoryStateBackend, FsStateBackend, RocksDBStateBackend).
     */
    @Documentation.Section(value = Documentation.Sections.COMMON_STATE_BACKENDS, position = 3)
    public static final ConfigOption<String> SAVEPOINT_DIRECTORY =
            ConfigOptions.key("state.savepoints.dir")
                    .noDefaultValue()
                    .withDeprecatedKeys("savepoints.state.backend.fs.dir")
                    .withDescription(
                            "The default directory for savepoints. Used by the state backends that write savepoints to"
                                    + " file systems (MemoryStateBackend, FsStateBackend, RocksDBStateBackend).");

    /**
     * 在Flink支持的文件系统中，用于存储数据文件和检查点的元数据的默认目录。存储路径必须可以从所有参与的进程节点
     * (即。所有任务管理器和工作管理器)。
     *
     * The default directory used for storing the data files and meta data of checkpoints in a Flink
     * supported filesystem. The storage path must be accessible from all participating
     * processes/nodes(i.e. all TaskManagers and JobManagers).
     */
    @Documentation.Section(value = Documentation.Sections.COMMON_STATE_BACKENDS, position = 2)
    public static final ConfigOption<String> CHECKPOINTS_DIRECTORY =
            ConfigOptions.key("state.checkpoints.dir")
                    .noDefaultValue()
                    .withDeprecatedKeys("state.backend.fs.checkpointdir")
                    .withDescription(
                            "The default directory used for storing the data files and meta data of checkpoints "
                                    + "in a Flink supported filesystem. The storage path must be accessible from all participating processes/nodes"
                                    + "(i.e. all TaskManagers and JobManagers).");

    /**
     * 状态数据文件的最小大小。所有比它小的状态块都内联存储在根检查点元数据文件中。
     *
     * The minimum size of state data files. All state chunks smaller than that are stored inline in
     * the root checkpoint metadata file.
     */
    @Documentation.Section(Documentation.Sections.EXPERT_STATE_BACKENDS)
    public static final ConfigOption<MemorySize> FS_SMALL_FILE_THRESHOLD =
            ConfigOptions.key("state.backend.fs.memory-threshold")
                    .memoryType()
                    .defaultValue(MemorySize.parse("20kb"))
                    .withDescription(
                            "The minimum size of state data files. All state chunks smaller than that are stored"
                                    + " inline in the root checkpoint metadata file. The max memory threshold for this configuration is 1MB.");

    /**
     * 写入文件系统的检查点流的写缓冲区的默认大小。
     *
     * The default size of the write buffer for the checkpoint streams that write to file systems.
     */
    @Documentation.Section(Documentation.Sections.EXPERT_STATE_BACKENDS)
    public static final ConfigOption<Integer> FS_WRITE_BUFFER_SIZE =
            ConfigOptions.key("state.backend.fs.write-buffer-size")
                    .defaultValue(4 * 1024)
                    .withDescription(
                            String.format(
                                    "The default size of the write buffer for the checkpoint streams that write to file systems. "
                                            + "The actual write buffer size is determined to be the maximum of the value of this option and option '%s'.",
                                    FS_SMALL_FILE_THRESHOLD.key()));
}
