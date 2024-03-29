/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.api.common;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.Public;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.configuration.ConfigurationUtils;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.ExecutionOptions;
import org.apache.flink.configuration.MetricOptions;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.util.Preconditions;

import com.esotericsoftware.kryo.Serializer;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.flink.util.Preconditions.checkArgument;

/**
 * 定义程序执行行为的配置。允许定义(在其他选项中)以下设置:
 *
 *   - 程序的默认并行度，即对于所有没有直接定义特定值的函数，需要使用多少个并行任务。
 *   - 在执行失败的情况下重试的次数。
 *   - 执行重试之间的延迟。
 *   - 程序的{@link ExecutionMode}: 批处理或流水线。默认的执行模式是 {@link ExecutionMode# pipelining}
 *   - 启用或禁用 “闭包清理器”。闭包清理器对函数的实现进行预处理。如果它们是(匿名)内部类，它会删除对封闭类的未使用引用，以
 *       修复某些与序列化相关的问题，并减少闭包的大小。
 *   - 该配置允许注册类型和序列化器，以提高处理<i>泛型类型<i>和<i> pojo <i>的效率。这通常只在函数不仅返回在其签名中声明
 *       的类型，而且还返回这些类型的子类时才需要。
 *
 * A config to define the behavior of the program execution. It allows to define (among other
 * options) the following settings:
 *
 * <ul>
 *   <li>The default parallelism of the program, i.e., how many parallel tasks to use for all
 *       functions that do not define a specific value directly.
 *   <li>The number of retries in the case of failed executions.
 *   <li>The delay between execution retries.
 *   <li>The {@link ExecutionMode} of the program: Batch or Pipelined. The default execution mode is
 *       {@link ExecutionMode#PIPELINED}
 *   <li>Enabling or disabling the "closure cleaner". The closure cleaner pre-processes the
 *       implementations of functions. In case they are (anonymous) inner classes, it removes unused
 *       references to the enclosing class to fix certain serialization-related problems and to
 *       reduce the size of the closure.
 *   <li>The config allows to register types and serializers to increase the efficiency of handling
 *       <i>generic types</i> and <i>POJOs</i>. This is usually only needed when the functions
 *       return not only the types declared in their signature, but also subclasses of those types.
 * </ul>
 */
@Public
public class ExecutionConfig implements Serializable, Archiveable<ArchivedExecutionConfig> {

    private static final long serialVersionUID = 1L;

    /**
     * 用于并行度的常量，如果系统应该使用当前可用槽的数量。
     *
     * The constant to use for the parallelism, if the system should use the number of currently
     * available slots.
     */
    @Deprecated public static final int PARALLELISM_AUTO_MAX = Integer.MAX_VALUE;

    /**
     * 指示使用默认并行度的标志值。此值可用于将并行度重置回默认状态。
     *
     * J: 可配合自动调优使用
     *
     * The flag value indicating use of the default parallelism. This value can be used to reset the
     * parallelism back to the default state.
     */
    public static final int PARALLELISM_DEFAULT = -1;

    /**
     * 指示未知或未设置并行度的标志值。该值不是有效的并行度，并表示并行度应该保持不变。
     *
     * The flag value indicating an unknown or unset parallelism. This value is not a valid
     * parallelism and indicates that the parallelism should remain unchanged.
     */
    public static final int PARALLELISM_UNKNOWN = -2;

    private static final long DEFAULT_RESTART_DELAY = 10000L;

    // --------------------------------------------------------------------------------------------

    /** Defines how data exchange happens - batch or pipelined */
    private ExecutionMode executionMode = ExecutionMode.PIPELINED;

    private ClosureCleanerLevel closureCleanerLevel = ClosureCleanerLevel.RECURSIVE;

    private int parallelism = CoreOptions.DEFAULT_PARALLELISM.defaultValue();

    /**
     * The program wide maximum parallelism used for operators which haven't specified a maximum
     * parallelism. The maximum parallelism specifies the upper limit for dynamic scaling and the
     * number of key groups used for partitioned state.
     */
    private int maxParallelism = -1;

    /**
     * @deprecated Should no longer be used because it is subsumed by RestartStrategyConfiguration
     */
    @Deprecated private int numberOfExecutionRetries = -1;

    private boolean forceKryo = false;

    /** Flag to indicate whether generic types (through Kryo) are supported */
    // 标志，表示是否支持泛型类型(通过 Kryo)
    private boolean disableGenericTypes = false;

    private boolean enableAutoGeneratedUids = true;

    private boolean objectReuse = false;

    private boolean autoTypeRegistrationEnabled = true;

    private boolean forceAvro = false;
    private long autoWatermarkInterval = 200;

    /**
     * 将延迟跟踪标记从源发送到接收的间隔(以毫秒为单位)
     *
     * Interval in milliseconds for sending latency tracking marks from the sources to the sinks.
     */
    private long latencyTrackingInterval = MetricOptions.LATENCY_INTERVAL.defaultValue();

    private boolean isLatencyTrackingConfigured = false;

    /**
     * @deprecated Should no longer be used because it is subsumed by RestartStrategyConfiguration
     */
    @Deprecated private long executionRetryDelay = DEFAULT_RESTART_DELAY;

    private RestartStrategies.RestartStrategyConfiguration restartStrategyConfiguration =
            new RestartStrategies.FallbackRestartStrategyConfiguration();

    private long taskCancellationIntervalMillis = -1;

    /**
     * 超时，在此之后，正在进行的任务取消将导致致命的TaskManager错误，通常会杀死JVM。
     *
     * Timeout after which an ongoing task cancellation will lead to a fatal TaskManager error,
     * usually killing the JVM.
     */
    private long taskCancellationTimeoutMillis = -1;

    /**
     * 该标志定义状态快照数据是否使用压缩。默认值:假
     *
     * This flag defines if we use compression for the state snapshot data or not. Default: false
     */
    private boolean useSnapshotCompression = false;

    // ------------------------------- User code values --------------------------------------------

    // J: 全局配置参数
    private GlobalJobParameters globalJobParameters = new GlobalJobParameters();

    // Serializers and types registered with Kryo and the PojoSerializer
    // we store them in linked maps/sets to ensure they are registered in order in all kryo
    // instances.
    // 序列化器和类型注册到 Kryo 和 PojoSerializer 中，我们将它们存储在链接的映射集中，以确保它们在所有 Kryo 实例中是
    // 按顺序注册的。

    private LinkedHashMap<Class<?>, SerializableSerializer<?>> registeredTypesWithKryoSerializers =
            new LinkedHashMap<>();

    private LinkedHashMap<Class<?>, Class<? extends Serializer<?>>>
            registeredTypesWithKryoSerializerClasses = new LinkedHashMap<>();

    private LinkedHashMap<Class<?>, SerializableSerializer<?>> defaultKryoSerializers =
            new LinkedHashMap<>();

    private LinkedHashMap<Class<?>, Class<? extends Serializer<?>>> defaultKryoSerializerClasses =
            new LinkedHashMap<>();

    private LinkedHashSet<Class<?>> registeredKryoTypes = new LinkedHashSet<>();

    private LinkedHashSet<Class<?>> registeredPojoTypes = new LinkedHashSet<>();

    // --------------------------------------------------------------------------------------------

    /**
     * 使 ClosureCleaner。这会分析用户代码函数，并将未使用的字段设置为空。在大多数情况下，这将使闭包或匿名内部类可
     * 序列化，而这些类由于某些 Scala 或 Java 实现工件而不能序列化。用户代码必须是可序列化的，因为它需要被发送到工作节点。
     *
     * Enables the ClosureCleaner. This analyzes user code functions and sets fields to null that
     * are not used. This will in most cases make closures or anonymous inner classes serializable
     * that where not serializable due to some Scala or Java implementation artifact. User code must
     * be serializable because it needs to be sent to worker nodes.
     */
    public ExecutionConfig enableClosureCleaner() {
        this.closureCleanerLevel = ClosureCleanerLevel.RECURSIVE;
        return this;
    }

    /**
     * Disables the ClosureCleaner.
     *
     * @see #enableClosureCleaner()
     */
    public ExecutionConfig disableClosureCleaner() {
        this.closureCleanerLevel = ClosureCleanerLevel.NONE;
        return this;
    }

    /**
     * Returns whether the ClosureCleaner is enabled.
     *
     * @see #enableClosureCleaner()
     */
    public boolean isClosureCleanerEnabled() {
        return !(closureCleanerLevel == ClosureCleanerLevel.NONE);
    }

    /**
     * Configures the closure cleaner. Please see {@link ClosureCleanerLevel} for details on the
     * different settings.
     */
    public ExecutionConfig setClosureCleanerLevel(ClosureCleanerLevel level) {
        this.closureCleanerLevel = level;
        return this;
    }

    /** Returns the configured {@link ClosureCleanerLevel}. */
    public ClosureCleanerLevel getClosureCleanerLevel() {
        return closureCleanerLevel;
    }

    /**
     * 设置自动水印发送的时间间隔。水印在整个流媒体系统中被用来跟踪时间的进程。例如，它们用于基于时间的窗口。
     *
     * <p>设置“{@code 0}”的发送时间间隔，将使水印的周期性发送失效。
     *
     * Sets the interval of the automatic watermark emission. Watermarks are used throughout the
     * streaming system to keep track of the progress of time. They are used, for example, for time
     * based windowing.
     *
     * <p>Setting an interval of {@code 0} will disable periodic watermark emission.
     *
     * @param interval The interval between watermarks in milliseconds.
     */
    @PublicEvolving
    public ExecutionConfig setAutoWatermarkInterval(long interval) {
        Preconditions.checkArgument(interval >= 0, "Auto watermark interval must not be negative.");
        this.autoWatermarkInterval = interval;
        return this;
    }

    /**
     * Returns the interval of the automatic watermark emission.
     *
     * @see #setAutoWatermarkInterval(long)
     */
    @PublicEvolving
    public long getAutoWatermarkInterval() {
        return this.autoWatermarkInterval;
    }

    /**
     * 将延迟跟踪标记从源发送到接收的间隔。Flink 将以指定的间隔从源发送延迟跟踪标记。
     *
     * <p>设置跟踪间隔 <= 0禁用延迟跟踪。
     *
     * Interval for sending latency tracking marks from the sources to the sinks. Flink will send
     * latency tracking marks from the sources at the specified interval.
     *
     * <p>Setting a tracking interval <= 0 disables the latency tracking.
     *
     * @param interval Interval in milliseconds.
     */
    @PublicEvolving
    public ExecutionConfig setLatencyTrackingInterval(long interval) {
        this.latencyTrackingInterval = interval;
        this.isLatencyTrackingConfigured = true;
        return this;
    }

    /**
     * Returns the latency tracking interval.
     *
     * @return The latency tracking interval in milliseconds
     */
    @PublicEvolving
    public long getLatencyTrackingInterval() {
        return latencyTrackingInterval;
    }

    @Internal
    public boolean isLatencyTrackingConfigured() {
        return isLatencyTrackingConfigured;
    }

    /**
     * Gets the parallelism with which operation are executed by default. Operations can
     * individually override this value to use a specific parallelism.
     *
     * <p>Other operations may need to run with a different parallelism - for example calling a
     * reduce operation over the entire data set will involve an operation that runs with a
     * parallelism of one (the final reduce to the single result value).
     *
     * @return The parallelism used by operations, unless they override that value. This method
     *     returns {@link #PARALLELISM_DEFAULT} if the environment's default parallelism should be
     *     used.
     */
    public int getParallelism() {
        return parallelism;
    }

    /**
     * Sets the parallelism for operations executed through this environment. Setting a parallelism
     * of x here will cause all operators (such as join, map, reduce) to run with x parallel
     * instances.
     *
     * <p>This method overrides the default parallelism for this environment. The local execution
     * environment uses by default a value equal to the number of hardware contexts (CPU cores /
     * threads). When executing the program via the command line client from a JAR file, the default
     * parallelism is the one configured for that setup.
     *
     * @param parallelism The parallelism to use
     */
    public ExecutionConfig setParallelism(int parallelism) {
        if (parallelism != PARALLELISM_UNKNOWN) {
            if (parallelism < 1 && parallelism != PARALLELISM_DEFAULT) {
                throw new IllegalArgumentException(
                        "Parallelism must be at least one, or ExecutionConfig.PARALLELISM_DEFAULT (use system default).");
            }
            this.parallelism = parallelism;
        }
        return this;
    }

    /**
     * Gets the maximum degree of parallelism defined for the program.
     *
     * <p>The maximum degree of parallelism specifies the upper limit for dynamic scaling. It also
     * defines the number of key groups used for partitioned state.
     *
     * @return Maximum degree of parallelism
     */
    @PublicEvolving
    public int getMaxParallelism() {
        return maxParallelism;
    }

    /**
     * Sets the maximum degree of parallelism defined for the program.
     *
     * <p>The maximum degree of parallelism specifies the upper limit for dynamic scaling. It also
     * defines the number of key groups used for partitioned state.
     *
     * @param maxParallelism Maximum degree of parallelism to be used for the program.
     */
    @PublicEvolving
    public void setMaxParallelism(int maxParallelism) {
        checkArgument(maxParallelism > 0, "The maximum parallelism must be greater than 0.");
        this.maxParallelism = maxParallelism;
    }

    /**
     * Gets the interval (in milliseconds) between consecutive attempts to cancel a running task.
     */
    public long getTaskCancellationInterval() {
        return this.taskCancellationIntervalMillis;
    }

    /**
     * Sets the configuration parameter specifying the interval (in milliseconds) between
     * consecutive attempts to cancel a running task.
     *
     * @param interval the interval (in milliseconds).
     */
    public ExecutionConfig setTaskCancellationInterval(long interval) {
        this.taskCancellationIntervalMillis = interval;
        return this;
    }

    /**
     * Returns the timeout (in milliseconds) after which an ongoing task cancellation leads to a
     * fatal TaskManager error.
     *
     * <p>The value <code>0</code> means that the timeout is disabled. In this case a stuck
     * cancellation will not lead to a fatal error.
     */
    @PublicEvolving
    public long getTaskCancellationTimeout() {
        return this.taskCancellationTimeoutMillis;
    }

    /**
     * Sets the timeout (in milliseconds) after which an ongoing task cancellation is considered
     * failed, leading to a fatal TaskManager error.
     *
     * <p>The cluster default is configured via {@link
     * TaskManagerOptions#TASK_CANCELLATION_TIMEOUT}.
     *
     * <p>The value <code>0</code> disables the timeout. In this case a stuck cancellation will not
     * lead to a fatal error.
     *
     * @param timeout The task cancellation timeout (in milliseconds).
     */
    @PublicEvolving
    public ExecutionConfig setTaskCancellationTimeout(long timeout) {
        checkArgument(timeout >= 0, "Timeout needs to be >= 0.");
        this.taskCancellationTimeoutMillis = timeout;
        return this;
    }

    /**
     * 设置恢复时使用的重启策略。
     *
     * Sets the restart strategy to be used for recovery.
     *
     * <pre>{@code
     * ExecutionConfig config = env.getConfig();
     *
     * config.setRestartStrategy(RestartStrategies.fixedDelayRestart(
     * 	10,  // number of retries
     * 	1000 // delay between retries));
     * }</pre>
     *
     * @param restartStrategyConfiguration Configuration defining the restart strategy to use
     */
    @PublicEvolving
    public void setRestartStrategy(
            RestartStrategies.RestartStrategyConfiguration restartStrategyConfiguration) {
        this.restartStrategyConfiguration =
                Preconditions.checkNotNull(restartStrategyConfiguration);
    }

    /**
     * Returns the restart strategy which has been set for the current job.
     *
     * @return The specified restart configuration
     */
    @PublicEvolving
    @SuppressWarnings("deprecation")
    public RestartStrategies.RestartStrategyConfiguration getRestartStrategy() {
        if (restartStrategyConfiguration
                instanceof RestartStrategies.FallbackRestartStrategyConfiguration) {
            // support the old API calls by creating a restart strategy from them
            if (getNumberOfExecutionRetries() > 0 && getExecutionRetryDelay() >= 0) {
                return RestartStrategies.fixedDelayRestart(
                        getNumberOfExecutionRetries(), getExecutionRetryDelay());
            } else if (getNumberOfExecutionRetries() == 0) {
                return RestartStrategies.noRestart();
            } else {
                return restartStrategyConfiguration;
            }
        } else {
            return restartStrategyConfiguration;
        }
    }

    /**
     * Gets the number of times the system will try to re-execute failed tasks. A value of {@code
     * -1} indicates that the system default value (as defined in the configuration) should be used.
     *
     * @return The number of times the system will try to re-execute failed tasks.
     * @deprecated Should no longer be used because it is subsumed by RestartStrategyConfiguration
     */
    @Deprecated
    public int getNumberOfExecutionRetries() {
        return numberOfExecutionRetries;
    }

    /**
     * Returns the delay between execution retries.
     *
     * @return The delay between successive execution retries in milliseconds.
     * @deprecated Should no longer be used because it is subsumed by RestartStrategyConfiguration
     */
    @Deprecated
    public long getExecutionRetryDelay() {
        return executionRetryDelay;
    }

    /**
     * Sets the number of times that failed tasks are re-executed. A value of zero effectively
     * disables fault tolerance. A value of {@code -1} indicates that the system default value (as
     * defined in the configuration) should be used.
     *
     * @param numberOfExecutionRetries The number of times the system will try to re-execute failed
     *     tasks.
     * @return The current execution configuration
     * @deprecated This method will be replaced by {@link #setRestartStrategy}. The {@link
     *     RestartStrategies.FixedDelayRestartStrategyConfiguration} contains the number of
     *     execution retries.
     */
    @Deprecated
    public ExecutionConfig setNumberOfExecutionRetries(int numberOfExecutionRetries) {
        if (numberOfExecutionRetries < -1) {
            throw new IllegalArgumentException(
                    "The number of execution retries must be non-negative, or -1 (use system default)");
        }
        this.numberOfExecutionRetries = numberOfExecutionRetries;
        return this;
    }

    /**
     * Sets the delay between executions.
     *
     * @param executionRetryDelay The number of milliseconds the system will wait to retry.
     * @return The current execution configuration
     * @deprecated This method will be replaced by {@link #setRestartStrategy}. The {@link
     *     RestartStrategies.FixedDelayRestartStrategyConfiguration} contains the delay between
     *     successive execution attempts.
     */
    @Deprecated
    public ExecutionConfig setExecutionRetryDelay(long executionRetryDelay) {
        if (executionRetryDelay < 0) {
            throw new IllegalArgumentException("The delay between retries must be non-negative.");
        }
        this.executionRetryDelay = executionRetryDelay;
        return this;
    }

    /**
     * Sets the execution mode to execute the program. The execution mode defines whether data
     * exchanges are performed in a batch or on a pipelined manner.
     *
     * <p>The default execution mode is {@link ExecutionMode#PIPELINED}.
     *
     * @param executionMode The execution mode to use.
     */
    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    /**
     * Gets the execution mode used to execute the program. The execution mode defines whether data
     * exchanges are performed in a batch or on a pipelined manner.
     *
     * <p>The default execution mode is {@link ExecutionMode#PIPELINED}.
     *
     * @return The execution mode for the program.
     */
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    /**
     * This method is deprecated. It was used to set the {@link InputDependencyConstraint} utilized
     * by the old scheduler implementations which got removed as part of FLINK-20589. The current
     * implementation has no effect.
     *
     * @param ignored Ignored parameter.
     * @deprecated due to the deprecation of {@code InputDependencyConstraint}.
     */
    @PublicEvolving
    @Deprecated
    public void setDefaultInputDependencyConstraint(InputDependencyConstraint ignored) {}

    /**
     * This method is deprecated. It was used to return the {@link InputDependencyConstraint}
     * utilized by the old scheduler implementations. These implementations were removed as part of
     * FLINK-20589.
     *
     * @return The previous default constraint {@link InputDependencyConstraint#ANY}.
     * @deprecated due to the deprecation of {@code InputDependencyConstraint}.
     */
    @PublicEvolving
    @Deprecated
    public InputDependencyConstraint getDefaultInputDependencyConstraint() {
        return InputDependencyConstraint.ANY;
    }

    /**
     * Force TypeExtractor to use Kryo serializer for POJOS even though we could analyze as POJO. In
     * some cases this might be preferable. For example, when using interfaces with subclasses that
     * cannot be analyzed as POJO.
     */
    public void enableForceKryo() {
        forceKryo = true;
    }

    /** Disable use of Kryo serializer for all POJOs. */
    public void disableForceKryo() {
        forceKryo = false;
    }

    public boolean isForceKryoEnabled() {
        return forceKryo;
    }

    /**
     * 允许使用通过 Kryo 序列化的泛型类型。
     *
     * <p>默认启用泛型类型。
     *
     * Enables the use generic types which are serialized via Kryo.
     *
     * <p>Generic types are enabled by default.
     *
     * @see #disableGenericTypes()
     */
    public void enableGenericTypes() {
        disableGenericTypes = false;
    }

    /**
     * Disables the use of generic types (types that would be serialized via Kryo). If this option
     * is used, Flink will throw an {@code UnsupportedOperationException} whenever it encounters a
     * data type that would go through Kryo for serialization.
     *
     * <p>Disabling generic types can be helpful to eagerly find and eliminate the use of types that
     * would go through Kryo serialization during runtime. Rather than checking types individually,
     * using this option will throw exceptions eagerly in the places where generic types are used.
     *
     * <p><b>Important:</b> We recommend to use this option only during development and
     * pre-production phases, not during actual production use. The application program and/or the
     * input data may be such that new, previously unseen, types occur at some point. In that case,
     * setting this option would cause the program to fail.
     *
     * @see #enableGenericTypes()
     */
    public void disableGenericTypes() {
        disableGenericTypes = true;
    }

    /**
     * Checks whether generic types are supported. Generic types are types that go through Kryo
     * during serialization.
     *
     * <p>Generic types are enabled by default.
     *
     * @see #enableGenericTypes()
     * @see #disableGenericTypes()
     */
    public boolean hasGenericTypesDisabled() {
        return disableGenericTypes;
    }

    /**
     * Enables the Flink runtime to auto-generate UID's for operators.
     *
     * @see #disableAutoGeneratedUIDs()
     */
    public void enableAutoGeneratedUIDs() {
        enableAutoGeneratedUids = true;
    }

    /**
     * Disables auto-generated UIDs. Forces users to manually specify UIDs on DataStream
     * applications.
     *
     * <p>It is highly recommended that users specify UIDs before deploying to production since they
     * are used to match state in savepoints to operators in a job. Because auto-generated ID's are
     * likely to change when modifying a job, specifying custom IDs allow an application to evolve
     * overtime without discarding state.
     */
    public void disableAutoGeneratedUIDs() {
        enableAutoGeneratedUids = false;
    }

    /**
     * Checks whether auto generated UIDs are supported.
     *
     * <p>Auto generated UIDs are enabled by default.
     *
     * @see #enableAutoGeneratedUIDs()
     * @see #disableAutoGeneratedUIDs()
     */
    public boolean hasAutoGeneratedUIDsEnabled() {
        return enableAutoGeneratedUids;
    }

    /**
     * 强制 Flink 为 pojo 使用 Apache Avro 序列化器。
     *
     * <p><b>重要:<b>确保包含<i>flink-avro<i>模块。
     *
     * Forces Flink to use the Apache Avro serializer for POJOs.
     *
     * <p><b>Important:</b> Make sure to include the <i>flink-avro</i> module.
     */
    public void enableForceAvro() {
        forceAvro = true;
    }

    /** Disables the Apache Avro serializer as the forced serializer for POJOs. */
    public void disableForceAvro() {
        forceAvro = false;
    }

    /** Returns whether the Apache Avro is the default serializer for POJOs. */
    public boolean isForceAvroEnabled() {
        return forceAvro;
    }

    /**
     * 允许重用Flink内部用于反序列化和向用户代码函数传递数据的对象。请记住，当操作的用户代码函数不知道这种行为时，这
     * 可能会导致错误。
     *
     * Enables reusing objects that Flink internally uses for deserialization and passing data to
     * user-code functions. Keep in mind that this can lead to bugs when the user-code function of
     * an operation is not aware of this behaviour.
     */
    public ExecutionConfig enableObjectReuse() {
        objectReuse = true;
        return this;
    }

    /**
     * Disables reusing objects that Flink internally uses for deserialization and passing data to
     * user-code functions. @see #enableObjectReuse()
     */
    public ExecutionConfig disableObjectReuse() {
        objectReuse = false;
        return this;
    }

    /** Returns whether object reuse has been enabled or disabled. @see #enableObjectReuse() */
    public boolean isObjectReuseEnabled() {
        return objectReuse;
    }

    public GlobalJobParameters getGlobalJobParameters() {
        return globalJobParameters;
    }

    /**
     * 注册一个自定义、可序列化的用户配置对象。
     *
     * Register a custom, serializable user configuration object.
     *
     * @param globalJobParameters Custom user configuration object
     */
    public void setGlobalJobParameters(GlobalJobParameters globalJobParameters) {
        Preconditions.checkNotNull(globalJobParameters, "globalJobParameters shouldn't be null");
        this.globalJobParameters = globalJobParameters;
    }

    // --------------------------------------------------------------------------------------------
    //  Registry for types and serializers
    // --------------------------------------------------------------------------------------------

    /**
     * 向运行时添加一个新的 Kryo 默认序列化器。
     *
     * <p>请注意，序列化器实例必须是可序列化的(正如java.io. serializable定义的那样)，因为它可能通过java序列化被分发到工作节点。
     *
     * Adds a new Kryo default serializer to the Runtime.
     *
     * <p>Note that the serializer instance must be serializable (as defined by
     * java.io.Serializable), because it may be distributed to the worker nodes by java
     * serialization.
     *
     * @param type The class of the types serialized with the given serializer.
     * @param serializer The serializer to use.
     */
    public <T extends Serializer<?> & Serializable> void addDefaultKryoSerializer(
            Class<?> type, T serializer) {
        if (type == null || serializer == null) {
            throw new NullPointerException("Cannot register null class or serializer.");
        }

        defaultKryoSerializers.put(type, new SerializableSerializer<>(serializer));
    }

    /**
     * Adds a new Kryo default serializer to the Runtime.
     *
     * @param type The class of the types serialized with the given serializer.
     * @param serializerClass The class of the serializer to use.
     */
    public void addDefaultKryoSerializer(
            Class<?> type, Class<? extends Serializer<?>> serializerClass) {
        if (type == null || serializerClass == null) {
            throw new NullPointerException("Cannot register null class or serializer.");
        }
        defaultKryoSerializerClasses.put(type, serializerClass);
    }

    /**
     * Registers the given type with a Kryo Serializer.
     *
     * <p>Note that the serializer instance must be serializable (as defined by
     * java.io.Serializable), because it may be distributed to the worker nodes by java
     * serialization.
     *
     * @param type The class of the types serialized with the given serializer.
     * @param serializer The serializer to use.
     */
    public <T extends Serializer<?> & Serializable> void registerTypeWithKryoSerializer(
            Class<?> type, T serializer) {
        if (type == null || serializer == null) {
            throw new NullPointerException("Cannot register null class or serializer.");
        }

        registeredTypesWithKryoSerializers.put(type, new SerializableSerializer<>(serializer));
    }

    /**
     * Registers the given Serializer via its class as a serializer for the given type at the
     * KryoSerializer
     *
     * @param type The class of the types serialized with the given serializer.
     * @param serializerClass The class of the serializer to use.
     */
    @SuppressWarnings("rawtypes")
    public void registerTypeWithKryoSerializer(
            Class<?> type, Class<? extends Serializer> serializerClass) {
        if (type == null || serializerClass == null) {
            throw new NullPointerException("Cannot register null class or serializer.");
        }

        @SuppressWarnings("unchecked")
        Class<? extends Serializer<?>> castedSerializerClass =
                (Class<? extends Serializer<?>>) serializerClass;
        registeredTypesWithKryoSerializerClasses.put(type, castedSerializerClass);
    }

    /**
     * 向序列化堆栈注册给定的类型。如果该类型最终被序列化为POJO，则该类型被注册到POJO序列化器中。如果类型最终用
     * Kryo序列化，那么它将在Kryo注册，以确保只写入标记。
     *
     * Registers the given type with the serialization stack. If the type is eventually serialized
     * as a POJO, then the type is registered with the POJO serializer. If the type ends up being
     * serialized with Kryo, then it will be registered at Kryo to make sure that only tags are
     * written.
     *
     * @param type The class of the type to register.
     */
    public void registerPojoType(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("Cannot register null type class.");
        }
        if (!registeredPojoTypes.contains(type)) {
            registeredPojoTypes.add(type);
        }
    }

    /**
     * Registers the given type with the serialization stack. If the type is eventually serialized
     * as a POJO, then the type is registered with the POJO serializer. If the type ends up being
     * serialized with Kryo, then it will be registered at Kryo to make sure that only tags are
     * written.
     *
     * @param type The class of the type to register.
     */
    public void registerKryoType(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("Cannot register null type class.");
        }
        registeredKryoTypes.add(type);
    }

    /** Returns the registered types with Kryo Serializers. */
    public LinkedHashMap<Class<?>, SerializableSerializer<?>>
            getRegisteredTypesWithKryoSerializers() {
        return registeredTypesWithKryoSerializers;
    }

    /** Returns the registered types with their Kryo Serializer classes. */
    public LinkedHashMap<Class<?>, Class<? extends Serializer<?>>>
            getRegisteredTypesWithKryoSerializerClasses() {
        return registeredTypesWithKryoSerializerClasses;
    }

    /** Returns the registered default Kryo Serializers. */
    public LinkedHashMap<Class<?>, SerializableSerializer<?>> getDefaultKryoSerializers() {
        return defaultKryoSerializers;
    }

    /** Returns the registered default Kryo Serializer classes. */
    public LinkedHashMap<Class<?>, Class<? extends Serializer<?>>>
            getDefaultKryoSerializerClasses() {
        return defaultKryoSerializerClasses;
    }

    /** Returns the registered Kryo types. */
    public LinkedHashSet<Class<?>> getRegisteredKryoTypes() {
        if (isForceKryoEnabled()) {
            // if we force kryo, we must also return all the types that
            // were previously only registered as POJO
            LinkedHashSet<Class<?>> result = new LinkedHashSet<>();
            result.addAll(registeredKryoTypes);
            for (Class<?> t : registeredPojoTypes) {
                if (!result.contains(t)) {
                    result.add(t);
                }
            }
            return result;
        } else {
            return registeredKryoTypes;
        }
    }

    /** Returns the registered POJO types. */
    public LinkedHashSet<Class<?>> getRegisteredPojoTypes() {
        return registeredPojoTypes;
    }

    public boolean isAutoTypeRegistrationDisabled() {
        return !autoTypeRegistrationEnabled;
    }

    /**
     * Control whether Flink is automatically registering all types in the user programs with Kryo.
     */
    public void disableAutoTypeRegistration() {
        this.autoTypeRegistrationEnabled = false;
    }

    public boolean isUseSnapshotCompression() {
        return useSnapshotCompression;
    }

    public void setUseSnapshotCompression(boolean useSnapshotCompression) {
        this.useSnapshotCompression = useSnapshotCompression;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ExecutionConfig) {
            ExecutionConfig other = (ExecutionConfig) obj;

            return other.canEqual(this)
                    && Objects.equals(executionMode, other.executionMode)
                    && closureCleanerLevel == other.closureCleanerLevel
                    && parallelism == other.parallelism
                    && ((restartStrategyConfiguration == null
                                    && other.restartStrategyConfiguration == null)
                            || (null != restartStrategyConfiguration
                                    && restartStrategyConfiguration.equals(
                                            other.restartStrategyConfiguration)))
                    && forceKryo == other.forceKryo
                    && disableGenericTypes == other.disableGenericTypes
                    && objectReuse == other.objectReuse
                    && autoTypeRegistrationEnabled == other.autoTypeRegistrationEnabled
                    && forceAvro == other.forceAvro
                    && Objects.equals(globalJobParameters, other.globalJobParameters)
                    && autoWatermarkInterval == other.autoWatermarkInterval
                    && registeredTypesWithKryoSerializerClasses.equals(
                            other.registeredTypesWithKryoSerializerClasses)
                    && defaultKryoSerializerClasses.equals(other.defaultKryoSerializerClasses)
                    && registeredKryoTypes.equals(other.registeredKryoTypes)
                    && registeredPojoTypes.equals(other.registeredPojoTypes)
                    && taskCancellationIntervalMillis == other.taskCancellationIntervalMillis
                    && useSnapshotCompression == other.useSnapshotCompression;

        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                executionMode,
                closureCleanerLevel,
                parallelism,
                restartStrategyConfiguration,
                forceKryo,
                disableGenericTypes,
                objectReuse,
                autoTypeRegistrationEnabled,
                forceAvro,
                globalJobParameters,
                autoWatermarkInterval,
                registeredTypesWithKryoSerializerClasses,
                defaultKryoSerializerClasses,
                registeredKryoTypes,
                registeredPojoTypes,
                taskCancellationIntervalMillis,
                useSnapshotCompression);
    }

    @Override
    public String toString() {
        return "ExecutionConfig{"
                + "executionMode="
                + executionMode
                + ", closureCleanerLevel="
                + closureCleanerLevel
                + ", parallelism="
                + parallelism
                + ", maxParallelism="
                + maxParallelism
                + ", numberOfExecutionRetries="
                + numberOfExecutionRetries
                + ", forceKryo="
                + forceKryo
                + ", disableGenericTypes="
                + disableGenericTypes
                + ", enableAutoGeneratedUids="
                + enableAutoGeneratedUids
                + ", objectReuse="
                + objectReuse
                + ", autoTypeRegistrationEnabled="
                + autoTypeRegistrationEnabled
                + ", forceAvro="
                + forceAvro
                + ", autoWatermarkInterval="
                + autoWatermarkInterval
                + ", latencyTrackingInterval="
                + latencyTrackingInterval
                + ", isLatencyTrackingConfigured="
                + isLatencyTrackingConfigured
                + ", executionRetryDelay="
                + executionRetryDelay
                + ", restartStrategyConfiguration="
                + restartStrategyConfiguration
                + ", taskCancellationIntervalMillis="
                + taskCancellationIntervalMillis
                + ", taskCancellationTimeoutMillis="
                + taskCancellationTimeoutMillis
                + ", useSnapshotCompression="
                + useSnapshotCompression
                + ", globalJobParameters="
                + globalJobParameters
                + ", registeredTypesWithKryoSerializers="
                + registeredTypesWithKryoSerializers
                + ", registeredTypesWithKryoSerializerClasses="
                + registeredTypesWithKryoSerializerClasses
                + ", defaultKryoSerializers="
                + defaultKryoSerializers
                + ", defaultKryoSerializerClasses="
                + defaultKryoSerializerClasses
                + ", registeredKryoTypes="
                + registeredKryoTypes
                + ", registeredPojoTypes="
                + registeredPojoTypes
                + '}';
    }

    public boolean canEqual(Object obj) {
        return obj instanceof ExecutionConfig;
    }

    @Override
    @Internal
    public ArchivedExecutionConfig archive() {
        return new ArchivedExecutionConfig(this);
    }

    // ------------------------------ Utilities  ----------------------------------

    public static class SerializableSerializer<T extends Serializer<?> & Serializable>
            implements Serializable {
        private static final long serialVersionUID = 4687893502781067189L;

        private T serializer;

        public SerializableSerializer(T serializer) {
            this.serializer = serializer;
        }

        public T getSerializer() {
            return serializer;
        }
    }

    /**
     * 在执行配置处注册的自定义用户配置对象的抽象类。
     *
     * Abstract class for a custom user configuration object registered at the execution config.
     *
     * <p>This user config is accessible at runtime through
     * getRuntimeContext().getExecutionConfig().GlobalJobParameters()
     */
    public static class GlobalJobParameters implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Convert UserConfig into a {@code Map<String, String>} representation. This can be used by
         * the runtime, for example for presenting the user config in the web frontend.
         *
         * @return Key/Value representation of the UserConfig
         */
        public Map<String, String> toMap() {
            return Collections.emptyMap();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash();
        }
    }

    /** Configuration settings for the closure cleaner. */
    // 闭合清洁器的配置设置。
    public enum ClosureCleanerLevel {
        /** Disable the closure cleaner completely. */
        NONE,

        /** Clean only the top-level class without recursing into fields. */
        TOP_LEVEL,

        /** Clean all the fields recursively. */
        RECURSIVE
    }

    /**
     * Sets all relevant options contained in the {@link ReadableConfig} such as e.g. {@link
     * PipelineOptions#CLOSURE_CLEANER_LEVEL}.
     *
     * <p>It will change the value of a setting only if a corresponding option was set in the {@code
     * configuration}. If a key is not present, the current value of a field will remain untouched.
     *
     * @param configuration a configuration to read the values from
     * @param classLoader a class loader to use when loading classes
     */
    public void configure(ReadableConfig configuration, ClassLoader classLoader) {
        configuration
                .getOptional(PipelineOptions.AUTO_TYPE_REGISTRATION)
                .ifPresent(b -> this.autoTypeRegistrationEnabled = b);
        configuration
                .getOptional(PipelineOptions.AUTO_GENERATE_UIDS)
                .ifPresent(b -> this.enableAutoGeneratedUids = b);
        configuration
                .getOptional(PipelineOptions.AUTO_WATERMARK_INTERVAL)
                .ifPresent(i -> this.setAutoWatermarkInterval(i.toMillis()));
        configuration
                .getOptional(PipelineOptions.CLOSURE_CLEANER_LEVEL)
                .ifPresent(this::setClosureCleanerLevel);
        configuration.getOptional(PipelineOptions.FORCE_AVRO).ifPresent(b -> this.forceAvro = b);
        configuration
                .getOptional(PipelineOptions.GENERIC_TYPES)
                .ifPresent(b -> this.disableGenericTypes = !b);
        configuration.getOptional(PipelineOptions.FORCE_KRYO).ifPresent(b -> this.forceKryo = b);
        configuration
                .getOptional(PipelineOptions.GLOBAL_JOB_PARAMETERS)
                .<GlobalJobParameters>map(MapBasedJobParameters::new)
                .ifPresent(this::setGlobalJobParameters);

        // 延迟统计只读
        configuration
                .getOptional(MetricOptions.LATENCY_INTERVAL)
                .ifPresent(this::setLatencyTrackingInterval);

        configuration
                .getOptional(PipelineOptions.MAX_PARALLELISM)
                .ifPresent(this::setMaxParallelism);
        configuration.getOptional(CoreOptions.DEFAULT_PARALLELISM).ifPresent(this::setParallelism);
        configuration
                .getOptional(PipelineOptions.OBJECT_REUSE)
                .ifPresent(o -> this.objectReuse = o);
        configuration
                .getOptional(TaskManagerOptions.TASK_CANCELLATION_INTERVAL)
                .ifPresent(this::setTaskCancellationInterval);
        configuration
                .getOptional(TaskManagerOptions.TASK_CANCELLATION_TIMEOUT)
                .ifPresent(this::setTaskCancellationTimeout);
        configuration
                .getOptional(ExecutionOptions.SNAPSHOT_COMPRESSION)
                .ifPresent(this::setUseSnapshotCompression);
        RestartStrategies.fromConfiguration(configuration).ifPresent(this::setRestartStrategy);
        configuration
                .getOptional(PipelineOptions.KRYO_DEFAULT_SERIALIZERS)
                .map(s -> parseKryoSerializersWithExceptionHandling(classLoader, s))
                .ifPresent(s -> this.defaultKryoSerializerClasses = s);

        configuration
                .getOptional(PipelineOptions.POJO_REGISTERED_CLASSES)
                .map(c -> loadClasses(c, classLoader, "Could not load pojo type to be registered."))
                .ifPresent(c -> this.registeredPojoTypes = c);

        configuration
                .getOptional(PipelineOptions.KRYO_REGISTERED_CLASSES)
                .map(c -> loadClasses(c, classLoader, "Could not load kryo type to be registered."))
                .ifPresent(c -> this.registeredKryoTypes = c);
    }

    private LinkedHashSet<Class<?>> loadClasses(
            List<String> classNames, ClassLoader classLoader, String errorMessage) {
        return classNames.stream()
                .map(name -> this.<Class<?>>loadClass(name, classLoader, errorMessage))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private LinkedHashMap<Class<?>, Class<? extends Serializer<?>>>
            parseKryoSerializersWithExceptionHandling(
                    ClassLoader classLoader, List<String> kryoSerializers) {
        try {
            return parseKryoSerializers(classLoader, kryoSerializers);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Could not configure kryo serializers from %s. The expected format is:"
                                    + "'class:<fully qualified class name>,serializer:<fully qualified serializer name>;...",
                            kryoSerializers),
                    e);
        }
    }

    private LinkedHashMap<Class<?>, Class<? extends Serializer<?>>> parseKryoSerializers(
            ClassLoader classLoader, List<String> kryoSerializers) {
        return kryoSerializers.stream()
                .map(ConfigurationUtils::parseMap)
                .collect(
                        Collectors.toMap(
                                m ->
                                        loadClass(
                                                m.get("class"),
                                                classLoader,
                                                "Could not load class for kryo serialization"),
                                m ->
                                        loadClass(
                                                m.get("serializer"),
                                                classLoader,
                                                "Could not load serializer's class"),
                                (m1, m2) -> {
                                    throw new IllegalArgumentException(
                                            "Duplicated serializer for class: " + m1);
                                },
                                LinkedHashMap::new));
    }

    @SuppressWarnings("unchecked")
    private <T extends Class> T loadClass(
            String className, ClassLoader classLoader, String errorMessage) {
        try {
            return (T) Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    private static class MapBasedJobParameters extends GlobalJobParameters {
        private final Map<String, String> properties;

        private MapBasedJobParameters(Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        public Map<String, String> toMap() {
            return properties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GlobalJobParameters)) {
                return false;
            }
            GlobalJobParameters that = (GlobalJobParameters) o;
            return Objects.equals(properties, that.toMap());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), properties);
        }
    }
}
