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

package org.apache.flink.table.api.config;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.annotation.docs.Documentation;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.description.Description;

import java.time.Duration;

import static org.apache.flink.configuration.ConfigOptions.key;
import static org.apache.flink.configuration.description.TextElement.code;
import static org.apache.flink.configuration.description.TextElement.text;

/**
 * 这个类保存了Flink的table模块使用的配置常量。
 *
 * <p>仅用于Blink计划器。注意:该类中的所有选项键必须以"table.exec"开头。
 *
 * This class holds configuration constants used by Flink's table module.
 *
 * <p>This is only used for the Blink planner.
 *
 * <p>NOTE: All option keys in this class must start with "table.exec".
 */
@PublicEvolving
public class ExecutionConfigOptions {

    // ------------------------------------------------------------------------
    //  State Options
    // ------------------------------------------------------------------------

    // J: 装填过期时间，默认不过期
    @Documentation.TableOption(execMode = Documentation.ExecMode.STREAMING)
    public static final ConfigOption<Duration> IDLE_STATE_RETENTION =
            key("table.exec.state.ttl")
                    .durationType()
                    .defaultValue(Duration.ofMillis(0))
                    .withDescription(
                            "Specifies a minimum time interval for how long idle state "
                                    + "(i.e. state which was not updated), will be retained. State will never be "
                                    + "cleared until it was idle for less than the minimum time, and will be cleared "
                                    + "at some time after it was idle. Default is never clean-up the state. "
                                    + "NOTE: Cleaning up state requires additional overhead for bookkeeping. "
                                    + "Default value is 0, which means that it will never clean up state.");

    // ------------------------------------------------------------------------
    //  Source Options
    // ------------------------------------------------------------------------

    // 检测 source 空闲的超时，默认不检测，用于下游任务无需等待该 source 水印
    @Documentation.TableOption(execMode = Documentation.ExecMode.STREAMING)
    public static final ConfigOption<Duration> TABLE_EXEC_SOURCE_IDLE_TIMEOUT =
            key("table.exec.source.idle-timeout")
                    .durationType()
                    .defaultValue(Duration.ofMillis(0))
                    .withDescription(
                            "When a source do not receive any elements for the timeout time, "
                                    + "it will be marked as temporarily idle. This allows downstream "
                                    + "tasks to advance their watermarks without the need to wait for "
                                    + "watermarks from this source while it is idle. "
                                    + "Default value is 0, which means detecting source idleness is not enabled.");

    // J: CDC 相关的去重
    // 指示作业中的CDC (Change Data Capture)源是否会产生重复的更改事件，需要框架去重并获得一致的结果。CDC源是
    // 指产生完整变更事件的源，包括INSERTUPDATE_BEFORE“+”UPDATE_AFTERDELETE，例如Debezium格式的Kafka源。
    // 该配置默认为false
    @Documentation.TableOption(execMode = Documentation.ExecMode.STREAMING)
    public static final ConfigOption<Boolean> TABLE_EXEC_SOURCE_CDC_EVENTS_DUPLICATE =
            key("table.exec.source.cdc-events-duplicate")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Indicates whether the CDC (Change Data Capture) sources "
                                                    + "in the job will produce duplicate change events that requires the "
                                                    + "framework to deduplicate and get consistent result. CDC source refers to the "
                                                    + "source that produces full change events, including INSERT/UPDATE_BEFORE/"
                                                    + "UPDATE_AFTER/DELETE, for example Kafka source with Debezium format. "
                                                    + "The value of this configuration is false by default.")
                                    .linebreak()
                                    .linebreak()
                                    .text(
                                            "However, it's a common case that there are duplicate change events. "
                                                    + "Because usually the CDC tools (e.g. Debezium) work in at-least-once delivery "
                                                    + "when failover happens. Thus, in the abnormal situations Debezium may deliver "
                                                    + "duplicate change events to Kafka and Flink will get the duplicate events. "
                                                    + "This may cause Flink query to get wrong results or unexpected exceptions.")
                                    .linebreak()
                                    .linebreak()
                                    .text(
                                            "Therefore, it is recommended to turn on this configuration if your CDC tool "
                                                    + "is at-least-once delivery. Enabling this configuration requires to define "
                                                    + "PRIMARY KEY on the CDC sources. The primary key will be used to deduplicate "
                                                    + "change events and generate normalized changelog stream at the cost of "
                                                    + "an additional stateful operator.")
                                    .build());

    // ------------------------------------------------------------------------
    //  Sink Options
    // ------------------------------------------------------------------------

    // 表上的NOT NULL列约束强制空值不能插入到表中。Flink支持、“error”(默认)和“drop”强制行为。默认情况下，
    // Flink将检查值，并在null值将写入NOT null列时抛出运行时异常。用户可以将行为从“drop”改为，无声地删除这些记录，
    // 而不会抛出异常
    // J: 对于一些维度信息未上报的，定义时定义为 Not null 的场景适用
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH_STREAMING)
    public static final ConfigOption<NotNullEnforcer> TABLE_EXEC_SINK_NOT_NULL_ENFORCER =
            key("table.exec.sink.not-null-enforcer")
                    .enumType(NotNullEnforcer.class)
                    .defaultValue(NotNullEnforcer.ERROR)
                    .withDescription(
                            "The NOT NULL column constraint on a table enforces that "
                                    + "null values can't be inserted into the table. Flink supports "
                                    + "'error' (default) and 'drop' enforcement behavior. By default, "
                                    + "Flink will check values and throw runtime exception when null values writing "
                                    + "into NOT NULL columns. Users can change the behavior to 'drop' to "
                                    + "silently drop such records without throwing exception.");

    @Documentation.TableOption(execMode = Documentation.ExecMode.STREAMING)
    public static final ConfigOption<UpsertMaterialize> TABLE_EXEC_SINK_UPSERT_MATERIALIZE =
            key("table.exec.sink.upsert-materialize")
                    .enumType(UpsertMaterialize.class)
                    .defaultValue(UpsertMaterialize.AUTO)
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Because of the disorder of ChangeLog data caused by Shuffle in distributed system, "
                                                    + "the data received by Sink may not be the order of global upsert. "
                                                    + "So add upsert materialize operator before upsert sink. It receives the "
                                                    + "upstream changelog records and generate an upsert view for the downstream.")
                                    .linebreak()
                                    .text(
                                            "By default, the materialize operator will be added when a distributed disorder "
                                                    + "occurs on unique keys. You can also choose no materialization(NONE) "
                                                    + "or force materialization(FORCE).")
                                    .build());

    // ------------------------------------------------------------------------
    //  Sort Options
    // ------------------------------------------------------------------------
    // J: 当 order by 未设置 limit 时的默认输出个数
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH)
    public static final ConfigOption<Integer> TABLE_EXEC_SORT_DEFAULT_LIMIT =
            key("table.exec.sort.default-limit")
                    .defaultValue(-1)
                    .withDescription(
                            "Default limit when user don't set a limit after order by. -1 indicates that this configuration is ignored.");

    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH)
    public static final ConfigOption<Integer> TABLE_EXEC_SORT_MAX_NUM_FILE_HANDLES =
            key("table.exec.sort.max-num-file-handles")
                    .defaultValue(128)
                    .withDescription(
                            "The maximal fan-in for external merge sort. It limits the number of file handles per operator. "
                                    + "If it is too small, may cause intermediate merging. But if it is too large, "
                                    + "it will cause too many files opened at the same time, consume memory and lead to random reading.");

    // 是否异步合并已排序的溢出文件
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH)
    public static final ConfigOption<Boolean> TABLE_EXEC_SORT_ASYNC_MERGE_ENABLED =
            key("table.exec.sort.async-merge-enabled")
                    .defaultValue(true)
                    .withDescription("Whether to asynchronously merge sorted spill files.");

    // ------------------------------------------------------------------------
    //  Spill Options
    // ------------------------------------------------------------------------
    // J: 是否压缩溢出的数据，当前仅支持 sort、hash 聚合、hash 连接 算子
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH)
    public static final ConfigOption<Boolean> TABLE_EXEC_SPILL_COMPRESSION_ENABLED =
            key("table.exec.spill-compression.enabled")
                    .defaultValue(true)
                    .withDescription(
                            "Whether to compress spilled data. "
                                    + "Currently we only support compress spilled data for sort and hash-agg and hash-join operators.");

    // J: 溢出压缩的 block 大小
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH)
    public static final ConfigOption<MemorySize> TABLE_EXEC_SPILL_COMPRESSION_BLOCK_SIZE =
            key("table.exec.spill-compression.block-size")
                    .memoryType()
                    .defaultValue(MemorySize.parse("64 kb"))
                    .withDescription(
                            "The memory size used to do compress when spilling data. "
                                    + "The larger the memory, the higher the compression ratio, "
                                    + "but more memory resource will be consumed by the job.");

    // ------------------------------------------------------------------------
    //  Resource Options
    // ------------------------------------------------------------------------
    // 对于所有算子的并行度，比 StreamExecutionEnvironment 优先级更高
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH_STREAMING)
    public static final ConfigOption<Integer> TABLE_EXEC_RESOURCE_DEFAULT_PARALLELISM =
            key("table.exec.resource.default-parallelism")
                    .defaultValue(-1)
                    .withDescription(
                            "Sets default parallelism for all operators "
                                    + "(such as aggregate, join, filter) to run with parallel instances. "
                                    + "This config has a higher priority than parallelism of "
                                    + "StreamExecutionEnvironment (actually, this config overrides the parallelism "
                                    + "of StreamExecutionEnvironment). A value of -1 indicates that no "
                                    + "default parallelism is set, then it will fallback to use the parallelism "
                                    + "of StreamExecutionEnvironment.");

    // J: 外部缓冲内存大小，用于 sort merge join、nested join、over window
    @Documentation.ExcludeFromDocumentation(
            "Beginning from Flink 1.10, this is interpreted as a weight hint "
                    + "instead of an absolute memory requirement. Users should not need to change these carefully tuned weight hints.")
    public static final ConfigOption<MemorySize> TABLE_EXEC_RESOURCE_EXTERNAL_BUFFER_MEMORY =
            key("table.exec.resource.external-buffer-memory")
                    .memoryType()
                    .defaultValue(MemorySize.parse("10 mb"))
                    .withDescription(
                            "Sets the external buffer memory size that is used in sort merge join"
                                    + " and nested join and over window. Note: memory size is only a weight hint,"
                                    + " it will affect the weight of memory that can be applied by a single operator"
                                    + " in the task, the actual memory used depends on the running environment.");

    // J: hash 聚合算子管理的内存大小
    @Documentation.ExcludeFromDocumentation(
            "Beginning from Flink 1.10, this is interpreted as a weight hint "
                    + "instead of an absolute memory requirement. Users should not need to change these carefully tuned weight hints.")
    public static final ConfigOption<MemorySize> TABLE_EXEC_RESOURCE_HASH_AGG_MEMORY =
            key("table.exec.resource.hash-agg.memory")
                    .memoryType()
                    .defaultValue(MemorySize.parse("128 mb"))
                    .withDescription(
                            "Sets the managed memory size of hash aggregate operator."
                                    + " Note: memory size is only a weight hint, it will affect the weight of memory"
                                    + " that can be applied by a single operator in the task, the actual memory used"
                                    + " depends on the running environment.");

    // J: hash 连接算子管理的内存大小
    @Documentation.ExcludeFromDocumentation(
            "Beginning from Flink 1.10, this is interpreted as a weight hint "
                    + "instead of an absolute memory requirement. Users should not need to change these carefully tuned weight hints.")
    public static final ConfigOption<MemorySize> TABLE_EXEC_RESOURCE_HASH_JOIN_MEMORY =
            key("table.exec.resource.hash-join.memory")
                    .memoryType()
                    .defaultValue(MemorySize.parse("128 mb"))
                    .withDescription(
                            "Sets the managed memory for hash join operator. It defines the lower"
                                    + " limit. Note: memory size is only a weight hint, it will affect the weight of"
                                    + " memory that can be applied by a single operator in the task, the actual"
                                    + " memory used depends on the running environment.");

    // J: 用于排序算子的缓冲大小
    @Documentation.ExcludeFromDocumentation(
            "Beginning from Flink 1.10, this is interpreted as a weight hint "
                    + "instead of an absolute memory requirement. Users should not need to change these carefully tuned weight hints.")
    public static final ConfigOption<MemorySize> TABLE_EXEC_RESOURCE_SORT_MEMORY =
            key("table.exec.resource.sort.memory")
                    .memoryType()
                    .defaultValue(MemorySize.parse("128 mb"))
                    .withDescription(
                            "Sets the managed buffer memory size for sort operator. Note: memory"
                                    + " size is only a weight hint, it will affect the weight of memory that can be"
                                    + " applied by a single operator in the task, the actual memory used depends on"
                                    + " the running environment.");

    // ------------------------------------------------------------------------
    //  Agg Options
    // ------------------------------------------------------------------------

    /** See {@code org.apache.flink.table.runtime.operators.window.grouping.HeapWindowsGrouping}. */
    // J: 窗口聚合算子中元素缓冲的限制条数
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH)
    public static final ConfigOption<Integer> TABLE_EXEC_WINDOW_AGG_BUFFER_SIZE_LIMIT =
            key("table.exec.window-agg.buffer-size-limit")
                    .defaultValue(100 * 1000)
                    .withDescription(
                            "Sets the window elements buffer size limit used in group window agg operator.");

    // ------------------------------------------------------------------------
    //  Async Lookup Options
    // ------------------------------------------------------------------------
    // 异步查找连接可以触发的异步 io 操作的最大数量
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH_STREAMING)
    public static final ConfigOption<Integer> TABLE_EXEC_ASYNC_LOOKUP_BUFFER_CAPACITY =
            key("table.exec.async-lookup.buffer-capacity")
                    .defaultValue(100)
                    .withDescription(
                            "The max number of async i/o operation that the async lookup join can trigger.");

    // 异步操作完成的异步超时
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH_STREAMING)
    public static final ConfigOption<Duration> TABLE_EXEC_ASYNC_LOOKUP_TIMEOUT =
            key("table.exec.async-lookup.timeout")
                    .durationType()
                    .defaultValue(Duration.ofMinutes(3))
                    .withDescription(
                            "The async timeout for the asynchronous operation to complete.");

    // ------------------------------------------------------------------------
    //  MiniBatch Options
    // ------------------------------------------------------------------------
    // J: 是否开启 table mini-batch
    //   开启后，必须设置 `table.exec.mini-batch.size`、`table.exec.mini-batch.allow-latency`
    @Documentation.TableOption(execMode = Documentation.ExecMode.STREAMING)
    public static final ConfigOption<Boolean> TABLE_EXEC_MINIBATCH_ENABLED =
            key("table.exec.mini-batch.enabled")
                    .defaultValue(false)
                    .withDescription(
                            "Specifies whether to enable MiniBatch optimization. "
                                    + "MiniBatch is an optimization to buffer input records to reduce state access. "
                                    + "This is disabled by default. To enable this, users should set this config to true. "
                                    + "NOTE: If mini-batch is enabled, 'table.exec.mini-batch.allow-latency' and "
                                    + "'table.exec.mini-batch.size' must be set.");

    // J: 使用 MiniBatch 缓冲输入记录时最大的延迟
    @Documentation.TableOption(execMode = Documentation.ExecMode.STREAMING)
    public static final ConfigOption<Duration> TABLE_EXEC_MINIBATCH_ALLOW_LATENCY =
            key("table.exec.mini-batch.allow-latency")
                    .durationType()
                    .defaultValue(Duration.ofMillis(0))
                    .withDescription(
                            "The maximum latency can be used for MiniBatch to buffer input records. "
                                    + "MiniBatch is an optimization to buffer input records to reduce state access. "
                                    + "MiniBatch is triggered with the allowed latency interval and when the maximum number of buffered records reached. "
                                    + "NOTE: If "
                                    + TABLE_EXEC_MINIBATCH_ENABLED.key()
                                    + " is set true, its value must be greater than zero.");

    // J: 使用 MiniBatch 时输入记录的最大记录数
    @Documentation.TableOption(execMode = Documentation.ExecMode.STREAMING)
    public static final ConfigOption<Long> TABLE_EXEC_MINIBATCH_SIZE =
            key("table.exec.mini-batch.size")
                    .defaultValue(-1L)
                    .withDescription(
                            "The maximum number of input records can be buffered for MiniBatch. "
                                    + "MiniBatch is an optimization to buffer input records to reduce state access. "
                                    + "MiniBatch is triggered with the allowed latency interval and when the maximum number of buffered records reached. "
                                    + "NOTE: MiniBatch only works for non-windowed aggregations currently. If "
                                    + TABLE_EXEC_MINIBATCH_ENABLED.key()
                                    + " is set true, its value must be positive.");

    // ------------------------------------------------------------------------
    //  Other Exec Options
    // ------------------------------------------------------------------------
    // J: 禁用的算子。使用 `,` 分割，可选的包含 NestedLoopJoin, ShuffleHashJoin, BroadcastHashJoin, SortMergeJoin, HashAgg, SortAgg
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH)
    public static final ConfigOption<String> TABLE_EXEC_DISABLED_OPERATORS =
            key("table.exec.disabled-operators")
                    .noDefaultValue()
                    .withDescription(
                            "Mainly for testing. A comma-separated list of operator names, each name "
                                    + "represents a kind of disabled operator.\n"
                                    + "Operators that can be disabled include \"NestedLoopJoin\", \"ShuffleHashJoin\", \"BroadcastHashJoin\", "
                                    + "\"SortMergeJoin\", \"HashAgg\", \"SortAgg\".\n"
                                    + "By default no operator is disabled.");

    // J: 执行的 shuffle 模式，默认为 ALL_EDGES_BLOCKING
    @Documentation.TableOption(execMode = Documentation.ExecMode.BATCH)
    public static final ConfigOption<String> TABLE_EXEC_SHUFFLE_MODE =
            key("table.exec.shuffle-mode")
                    .stringType()
                    .defaultValue("ALL_EDGES_BLOCKING")
                    .withDescription(
                            Description.builder()
                                    .text("Sets exec shuffle mode.")
                                    .linebreak()
                                    .text("Accepted values are:")
                                    .list(
                                            text(
                                                    "%s: All edges will use blocking shuffle.",
                                                    code("ALL_EDGES_BLOCKING")),
                                            text(
                                                    "%s: Forward edges will use pipelined shuffle, others blocking.",
                                                    code("FORWARD_EDGES_PIPELINED")),
                                            text(
                                                    "%s: Pointwise edges will use pipelined shuffle, others blocking. "
                                                            + "Pointwise edges include forward and rescale edges.",
                                                    code("POINTWISE_EDGES_PIPELINED")),
                                            text(
                                                    "%s: All edges will use pipelined shuffle.",
                                                    code("ALL_EDGES_PIPELINED")),
                                            text(
                                                    "%s: the same as %s. Deprecated.",
                                                    code("batch"), code("ALL_EDGES_BLOCKING")),
                                            text(
                                                    "%s: the same as %s. Deprecated.",
                                                    code("pipelined"), code("ALL_EDGES_PIPELINED")))
                                    .text(
                                            "Note: Blocking shuffle means data will be fully produced before sent to consumer tasks. "
                                                    + "Pipelined shuffle means data will be sent to consumer tasks once produced.")
                                    .build());

    // ------------------------------------------------------------------------------------------
    // Enum option types
    // ------------------------------------------------------------------------------------------

    /** The enforcer to guarantee NOT NULL column constraint when writing data into sink. */
    // 当向 sink 写入数据时，强制保证 NOT NULL 列约束。
    public enum NotNullEnforcer {
        /** Throws runtime exception when writing null values into NOT NULL column. */
        ERROR,
        /** Drop records when writing null values into NOT NULL column. */
        DROP
    }

    /** Upsert materialize strategy before sink. */
    // 在下沉之前先实现战略
    public enum UpsertMaterialize {

        /** In no case will materialize operator be added. */
        NONE,

        /** Add materialize operator when a distributed disorder occurs on unique keys. */
        AUTO,

        /** Add materialize operator in any case. */
        FORCE
    }
}
