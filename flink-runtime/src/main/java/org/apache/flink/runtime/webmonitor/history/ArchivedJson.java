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

package org.apache.flink.runtime.webmonitor.history;

import org.apache.flink.runtime.history.FsJobArchivist;
import org.apache.flink.runtime.rest.messages.ResponseBody;
import org.apache.flink.runtime.rest.util.RestMapperUtils;
import org.apache.flink.util.Preconditions;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

/**
 * 一个简单的容器，用于处理程序的JSON响应和将为其返回响应的REST url。
 *
 * <p>这些是由{@link JsonArchivist}创建的，并由{@link FsJobArchivist}用于创建类似于REST API的目录结构
 *
 * A simple container for a handler's JSON response and the REST URLs for which the response
 * would've been returned.
 *
 * <p>These are created by {@link JsonArchivist}s, and used by the {@link FsJobArchivist} to create
 * a directory structure resembling the REST API.
 */
public class ArchivedJson {

    private static final ObjectMapper MAPPER = RestMapperUtils.getStrictObjectMapper();

    private final String path;
    private final String json;

    public ArchivedJson(String path, String json) {
        this.path = Preconditions.checkNotNull(path);
        this.json = Preconditions.checkNotNull(json);
    }

    public ArchivedJson(String path, ResponseBody json) throws IOException {
        this.path = Preconditions.checkNotNull(path);
        StringWriter sw = new StringWriter();
        MAPPER.writeValue(sw, Preconditions.checkNotNull(json));
        this.json = sw.toString();
    }

    public String getPath() {
        return path;
    }

    public String getJson() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArchivedJson) {
            ArchivedJson other = (ArchivedJson) obj;
            return this.path.equals(other.path) && this.json.equals(other.json);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, json);
    }

    @Override
    public String toString() {
        return path + ":" + json;
    }
}
