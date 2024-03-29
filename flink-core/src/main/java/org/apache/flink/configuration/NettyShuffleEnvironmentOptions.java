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

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.annotation.docs.Documentation;

import static org.apache.flink.configuration.ConfigOptions.key;

/** The set of configuration options relating to network stack. */
// 与网络堆栈相关的配置选项集。
@PublicEvolving
public class NettyShuffleEnvironmentOptions {

    // ------------------------------------------------------------------------
    //  Network General Options
    // ------------------------------------------------------------------------

    /**
     * 任务管理器期望接收传输信封的默认网络端口。 {@code 0} 表示 TaskManager 搜索空闲端口。
     *
     * The default network port the task manager expects to receive transfer envelopes on. The
     * {@code 0} means that the TaskManager searches for a free port.
     */
    @Documentation.Section({
        Documentation.Sections.COMMON_HOST_PORT,
        Documentation.Sections.ALL_TASK_MANAGER
    })
    public static final ConfigOption<Integer> DATA_PORT =
            key("taskmanager.data.port")
                    .defaultValue(0)
                    .withDescription(
                            "The task manager’s external port used for data exchange operations.");

    /** The local network port that the task manager listen at for data exchange. */
    // 任务管理器侦听数据交换的本地网络端口。
    public static final ConfigOption<Integer> DATA_BIND_PORT =
            key("taskmanager.data.bind-port")
                    .intType()
                    .noDefaultValue()
                    .withDescription(
                            "The task manager's bind port used for data exchange operations. If not configured, '"
                                    + DATA_PORT.key()
                                    + "' will be used.");

    /** Config parameter to override SSL support for taskmanager's data transport. */
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER)
    public static final ConfigOption<Boolean> DATA_SSL_ENABLED =
            key("taskmanager.data.ssl.enabled")
                    .defaultValue(true)
                    .withDescription(
                            "Enable SSL support for the taskmanager data transport. This is applicable only when the"
                                    + " global flag for internal SSL ("
                                    + SecurityOptions.SSL_INTERNAL_ENABLED.key()
                                    + ") is set to true");

    /**
     * 布尔标志，指示 shuffle 数据是否将被压缩以用于阻塞 shuffle 模式。
     *
     * Boolean flag indicating whether the shuffle data will be compressed for blocking shuffle
     * mode.
     *
     * <p>Note: Data is compressed per buffer and compression can incur extra CPU overhead so it is
     * more effective for IO bounded scenario when data compression ratio is high.
     */
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Boolean> BLOCKING_SHUFFLE_COMPRESSION_ENABLED =
            key("taskmanager.network.blocking-shuffle.compression.enabled")
                    .defaultValue(false)
                    .withDescription(
                            "Boolean flag indicating whether the shuffle data will be compressed "
                                    + "for blocking shuffle mode. Note that data is compressed per "
                                    + "buffer and compression can incur extra CPU overhead, so it "
                                    + "is more effective for IO bounded scenario when compression "
                                    + "ratio is high.");

    /** The codec to be used when compressing shuffle data. */
    // 压缩 shuffle 数据时要使用的编解码器。
    @Documentation.ExcludeFromDocumentation("Currently, LZ4 is the only legal option.")
    public static final ConfigOption<String> SHUFFLE_COMPRESSION_CODEC =
            key("taskmanager.network.compression.codec")
                    .defaultValue("LZ4")
                    .withDescription("The codec to be used when compressing shuffle data.");

    /**
     * 用于启用的布尔标志禁用有关入站出站网络队列长度的更详细指标。
     *
     * Boolean flag to enable/disable more detailed metrics about inbound/outbound network queue
     * lengths.
     */
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Boolean> NETWORK_DETAILED_METRICS =
            key("taskmanager.network.detailed-metrics")
                    .defaultValue(false)
                    .withDescription(
                            "Boolean flag to enable/disable more detailed metrics about inbound/outbound network queue lengths.");

    /**
     * Number of buffers used in the network stack. This defines the number of possible tasks and
     * shuffles.
     *
     * @deprecated use {@link TaskManagerOptions#NETWORK_MEMORY_FRACTION}, {@link
     *     TaskManagerOptions#NETWORK_MEMORY_MIN}, and {@link TaskManagerOptions#NETWORK_MEMORY_MAX}
     *     instead
     */
    @Deprecated
    public static final ConfigOption<Integer> NETWORK_NUM_BUFFERS =
            key("taskmanager.network.numberOfBuffers").defaultValue(2048);

    /**
     * Fraction of JVM memory to use for network buffers.
     *
     * @deprecated use {@link TaskManagerOptions#NETWORK_MEMORY_FRACTION} instead
     */
    @Deprecated
    public static final ConfigOption<Float> NETWORK_BUFFERS_MEMORY_FRACTION =
            key("taskmanager.network.memory.fraction")
                    .defaultValue(0.1f)
                    .withDescription(
                            "Fraction of JVM memory to use for network buffers. This determines how many streaming"
                                    + " data exchange channels a TaskManager can have at the same time and how well buffered the channels"
                                    + " are. If a job is rejected or you get a warning that the system has not enough buffers available,"
                                    + " increase this value or the min/max values below. Also note, that \"taskmanager.network.memory.min\""
                                    + "` and \"taskmanager.network.memory.max\" may override this fraction.");

    /**
     * Minimum memory size for network buffers.
     *
     * @deprecated use {@link TaskManagerOptions#NETWORK_MEMORY_MIN} instead
     */
    @Deprecated
    public static final ConfigOption<String> NETWORK_BUFFERS_MEMORY_MIN =
            key("taskmanager.network.memory.min")
                    .defaultValue("64mb")
                    .withDescription("Minimum memory size for network buffers.");

    /**
     * Maximum memory size for network buffers.
     *
     * @deprecated use {@link TaskManagerOptions#NETWORK_MEMORY_MAX} instead
     */
    @Deprecated
    public static final ConfigOption<String> NETWORK_BUFFERS_MEMORY_MAX =
            key("taskmanager.network.memory.max")
                    .defaultValue("1gb")
                    .withDescription("Maximum memory size for network buffers.");

    /**
     * 用于每个传出传入通道（子分区输入通道）的网络缓冲区数。
     *
     * <p>推理：1 个用于子分区中传输数据的缓冲区 + 1 个用于并行序列化的缓冲区。
     *
     * Number of network buffers to use for each outgoing/incoming channel (subpartition/input
     * channel).
     *
     * <p>Reasoning: 1 buffer for in-flight data in the subpartition + 1 buffer for parallel
     * serialization.
     */
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NETWORK_BUFFERS_PER_CHANNEL =
            key("taskmanager.network.memory.buffers-per-channel")
                    .defaultValue(2)
                    .withDescription(
                            "Number of exclusive network buffers to use for each outgoing/incoming channel (subpartition/inputchannel)"
                                    + " in the credit-based flow control model. It should be configured at least 2 for good performance."
                                    + " 1 buffer is for receiving in-flight data in the subpartition and 1 buffer is for parallel serialization.");

    /**
     * 用于每个传出传入门（结果分区输入门）的额外网络缓冲区的数量。
     *
     * Number of extra network buffers to use for each outgoing/incoming gate (result
     * partition/input gate).
     */
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NETWORK_EXTRA_BUFFERS_PER_GATE =
            key("taskmanager.network.memory.floating-buffers-per-gate")
                    .defaultValue(8)
                    .withDescription(
                            "Number of extra network buffers to use for each outgoing/incoming gate (result partition/input gate)."
                                    + " In credit-based flow control mode, this indicates how many floating credits are shared among all the input channels."
                                    + " The floating buffers are distributed based on backlog (real-time output buffers in the subpartition) feedback, and can"
                                    + " help relieve back-pressure caused by unbalanced data distribution among the subpartitions. This value should be"
                                    + " increased in case of higher round trip times between nodes and/or larger number of machines in the cluster.");

    /** Minimum number of network buffers required per sort-merge blocking result partition. */
    // 每个排序合并阻塞结果分区所需的最小网络缓冲区数。
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NETWORK_SORT_SHUFFLE_MIN_BUFFERS =
            key("taskmanager.network.sort-shuffle.min-buffers")
                    .intType()
                    .defaultValue(64)
                    .withDescription(
                            "Minimum number of network buffers required per sort-merge blocking "
                                    + "result partition. For production usage, it is suggested to "
                                    + "increase this config value to at least 2048 (64M memory if "
                                    + "the default 32K memory segment size is used) to improve the "
                                    + "data compression ratio and reduce the small network packets."
                                    + " Usually, several hundreds of megabytes memory is enough for"
                                    + " large scale batch jobs. Note: you may also need to increase"
                                    + " the size of total network memory to avoid the 'insufficient"
                                    + " number of network buffers' error if you are increasing this"
                                    + " config value.");

    /**
     * 在基于排序合并的阻塞 shuffle 和默认的基于散列的阻塞 shuffle 之间切换的并行度阈值。
     *
     * Parallelism threshold to switch between sort-merge based blocking shuffle and the default
     * hash-based blocking shuffle.
     */
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NETWORK_SORT_SHUFFLE_MIN_PARALLELISM =
            key("taskmanager.network.sort-shuffle.min-parallelism")
                    .intType()
                    .defaultValue(Integer.MAX_VALUE)
                    .withDescription(
                            String.format(
                                    "Parallelism threshold to switch between sort-merge blocking "
                                            + "shuffle and the default hash-based blocking shuffle,"
                                            + " which means for batch jobs of small parallelism, "
                                            + "the hash-based blocking shuffle will be used and for"
                                            + " batch jobs of large parallelism, the sort-merge one"
                                            + " will be used. Note: For production usage, if sort-"
                                            + "merge blocking shuffle is enabled, you may also need"
                                            + " to enable data compression by setting '%s' to true "
                                            + "and tune '%s' and '%s' for better performance.",
                                    BLOCKING_SHUFFLE_COMPRESSION_ENABLED.key(),
                                    NETWORK_SORT_SHUFFLE_MIN_BUFFERS.key(),
                                    // raw string key is used here to avoid interdependence, a test
                                    // is implemented to guard that when the target key is modified,
                                    // this raw value must be changed correspondingly
                                    "taskmanager.memory.framework.off-heap.batch-shuffle.size"));

    /** Number of max buffers can be used for each output subparition. */
    // 最大缓冲区数可用于每个输出子分区。
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NETWORK_MAX_BUFFERS_PER_CHANNEL =
            key("taskmanager.network.memory.max-buffers-per-channel")
                    .defaultValue(10)
                    .withDescription(
                            "Number of max buffers that can be used for each channel. If a channel exceeds the number of max"
                                    + " buffers, it will make the task become unavailable, cause the back pressure and block the data processing. This"
                                    + " might speed up checkpoint alignment by preventing excessive growth of the buffered in-flight data in"
                                    + " case of data skew and high number of configured floating buffers. This limit is not strictly guaranteed,"
                                    + " and can be ignored by things like flatMap operators, records spanning multiple buffers or single timer"
                                    + " producing large amount of data.");

    /** The timeout for requesting exclusive buffers for each channel. */
    // 为每个通道请求独占缓冲区的超时时间。
    @Documentation.ExcludeFromDocumentation(
            "This option is purely implementation related, and may be removed as the implementation changes.")
    public static final ConfigOption<Long> NETWORK_EXCLUSIVE_BUFFERS_REQUEST_TIMEOUT_MILLISECONDS =
            key("taskmanager.network.memory.exclusive-buffers-request-timeout-ms")
                    .defaultValue(30000L)
                    .withDescription(
                            "The timeout for requesting exclusive buffers for each channel. Since the number of maximum buffers and "
                                    + "the number of required buffers is not the same for local buffer pools, there may be deadlock cases that the upstream"
                                    + "tasks have occupied all the buffers and the downstream tasks are waiting for the exclusive buffers. The timeout breaks"
                                    + "the tie by failing the request of exclusive buffers and ask users to increase the number of total buffers.");

    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<String> NETWORK_BLOCKING_SHUFFLE_TYPE =
            key("taskmanager.network.blocking-shuffle.type")
                    .defaultValue("file")
                    .withDescription(
                            "The blocking shuffle type, either \"mmap\" or \"file\". The \"auto\" means selecting the property type automatically"
                                    + " based on system memory architecture (64 bit for mmap and 32 bit for file). Note that the memory usage of mmap is not accounted"
                                    + " by configured memory limits, but some resource frameworks like yarn would track this memory usage and kill the container once"
                                    + " memory exceeding some threshold. Also note that this option is experimental and might be changed future.");

    // ------------------------------------------------------------------------
    //  Netty Options
    // ------------------------------------------------------------------------

    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NUM_ARENAS =
            key("taskmanager.network.netty.num-arenas")
                    .defaultValue(-1)
                    .withDeprecatedKeys("taskmanager.net.num-arenas")
                    .withDescription("The number of Netty arenas.");

    // J: 服务端线程数
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NUM_THREADS_SERVER =
            key("taskmanager.network.netty.server.numThreads")
                    .defaultValue(-1)
                    .withDeprecatedKeys("taskmanager.net.server.numThreads")
                    .withDescription("The number of Netty server threads.");

    // J: 客户端线程数
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NUM_THREADS_CLIENT =
            key("taskmanager.network.netty.client.numThreads")
                    .defaultValue(-1)
                    .withDeprecatedKeys("taskmanager.net.client.numThreads")
                    .withDescription("The number of Netty client threads.");

    // J: 服务端积压
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> CONNECT_BACKLOG =
            key("taskmanager.network.netty.server.backlog")
                    .defaultValue(0) // default: 0 => Netty's default
                    .withDeprecatedKeys("taskmanager.net.server.backlog")
                    .withDescription("The netty server connection backlog.");

    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> CLIENT_CONNECT_TIMEOUT_SECONDS =
            key("taskmanager.network.netty.client.connectTimeoutSec")
                    .defaultValue(120) // default: 120s = 2min
                    .withDeprecatedKeys("taskmanager.net.client.connectTimeoutSec")
                    .withDescription("The Netty client connection timeout.");

    // J: 网络重试次数，目前仅用于建立输入输出通道连接
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NETWORK_RETRIES =
            key("taskmanager.network.retries")
                    .defaultValue(0)
                    .withDeprecatedKeys("taskmanager.network.retries")
                    .withDescription(
                            "The number of retry attempts for network communication."
                                    + " Currently it's only used for establishing input/output channel connections");

    // Netty 发送和接收缓冲区大小。这默认为系统缓冲区大小“+”(cat /proc/sys/net/ipv4/tcp_[rw]mem)，在现代 Linux 中为 4 MiB。
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> SEND_RECEIVE_BUFFER_SIZE =
            key("taskmanager.network.netty.sendReceiveBufferSize")
                    .defaultValue(0) // default: 0 => Netty's default
                    .withDeprecatedKeys("taskmanager.net.sendReceiveBufferSize")
                    .withDescription(
                            "The Netty send and receive buffer size. This defaults to the system buffer size"
                                    + " (cat /proc/sys/net/ipv4/tcp_[rw]mem) and is 4 MiB in modern Linux.");

    // Netty 传输类型，"nio" 或 "epoll"。 “auto”表示根据平台自动选择属性模式。请注意，"epoll" 模式可以获得
    // 更好的性能、更少的 GC 并具有更多高级功能，这些功能仅在现代 Linux 上可用。
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<String> TRANSPORT_TYPE =
            key("taskmanager.network.netty.transport")
                    .defaultValue("auto")
                    .withDeprecatedKeys("taskmanager.net.transport")
                    .withDescription(
                            "The Netty transport type, either \"nio\" or \"epoll\". The \"auto\" means selecting the property mode automatically"
                                    + " based on the platform. Note that the \"epoll\" mode can get better performance, less GC and have more advanced features which are"
                                    + " only available on modern Linux.");

    // ------------------------------------------------------------------------
    //  Partition Request Options
    // ------------------------------------------------------------------------

    /** Minimum backoff for partition requests of input channels. */
    // 输入通道分区请求的最小退避。
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NETWORK_REQUEST_BACKOFF_INITIAL =
            key("taskmanager.network.request-backoff.initial")
                    .defaultValue(100)
                    .withDeprecatedKeys("taskmanager.net.request-backoff.initial")
                    .withDescription(
                            "Minimum backoff in milliseconds for partition requests of input channels.");

    /** Maximum backoff for partition requests of input channels. */
    // 输入通道分区请求的最大退避。
    @Documentation.Section(Documentation.Sections.ALL_TASK_MANAGER_NETWORK)
    public static final ConfigOption<Integer> NETWORK_REQUEST_BACKOFF_MAX =
            key("taskmanager.network.request-backoff.max")
                    .defaultValue(10000)
                    .withDeprecatedKeys("taskmanager.net.request-backoff.max")
                    .withDescription(
                            "Maximum backoff in milliseconds for partition requests of input channels.");

    // ------------------------------------------------------------------------

    /** Not intended to be instantiated. */
    private NettyShuffleEnvironmentOptions() {}
}
