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

package org.apache.flink.api.common.serialization;

import org.apache.flink.annotation.PublicEvolving;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * 流文件接收器使用{@link Encoder}将传入的元素实际写入到桶中的文件中。
 *
 * A {@link Encoder} is used by the streaming file sink to perform the actual writing of the
 * incoming elements to the files in a bucket.
 *
 * @param <IN> The type of the elements that are being written by the sink.
 */
@PublicEvolving
public interface Encoder<IN> extends Serializable {

    /**
     * Writes one element to the bucket file.
     *
     * @param element the element to be written.
     * @param stream the stream to write the element to.
     */
    void encode(IN element, OutputStream stream) throws IOException;
}
