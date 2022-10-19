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
import org.apache.flink.configuration.description.Description;
import org.apache.flink.configuration.description.TextElement;

/** A collection of all configuration options that relate to checkpoints and savepoints. */
// 与检查点和保存点相关的所有配置选项的集合
public class CheckpointingOptions {

    // ------------------------------------------------------------------------
    //  general checkpoint options
    // ------------------------------------------------------------------------

    /**
     * 执行期间用于在集群内本地存储操作员状态的检查点存储。
     *
     * <p>可以通过快捷方式名称或 {@code StateBackendFactory} 的类名指定实现。如果指定了 StateBackendFactory
     *   类名，则实例化工厂（通过其零参数构造函数）并调用其
     *   {@code StateBackendFactory#createFromConfig(ReadableConfig, ClassLoader)} 方法。
     *
     * <p>可识别的快捷方式名称为 “hashmap” 和 “rocksdb”。
     *
     * The checkpoint storage used to store operator state locally within the cluster during
     * execution.
     *
     * <p>The implementation can be specified either via their shortcut name, or via the class name
     * of a {@code StateBackendFactory}. If a StateBackendFactory class name is specified, the
     * factory is instantiated (via its zero-argument constructor) and its {@code
     * StateBackendFactory#createFromConfig(ReadableConfig, ClassLoader)} method is called.
     *
     * <p>Recognized shortcut names are 'hashmap' and 'rocksdb'.
     *
     * @deprecated Use {@link StateBackendOptions#STATE_BACKEND}.
     */
    @Documentation.Section(value = Documentation.Sections.COMMON_STATE_BACKENDS)
    @Documentation.ExcludeFromDocumentation("Hidden for deprecated")
    @Deprecated
    public static final ConfigOption<String> STATE_BACKEND =
            ConfigOptions.key("state.backend")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            Description.builder()
                                    .text("The state backend to be used to store state.")
                                    .linebreak()
                                    .text(
                                            "The implementation can be specified either via their shortcut "
                                                    + " name, or via the class name of a %s. "
                                                    + "If a factory is specified it is instantiated via its "
                                                    + "zero argument constructor and its %s "
                                                    + "method is called.",
                                            TextElement.code("StateBackendFactory"),
                                            TextElement.code(
                                                    "StateBackendFactory#createFromConfig(ReadableConfig, ClassLoader)"))
                                    .linebreak()
                                    .text("Recognized shortcut names are 'hashmap' and 'rocksdb'.")
                                    .build());

    /**
     * 检查点存储用于检查点状态以进行恢复。
     *
     * ...
     *
     * <p>公认的快捷方式名称是 “jobmanager” 和 “filesystem”。
     *
     * The checkpoint storage used to checkpoint state for recovery.
     *
     * <p>The implementation can be specified either via their shortcut name, or via the class name
     * of a {@code CheckpointStorageFactory}. If a CheckpointStorageFactory class name is specified,
     * the factory is instantiated (via its zero-argument constructor) and its {@code
     * CheckpointStorageFactory#createFromConfig(ReadableConfig, ClassLoader)} method is called.
     *
     * <p>Recognized shortcut names are 'jobmanager' and 'filesystem'.
     */
    @Documentation.Section(value = Documentation.Sections.COMMON_STATE_BACKENDS, position = 2)
    public static final ConfigOption<String> CHECKPOINT_STORAGE =
            ConfigOptions.key("state.checkpoint-storage")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "The checkpoint storage implementation to be used to checkpoint state.")
                                    .linebreak()
                                    .text(
                                            "The implementation can be specified either via their shortcut "
                                                    + " name, or via the class name of a %s. "
                                                    + "If a factory is specified it is instantiated via its "
                                                    + "zero argument constructor and its %s "
                                                    + " method is called.",
                                            TextElement.code("CheckpointStorageFactory"),
                                            TextElement.code(
                                                    "CheckpointStorageFactory#createFromConfig(ReadableConfig, ClassLoader)"))
                                    .linebreak()
                                    .text(
                                            "Recognized shortcut names are 'jobmanager' and 'filesystem'.")
                                    .build());

    /** Whether to enable state change log. */
    // 是否启用状态更改日志。
    @Documentation.Section(value = Documentation.Sections.COMMON_STATE_BACKENDS)
    @Documentation.ExcludeFromDocumentation("Hidden for now")
    public static final ConfigOption<Boolean> ENABLE_STATE_CHANGE_LOG =
            ConfigOptions.key("state.backend.changelog.enabled")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            "Whether to enable state backend to write state changes to StateChangelog.");

    /** The maximum number of completed checkpoints to retain. */
    // 要保留的已完成检查点的最大数量。
    @Documentation.Section(Documentation.Sections.COMMON_STATE_BACKENDS)
    public static final ConfigOption<Integer> MAX_RETAINED_CHECKPOINTS =
            ConfigOptions.key("state.checkpoints.num-retained")
                    .defaultValue(1)
                    .withDescription("The maximum number of completed checkpoints to retain.");

    /** @deprecated Checkpoints are aways asynchronous. */
    @Deprecated
    public static final ConfigOption<Boolean> ASYNC_SNAPSHOTS =
            ConfigOptions.key("state.backend.async")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Deprecated option. All state snapshots are asynchronous.");

    /**
     * 如果可能，选择状态后端是否应创建增量检查点。对于增量检查点，仅存储与前一个检查点的差异，而不是完整的检查点状态。
     *
     * <p>启用后，Web UI 中显示的状态大小或从 REST API 获取的状态大小仅表示增量检查点大小，而不是完整检查点大小。
     *
     * <p>某些状态后端可能不支持增量检查点并忽略此选项。
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
     * 此选项为此状态后端配置本地恢复。默认情况下，本地恢复处于禁用状态。
     *
     * <p>本地恢复目前仅涵盖键控状态后端。目前 MemoryStateBackend 和 HashMapStateBackend 不支持本地恢复，
     *   忽略该选项。
     *
     * This option configures local recovery for this state backend. By default, local recovery is
     * deactivated.
     *
     * <p>Local recovery currently only covers keyed state backends. Currently, MemoryStateBackend
     * and HashMapStateBackend do not support local recovery and ignore this option.
     */
    @Documentation.Section(Documentation.Sections.COMMON_STATE_BACKENDS)
    public static final ConfigOption<Boolean> LOCAL_RECOVERY =
            ConfigOptions.key("state.backend.local-recovery")
                    .defaultValue(false)
                    .withDescription(
                            "This option configures local recovery for this state backend. By default, local recovery is "
                                    + "deactivated. Local recovery currently only covers keyed state backends. Currently, the MemoryStateBackend "
                                    + "does not support local recovery and ignores this option.");

    /**
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
     * 保存点的默认目录。由将保存点写入文件系统的状态后端（HashMapStateBackend、EmbeddedRocksDBStateBackend）使用。
     *
     * The default directory for savepoints. Used by the state backends that write savepoints to
     * file systems (HashMapStateBackend, EmbeddedRocksDBStateBackend).
     */
    @Documentation.Section(value = Documentation.Sections.COMMON_STATE_BACKENDS, position = 3)
    public static final ConfigOption<String> SAVEPOINT_DIRECTORY =
            ConfigOptions.key("state.savepoints.dir")
                    .noDefaultValue()
                    .withDeprecatedKeys("savepoints.state.backend.fs.dir")
                    .withDescription(
                            "The default directory for savepoints. Used by the state backends that write savepoints to"
                                    + " file systems (HashMapStateBackend, EmbeddedRocksDBStateBackend).");

    /**
     * Flink 支持的文件系统中用于存储检查点的数据文件和元数据的默认目录。存储路径必须可以从所有参与的进程节点（即所有
     * TaskManager 和 JobManager）访问。
     *
     * The default directory used for storing the data files and meta data of checkpoints in a Flink
     * supported filesystem. The storage path must be accessible from all participating
     * processes/nodes(i.e. all TaskManagers and JobManagers).
     */
    @Documentation.Section(value = Documentation.Sections.COMMON_STATE_BACKENDS, position = 2)
    public static final ConfigOption<String> CHECKPOINTS_DIRECTORY =
            ConfigOptions.key("state.checkpoints.dir")
                    .stringType()
                    .noDefaultValue()
                    .withDeprecatedKeys("state.backend.fs.checkpointdir")
                    .withDescription(
                            "The default directory used for storing the data files and meta data of checkpoints "
                                    + "in a Flink supported filesystem. The storage path must be accessible from all participating processes/nodes"
                                    + "(i.e. all TaskManagers and JobManagers).");

    /**
     * 状态数据文件的最小大小。所有小于该值的状态块都内联存储在根检查点元数据文件中。
     *
     * The minimum size of state data files. All state chunks smaller than that are stored inline in
     * the root checkpoint metadata file.
     */
    @Documentation.Section(Documentation.Sections.EXPERT_STATE_BACKENDS)
    public static final ConfigOption<MemorySize> FS_SMALL_FILE_THRESHOLD =
            ConfigOptions.key("state.storage.fs.memory-threshold")
                    .memoryType()
                    .defaultValue(MemorySize.parse("20kb"))
                    .withDescription(
                            "The minimum size of state data files. All state chunks smaller than that are stored"
                                    + " inline in the root checkpoint metadata file. The max memory threshold for this configuration is 1MB.")
                    .withDeprecatedKeys("state.backend.fs.memory-threshold");

    /**
     * 写入文件系统的检查点流的写入缓冲区的默认大小。
     *
     * The default size of the write buffer for the checkpoint streams that write to file systems.
     */
    @Documentation.Section(Documentation.Sections.EXPERT_STATE_BACKENDS)
    public static final ConfigOption<Integer> FS_WRITE_BUFFER_SIZE =
            ConfigOptions.key("state.storage.fs.write-buffer-size")
                    .intType()
                    .defaultValue(4 * 1024)
                    .withDescription(
                            String.format(
                                    "The default size of the write buffer for the checkpoint streams that write to file systems. "
                                            + "The actual write buffer size is determined to be the maximum of the value of this option and option '%s'.",
                                    FS_SMALL_FILE_THRESHOLD.key()))
                    .withDeprecatedKeys("state.backend.fs.write-buffer-size");
}
