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

package org.apache.flink.table.filesystem;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.description.Description;
import org.apache.flink.table.factories.FactoryUtil;

import java.time.Duration;

import static org.apache.flink.configuration.ConfigOptions.key;
import static org.apache.flink.configuration.description.TextElement.text;

/** This class holds configuration constants used by filesystem(Including hive) connector. */
// 这个类保存文件系统(包括 hive)连接器使用的配置常量。
public class FileSystemOptions {

    public static final ConfigOption<String> PATH =
            key("path").stringType().noDefaultValue().withDescription("The path of a directory");

    // 默认的分区名称
    public static final ConfigOption<String> PARTITION_DEFAULT_NAME =
            key("partition.default-name")
                    .stringType()
                    .defaultValue("__DEFAULT_PARTITION__")
                    .withDescription(
                            "The default partition name in case the dynamic partition"
                                    + " column value is null/empty string.");

    // 滚动前的最大部分文件大小
    public static final ConfigOption<MemorySize> SINK_ROLLING_POLICY_FILE_SIZE =
            key("sink.rolling-policy.file-size")
                    .memoryType()
                    .defaultValue(MemorySize.ofMebiBytes(128))
                    .withDescription("The maximum part file size before rolling.");

    // 回滚恢复间隔
    public static final ConfigOption<Duration> SINK_ROLLING_POLICY_ROLLOVER_INTERVAL =
            key("sink.rolling-policy.rollover-interval")
                    .durationType()
                    .defaultValue(Duration.ofMinutes(30))
                    .withDescription(
                            "The maximum time duration a part file can stay open before rolling"
                                    + " (by default long enough to avoid too many small files). The frequency at which"
                                    + " this is checked is controlled by the 'sink.rolling-policy.check-interval' option.");

    // 基于时间的滚动策略检查间隔。这控制了根据'sink.roll-policy.rollover-interval'检查零件文件是否
    // 应该滚动的频率");
    public static final ConfigOption<Duration> SINK_ROLLING_POLICY_CHECK_INTERVAL =
            key("sink.rolling-policy.check-interval")
                    .durationType()
                    .defaultValue(Duration.ofMinutes(1))
                    .withDescription(
                            "The interval for checking time based rolling policies. "
                                    + "This controls the frequency to check whether a part file should rollover based on 'sink.rolling-policy.rollover-interval'.");

    // 在接收阶段启用动态分区字段洗牌数据的选项，这可以大大减少文件系统接收的文件数量，但可能导致数据倾斜
    public static final ConfigOption<Boolean> SINK_SHUFFLE_BY_PARTITION =
            key("sink.shuffle-by-partition.enable")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            "The option to enable shuffle data by dynamic partition fields in sink"
                                    + " phase, this can greatly reduce the number of file for filesystem sink but may"
                                    + " lead data skew.");

    // 注意:请确保每个分区文件都应该以原子的方式写入，否则读取器可能会得到不完整的数据。
    public static final ConfigOption<Boolean> STREAMING_SOURCE_ENABLE =
            key("streaming-source.enable")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            Description.builder()
                                    .text("Enable streaming source or not.")
                                    .linebreak()
                                    .text(
                                            " NOTES: Please make sure that each partition/file should be written"
                                                    + " atomically, otherwise the reader may get incomplete data.")
                                    .build());

    // 选项设置分区为读取，支持的值是
    //   All(读取所有分区)
    //   Latest(按照“streaming-source.partition.order”的顺序读取Latest分区。，这只在Hive源表作为时态表时有效)"
    public static final ConfigOption<String> STREAMING_SOURCE_PARTITION_INCLUDE =
            key("streaming-source.partition.include")
                    .stringType()
                    .defaultValue("all")
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Option to set the partitions to read, supported values are")
                                    .list(
                                            text("all (read all partitions)"),
                                            text(
                                                    "latest (read latest partition in order of 'streaming-source.partition.order', this only works when a streaming Hive source table is used as a temporal table)"))
                                    .build());

    // 连续监控分区文件的时间间隔
    public static final ConfigOption<Duration> STREAMING_SOURCE_MONITOR_INTERVAL =
            key("streaming-source.monitor-interval")
                    .durationType()
                    .noDefaultValue()
                    .withDescription("Time interval for consecutively monitoring partition/file.");

    // 流源的分区顺序，支持的值为
    //   create-time: 比较 partition file 的创建时间，不是 Hive meta store 中的分区创建时间，而是文件系统中 folder file 的修改时间;
    //                例如，在文件夹中添加新文件可能会影响数据的使用方式)
    //   partition-time: 比较从分区名称提取的时间
    //   partition-name:按字典顺序比较分区名称
    public static final ConfigOption<String> STREAMING_SOURCE_PARTITION_ORDER =
            key("streaming-source.partition-order")
                    .stringType()
                    .defaultValue("partition-name")
                    .withDeprecatedKeys("streaming-source.consume-order")
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "The partition order of the streaming source, supported values are")
                                    .list(
                                            text(
                                                    "create-time (compares partition/file creation time, which is not the partition creation time in the Hive metastore, "
                                                            + "but the folder/file modification time in the filesystem; e.g., adding a new file into "
                                                            + "the folder may affect how the data is consumed)"),
                                            text(
                                                    "partition-time (compares the time extracted from the partition name)"),
                                            text(
                                                    "partition-name (compares partition names lexicographically)"))
                                    .text(
                                            "This is a synonym for the deprecated 'streaming-source.consume-order' option.")
                                    .build());

    // 从头开始消费
    public static final ConfigOption<String> STREAMING_SOURCE_CONSUME_START_OFFSET =
            key("streaming-source.consume-start-offset")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Start offset for streaming consuming. How to parse and compare offsets depends on 'streaming-source.partition-order'.")
                                    .list(
                                            text(
                                                    "For 'create-time' and 'partition-time' it should be a timestamp string (yyyy-[m]m-[d]d [hh:mm:ss])."),
                                            text(
                                                    "For 'partition-time' it will use a partition time extractor to extract the time from the partition."),
                                            text(
                                                    "For 'partition-name' it is the name of the partition, e.g. 'pt_year=2020/pt_mon=10/pt_day=01'."))
                                    .build());

    // 分区的时间抽取类型
    // 时间提取器从分区值中提取时间。这可以是'default'或自定义提取器类。对于'default'，可以配置时间戳模式
    public static final ConfigOption<String> PARTITION_TIME_EXTRACTOR_KIND =
            key("partition.time-extractor.kind")
                    .stringType()
                    .defaultValue("default")
                    .withDescription(
                            "Time extractor to extract time from partition values. "
                                    + "This can either be 'default' or a custom extractor class. "
                                    + "For 'default', you can configure a timestamp pattern.");

    // 用于实现 PartitionTimeExtractor 接口的提取器类
    public static final ConfigOption<String> PARTITION_TIME_EXTRACTOR_CLASS =
            key("partition.time-extractor.class")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "The extractor class for implement PartitionTimeExtractor interface.");

    // 在 `partition.time-extractor.kind` 设置为 default 时，可指定时间戳的格式化
    public static final ConfigOption<String> PARTITION_TIME_EXTRACTOR_TIMESTAMP_PATTERN =
            key("partition.time-extractor.timestamp-pattern")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "When 'partition.time-extractor.kind' is set to 'default', "
                                                    + "you can specify a pattern to get a timestamp from partitions.")
                                    .list(
                                            text(
                                                    "By default, a format of 'yyyy-mm-dd hh:mm:ss' is read from the first field."),
                                            text(
                                                    "If the timestamp in the partition is a single field called 'dt', you can use '$dt'."),
                                            text(
                                                    "If it is spread across multiple fields for year, month, day, and hour, you can use '$year-$month-$day $hour:00:00'."),
                                            text(
                                                    "If the timestamp is in fields dt and hour, you can use '$dt $hour:00:00'."))
                                    .build());

    // 在查找连接中构建表的缓存TTL(例如10分钟)
    public static final ConfigOption<Duration> LOOKUP_JOIN_CACHE_TTL =
            key("lookup.join.cache.ttl")
                    .durationType()
                    .defaultValue(Duration.ofMinutes(60))
                    .withDescription(
                            "The cache TTL (e.g. 10min) for the build table in lookup join.");

    // 分区提交的触发器类型，支持的值是
    public static final ConfigOption<String> SINK_PARTITION_COMMIT_TRIGGER =
            key("sink.partition-commit.trigger")
                    .stringType()
                    .defaultValue("process-time")
                    .withDescription(
                            Description.builder()
                                    .text("Trigger type for partition commit, supported values are")
                                    .list(
                                            text(
                                                    "process-time (based on the time of the machine, requires "
                                                            + "neither partition time extraction nor watermark generation; "
                                                            + "commits partition once the current system time passes partition creation system time plus delay)"),
                                            text(
                                                    "partition-time (based on the time extracted from partition values, "
                                                            + "requires watermark generation; commits partition once "
                                                            + "the watermark passes the time extracted from partition values plus delay)"))
                                    .build());

    // 该分区在延迟时间之前不会提交。" + " day分区为'1 d'， hour分区为'1 h'。"
    public static final ConfigOption<Duration> SINK_PARTITION_COMMIT_DELAY =
            key("sink.partition-commit.delay")
                    .durationType()
                    .defaultValue(Duration.ofMillis(0))
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "The partition will not commit until the delay time. "
                                                    + "The value should be '1 d' for day partitions and '1 h' for hour partitions.")
                                    .build());

    // 指定时区，默认为 UTC
    public static final ConfigOption<String> SINK_PARTITION_COMMIT_WATERMARK_TIME_ZONE =
            key("sink.partition-commit.watermark-time-zone")
                    .stringType()
                    .defaultValue("UTC")
                    .withDescription(
                            "The time zone to parse the long watermark value to TIMESTAMP value,"
                                    + " the parsed watermark timestamp is used to compare with partition time"
                                    + " to decide the partition should commit or not."
                                    + " The default value is 'UTC', which means the watermark is defined on TIMESTAMP column or not defined."
                                    + " If the watermark is defined on TIMESTAMP_LTZ column, the time zone of watermark is user configured time zone,"
                                    + " the the value should be the user configured local time zone. The option value is either a full name"
                                    + " such as 'America/Los_Angeles', or a custom timezone id such as 'GMT-08:00'.");

    // 提交分区的策略是通知下游的应用程序该分区已经完成写入，该分区已经准备好被读取
    public static final ConfigOption<String> SINK_PARTITION_COMMIT_POLICY_KIND =
            key("sink.partition-commit.policy.kind")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Policy to commit a partition is to notify the downstream"
                                    + " application that the partition has finished writing, the partition"
                                    + " is ready to be read."
                                    + " metastore: add partition to metastore. Only hive table supports metastore"
                                    + " policy, file system manages partitions through directory structure."
                                    + " success-file: add '_success' file to directory."
                                    + " Both can be configured at the same time: 'metastore,success-file'."
                                    + " custom: use policy class to create a commit policy."
                                    + " Support to configure multiple policies: 'metastore,success-file'.");

    // 用于实现PartitionCommitPolicy接口的分区提交策略类。只能在自定义提交策略中工作
    public static final ConfigOption<String> SINK_PARTITION_COMMIT_POLICY_CLASS =
            key("sink.partition-commit.policy.class")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "The partition commit policy class for implement"
                                    + " PartitionCommitPolicy interface. Only work in custom commit policy");

    // 成功文件分区提交策略的文件名，默认为“_SUCCESS”。
    public static final ConfigOption<String> SINK_PARTITION_COMMIT_SUCCESS_FILE_NAME =
            key("sink.partition-commit.success-file.name")
                    .stringType()
                    .defaultValue("_SUCCESS")
                    .withDescription(
                            "The file name for success-file partition commit policy,"
                                    + " default is '_SUCCESS'.");

    // 是否在流接收器中启用自动压缩。数据将被写入临时文件。检查点完成后，对检查点生成的临时文件进行压缩。"压缩前临时文件是不可见的。
    public static final ConfigOption<Boolean> AUTO_COMPACTION =
            key("auto-compaction")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            "Whether to enable automatic compaction in streaming sink or not.\n"
                                    + "The data will be written to temporary files. After the checkpoint is"
                                    + " completed, the temporary files generated by a checkpoint will be compacted.\n"
                                    + "The temporary files are invisible before compaction.");

    public static final ConfigOption<MemorySize> COMPACTION_FILE_SIZE =
            key("compaction.file-size")
                    .memoryType()
                    .noDefaultValue()
                    .withDescription(
                            "The compaction target file size, the default value is the rolling file size.");

    public static final ConfigOption<Integer> SINK_PARALLELISM = FactoryUtil.SINK_PARALLELISM;
}
