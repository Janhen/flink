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

import static org.apache.flink.configuration.ConfigOptions.key;

/** The set of configuration options relating to the HistoryServer. */
// 与HistoryServer相关的一组配置选项
@PublicEvolving
public class HistoryServerOptions {

    /**
     * HistoryServer为新存档轮询{@link HistoryServerOptions#HISTORY_SERVER_ARCHIVE_DIRS}的时间间隔。
     *
     * The interval at which the HistoryServer polls {@link
     * HistoryServerOptions#HISTORY_SERVER_ARCHIVE_DIRS} for new archives.
     */
    public static final ConfigOption<Long> HISTORY_SERVER_ARCHIVE_REFRESH_INTERVAL =
            key("historyserver.archive.fs.refresh-interval")
                    .defaultValue(10000L)
                    .withDescription(
                            "Interval in milliseconds for refreshing the archived job directories.");

    /** Comma-separated list of directories which the HistoryServer polls for new archives. */
    // 用逗号分隔的目录列表，HistoryServer将对这些目录进行调查，以获取新的存档。
    public static final ConfigOption<String> HISTORY_SERVER_ARCHIVE_DIRS =
            key("historyserver.archive.fs.dir")
                    .noDefaultValue()
                    .withDescription(
                            "Comma separated list of directories to fetch archived jobs from. The history server will"
                                    + " monitor these directories for archived jobs. You can configure the JobManager to archive jobs to a"
                                    + " directory via `jobmanager.archive.fs.dir`.");

    /** If this option is enabled then deleted job archives are also deleted from HistoryServer. */
    // 如果启用此选项，则也将从HistoryServer删除已删除的作业存档
    public static final ConfigOption<Boolean> HISTORY_SERVER_CLEANUP_EXPIRED_JOBS =
            key("historyserver.archive.clean-expired-jobs")
                    .defaultValue(false)
                    .withDescription(
                            String.format(
                                    "Whether HistoryServer should cleanup jobs"
                                            + " that are no longer present `%s`.",
                                    HISTORY_SERVER_ARCHIVE_DIRS.key()));

    /** The local directory used by the HistoryServer web-frontend. */
    // HistoryServer web-frontend使用的本地目录。
    public static final ConfigOption<String> HISTORY_SERVER_WEB_DIR =
            key("historyserver.web.tmpdir")
                    .noDefaultValue()
                    .withDescription(
                            "This configuration parameter allows defining the Flink web directory to be used by the"
                                    + " history server web interface. The web interface will copy its static files into the directory.");

    /** The address under which the HistoryServer web-frontend is accessible. */
    // 可以访问历史服务器web前端的地址
    public static final ConfigOption<String> HISTORY_SERVER_WEB_ADDRESS =
            key("historyserver.web.address")
                    .noDefaultValue()
                    .withDescription("Address of the HistoryServer's web interface.");

    /** The port under which the HistoryServer web-frontend is accessible. */
    // 历史服务器web前端可访问的端口。
    public static final ConfigOption<Integer> HISTORY_SERVER_WEB_PORT =
            key("historyserver.web.port")
                    .defaultValue(8082)
                    .withDescription("Port of the HistoryServers's web interface.");

    /** The refresh interval for the HistoryServer web-frontend in milliseconds. */
    // HistoryServer web-frontend 的刷新间隔(毫秒)。
    public static final ConfigOption<Long> HISTORY_SERVER_WEB_REFRESH_INTERVAL =
            key("historyserver.web.refresh-interval")
                    .defaultValue(10000L)
                    .withDescription(
                            "The refresh interval for the HistoryServer web-frontend in milliseconds.");

    /**
     * enables禁用HistoryServer web-frontend的SSL支持。只有启用了{@link SecurityOptions#SSL_REST_ENABLED}
     * 时才相关。
     *
     * Enables/Disables SSL support for the HistoryServer web-frontend. Only relevant if {@link
     * SecurityOptions#SSL_REST_ENABLED} is enabled.
     */
    public static final ConfigOption<Boolean> HISTORY_SERVER_WEB_SSL_ENABLED =
            key("historyserver.web.ssl.enabled")
                    .defaultValue(false)
                    .withDescription(
                            "Enable HTTPs access to the HistoryServer web frontend. This is applicable only when the"
                                    + " global SSL flag security.ssl.enabled is set to true.");

    private HistoryServerOptions() {}
}
