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

package org.apache.flink.table.api;

import org.apache.flink.annotation.Experimental;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.table.api.config.ExecutionConfigOptions;
import org.apache.flink.table.api.config.OptimizerConfigOptions;
import org.apache.flink.table.api.config.TableConfigOptions;
import org.apache.flink.util.Preconditions;

import java.math.MathContext;
import java.time.Duration;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static java.time.ZoneId.SHORT_IDS;

/**
 * 配置当前{@link TableEnvironment}会话，以调整表和SQL API程序。
 *
 * <p>对于常见或重要的配置选项，这个类提供了带有详细内联文档的getter和setter方法。
 *
 * <p>对于更高级的配置，用户可以通过{@link #getConfiguration()}直接访问底层的键值映射。目前，只有Blink规划器支持
 *   键值选项。用户还可以通过此对象配置底层执行参数。如。
 *
 * <pre>{@code
 * tEnv.getConfig().addConfiguration(
 *          new Configuration()
 *              .set(CoreOptions.DEFAULT_PARALLELISM, 128)
 *              .set(PipelineOptions.AUTO_WATERMARK_INTERVAL, Duration.ofMillis(800))
 *              .set(ExecutionCheckpointingOptions.CHECKPOINTING_INTERVAL, Duration.ofSeconds(30))
 *      );
 * }</pre>
 *
 * <p>注:由于在执行操作时读取选项的时间不同，所以建议在表环境实例化后尽早设置配置选项。
 *
 * Configuration for the current {@link TableEnvironment} session to adjust Table & SQL API
 * programs.
 *
 * <p>For common or important configuration options, this class provides getters and setters methods
 * with detailed inline documentation.
 *
 * <p>For more advanced configuration, users can directly access the underlying key-value map via
 * {@link #getConfiguration()}. Currently, key-value options are only supported for the Blink
 * planner. Users can configure also underlying execution parameters via this object. E.g.
 *
 * <pre>{@code
 * tEnv.getConfig().addConfiguration(
 *          new Configuration()
 *              .set(CoreOptions.DEFAULT_PARALLELISM, 128)
 *              .set(PipelineOptions.AUTO_WATERMARK_INTERVAL, Duration.ofMillis(800))
 *              .set(ExecutionCheckpointingOptions.CHECKPOINTING_INTERVAL, Duration.ofSeconds(30))
 *      );
 * }</pre>
 *
 * <p>Note: Because options are read at different point in time when performing operations, it is
 * recommended to set configuration options early after instantiating a table environment.
 *
 * @see ExecutionConfigOptions
 * @see OptimizerConfigOptions
 */
@PublicEvolving
public class TableConfig {
    /** Defines if all fields need to be checked for NULL first. */
    // 定义是否需要首先检查所有字段是否为 NULL
    private Boolean nullCheck = true;

    /** Defines the configuration of Planner for Table API and SQL queries. */
    // 为表API和SQL查询定义Planner的配置。
    private PlannerConfig plannerConfig = PlannerConfig.EMPTY_CONFIG;

    /**
     * 定义十进制除法计算的默认上下文。我们使用Scala的默认MathContext.DECIMAL128
     *
     * Defines the default context for decimal division calculation. We use Scala's default
     * MathContext.DECIMAL128.
     */
    private MathContext decimalContext = MathContext.DECIMAL128;

    /** A configuration object to hold all key/value configuration. */
    // 一个配置对象，用来保存所有的键值配置
    private final Configuration configuration = new Configuration();

    /** Gives direct access to the underlying key-value map for advanced configuration. */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * 将给定的键值配置添加到底层配置。它覆盖现有的键。
     *
     * Adds the given key-value configuration to the underlying configuration. It overwrites
     * existing keys.
     *
     * @param configuration key-value configuration to be added
     */
    public void addConfiguration(Configuration configuration) {
        Preconditions.checkNotNull(configuration);
        this.configuration.addAll(configuration);
    }

    /** Returns the current SQL dialect. */
    public SqlDialect getSqlDialect() {
        return SqlDialect.valueOf(
                getConfiguration().getString(TableConfigOptions.TABLE_SQL_DIALECT).toUpperCase());
    }

    /** Sets the current SQL dialect to parse a SQL query. Flink's SQL behavior by default. */
    public void setSqlDialect(SqlDialect sqlDialect) {
        getConfiguration()
                .setString(TableConfigOptions.TABLE_SQL_DIALECT, sqlDialect.name().toLowerCase());
    }

    /**
     * 返回当前会话时区id。它用于从{@code TIMESTAMP WITH LOCAL TIME ZONE}转换为
     * {@code TIMESTAMP WITH LOCAL TIME ZONE}。更多细节见{@link #setLocalTimeZone(ZoneId)}。
     *
     * Returns the current session time zone id. It is used when converting to/from {@code TIMESTAMP
     * WITH LOCAL TIME ZONE}. See {@link #setLocalTimeZone(ZoneId)} for more details.
     *
     * @see org.apache.flink.table.types.logical.LocalZonedTimestampType
     */
    public ZoneId getLocalTimeZone() {
        String zone = configuration.getString(TableConfigOptions.LOCAL_TIME_ZONE);
        validateTimeZone(zone);
        return TableConfigOptions.LOCAL_TIME_ZONE.defaultValue().equals(zone)
                ? ZoneId.systemDefault()
                : ZoneId.of(zone);
    }

    /**
     * 设置当前会话时区id。当从{@link DataTypes#TIMESTAMP_WITH_LOCAL_TIME_ZONE()}转换到
     * {@link DataTypes#TIMESTAMP_WITH_LOCAL_TIME_ZONE()}时使用。在内部，带有本地时区的时间戳总是用UTC
     * 时区表示。然而，当转换为不包含时区的数据类型时(例如:TIMESTAMP, time，或者简单的STRING)，会话时区会在转换
     * 期间被使用。
     *
     * Sets the current session time zone id. It is used when converting to/from {@link
     * DataTypes#TIMESTAMP_WITH_LOCAL_TIME_ZONE()}. Internally, timestamps with local time zone are
     * always represented in the UTC time zone. However, when converting to data types that don't
     * include a time zone (e.g. TIMESTAMP, TIME, or simply STRING), the session time zone is used
     * during conversion.
     *
     * <p>Example:
     *
     * <pre>{@code
     * TableEnvironment tEnv = ...
     * TableConfig config = tEnv.getConfig
     * config.setLocalTimeZone(ZoneOffset.ofHours(2));
     * tEnv("CREATE TABLE testTable (id BIGINT, tmstmp TIMESTAMP WITH LOCAL TIME ZONE)");
     * tEnv("INSERT INTO testTable VALUES ((1, '2000-01-01 2:00:00'), (2, TIMESTAMP '2000-01-01 2:00:00'))");
     * tEnv("SELECT * FROM testTable"); // query with local time zone set to UTC+2
     * }</pre>
     *
     * <p>should produce:
     *
     * <pre>
     * =============================
     *    id   |       tmstmp
     * =============================
     *    1    | 2000-01-01 2:00:00'
     *    2    | 2000-01-01 2:00:00'
     * </pre>
     *
     * <p>If we change the local time zone and query the same table:
     *
     * <pre>{@code
     * config.setLocalTimeZone(ZoneOffset.ofHours(0));
     * tEnv("SELECT * FROM testTable"); // query with local time zone set to UTC+0
     * }</pre>
     *
     * <p>we should get:
     *
     * <pre>
     * =============================
     *    id   |       tmstmp
     * =============================
     *    1    | 2000-01-01 0:00:00'
     *    2    | 2000-01-01 0:00:00'
     * </pre>
     *
     * @see org.apache.flink.table.types.logical.LocalZonedTimestampType
     */
    public void setLocalTimeZone(ZoneId zoneId) {
        validateTimeZone(zoneId.toString());
        configuration.setString(TableConfigOptions.LOCAL_TIME_ZONE, zoneId.toString());
    }

    /** Validates user configured time zone. */
    // 验证用户配置的时区。
    private void validateTimeZone(String zone) {
        final String zoneId = zone.toUpperCase();
        if (zoneId.startsWith("UTC+")
                || zoneId.startsWith("UTC-")
                || SHORT_IDS.containsKey(zoneId)) {
            throw new IllegalArgumentException(
                    String.format(
                            "The supported Zone ID is either a full name such as 'America/Los_Angeles',"
                                    + " or a custom timezone id such as 'GMT-08:00', but configured Zone ID is '%s'.",
                            zone));
        }
    }

    /** Returns the NULL check. If enabled, all fields need to be checked for NULL first. */
    // 返回NULL检查。如果启用，首先需要检查所有字段是否为NULL。
    public Boolean getNullCheck() {
        return nullCheck;
    }

    /** Sets the NULL check. If enabled, all fields need to be checked for NULL first. */
    public void setNullCheck(Boolean nullCheck) {
        this.nullCheck = Preconditions.checkNotNull(nullCheck);
    }

    /** Returns the current configuration of Planner for Table API and SQL queries. */
    // 返回表API和SQL查询的Planner的当前配置。
    public PlannerConfig getPlannerConfig() {
        return plannerConfig;
    }

    /**
     * Sets the configuration of Planner for Table API and SQL queries. Changing the configuration
     * has no effect after the first query has been defined.
     */
    public void setPlannerConfig(PlannerConfig plannerConfig) {
        this.plannerConfig = Preconditions.checkNotNull(plannerConfig);
    }

    /**
     * 返回小数除法计算的默认上下文。{@link java.math。默认MathContext # DECIMAL128}。
     *
     * Returns the default context for decimal division calculation. {@link
     * java.math.MathContext#DECIMAL128} by default.
     */
    public MathContext getDecimalContext() {
        return decimalContext;
    }

    /**
     * Sets the default context for decimal division calculation. {@link
     * java.math.MathContext#DECIMAL128} by default.
     */
    public void setDecimalContext(MathContext decimalContext) {
        this.decimalContext = Preconditions.checkNotNull(decimalContext);
    }

    /**
     * 返回将生成的代码拆分为子函数调用的当前阈值。Java的最大方法长度为64kb。如果需要，这个设置允许更细的粒度。
     * 默认是64000。
     *
     * Returns the current threshold where generated code will be split into sub-function calls.
     * Java has a maximum method length of 64 KB. This setting allows for finer granularity if
     * necessary. Default is 64000.
     */
    public Integer getMaxGeneratedCodeLength() {
        return this.configuration.getInteger(TableConfigOptions.MAX_LENGTH_GENERATED_CODE);
    }

    /**
     * Returns the current threshold where generated code will be split into sub-function calls.
     * Java has a maximum method length of 64 KB. This setting allows for finer granularity if
     * necessary. Default is 64000.
     */
    public void setMaxGeneratedCodeLength(Integer maxGeneratedCodeLength) {
        this.configuration.setInteger(
                TableConfigOptions.MAX_LENGTH_GENERATED_CODE, maxGeneratedCodeLength);
    }

    /**
     * Specifies a minimum and a maximum time interval for how long idle state, i.e., state which
     * was not updated, will be retained. State will never be cleared until it was idle for less
     * than the minimum time and will never be kept if it was idle for more than the maximum time.
     *
     * <p>When new data arrives for previously cleaned-up state, the new data will be handled as if
     * it was the first data. This can result in previous results being overwritten.
     *
     * <p>Set to 0 (zero) to never clean-up the state.
     *
     * <p>NOTE: Cleaning up state requires additional bookkeeping which becomes less expensive for
     * larger differences of minTime and maxTime. The difference between minTime and maxTime must be
     * at least 5 minutes.
     *
     * <p>NOTE: Currently maxTime will be ignored and it will automatically derived from minTime as
     * 1.5 x minTime.
     *
     * @param minTime The minimum time interval for which idle state is retained. Set to 0 (zero) to
     *     never clean-up the state.
     * @param maxTime The maximum time interval for which idle state is retained. Must be at least 5
     *     minutes greater than minTime. Set to 0 (zero) to never clean-up the state.
     * @deprecated use {@link #setIdleStateRetention(Duration)} instead.
     */
    @Deprecated
    public void setIdleStateRetentionTime(Time minTime, Time maxTime) {
        if (maxTime.toMilliseconds() - minTime.toMilliseconds() < 300000
                && !(maxTime.toMilliseconds() == 0 && minTime.toMilliseconds() == 0)) {
            throw new IllegalArgumentException(
                    "Difference between minTime: "
                            + minTime.toString()
                            + " and maxTime: "
                            + maxTime.toString()
                            + " should be at least 5 minutes.");
        }
        setIdleStateRetention(Duration.ofMillis(minTime.toMilliseconds()));
    }

    /**
     * Specifies a retention time interval for how long idle state, i.e., state which was not
     * updated, will be retained. State will never be cleared until it was idle for less than the
     * retention time and will be cleared on a best effort basis after the retention time.
     *
     * <p>When new data arrives for previously cleaned-up state, the new data will be handled as if
     * it was the first data. This can result in previous results being overwritten.
     *
     * <p>Set to 0 (zero) to never clean-up the state.
     *
     * @param duration The retention time interval for which idle state is retained. Set to 0 (zero)
     *     to never clean-up the state.
     * @see org.apache.flink.api.common.state.StateTtlConfig
     */
    public void setIdleStateRetention(Duration duration) {
        configuration.set(ExecutionConfigOptions.IDLE_STATE_RETENTION, duration);
    }

    /**
     * NOTE: Currently the concept of min/max idle state retention has been deprecated and only idle
     * state retention time is supported. The min idle state retention is regarded as idle state
     * retention and the max idle state retention is derived from idle state retention as 1.5 x idle
     * state retention.
     *
     * @return The minimum time until state which was not updated will be retained.
     * @deprecated use{@link getIdleStateRetention} instead.
     */
    @Deprecated
    public long getMinIdleStateRetentionTime() {
        return configuration.get(ExecutionConfigOptions.IDLE_STATE_RETENTION).toMillis();
    }

    /**
     * NOTE: Currently the concept of min/max idle state retention has been deprecated and only idle
     * state retention time is supported. The min idle state retention is regarded as idle state
     * retention and the max idle state retention is derived from idle state retention as 1.5 x idle
     * state retention.
     *
     * @return The maximum time until state which was not updated will be retained.
     * @deprecated use{@link getIdleStateRetention} instead.
     */
    @Deprecated
    public long getMaxIdleStateRetentionTime() {
        return getMinIdleStateRetentionTime() * 3 / 2;
    }

    /** @return The duration until state which was not updated will be retained. */
    public Duration getIdleStateRetention() {
        return configuration.get(ExecutionConfigOptions.IDLE_STATE_RETENTION);
    }

    /**
     * 设置一个可以通过{@link org.apache.flink.table.functions.FunctionContext#getJobParameter(String, String)}
     * 访问的自定义用户参数。。
     *
     * Sets a custom user parameter that can be accessed via {@link
     * org.apache.flink.table.functions.FunctionContext#getJobParameter(String, String)}.
     *
     * <p>This will add an entry to the current value of {@link
     * PipelineOptions#GLOBAL_JOB_PARAMETERS}.
     *
     * <p>It is also possible to set multiple parameters at once, which will override any previously
     * set parameters:
     *
     * <pre>{@code
     * Map<String, String> params = ...
     * TableConfig config = tEnv.getConfig;
     * config.getConfiguration().set(PipelineOptions.GLOBAL_JOB_PARAMETERS, params);
     * }</pre>
     */
    @Experimental
    public void addJobParameter(String key, String value) {
        Map<String, String> params =
                getConfiguration()
                        .getOptional(PipelineOptions.GLOBAL_JOB_PARAMETERS)
                        .map(HashMap::new)
                        .orElseGet(HashMap::new);
        params.put(key, value);
        getConfiguration().set(PipelineOptions.GLOBAL_JOB_PARAMETERS, params);
    }

    public static TableConfig getDefault() {
        return new TableConfig();
    }
}
