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

package org.apache.flink.streaming.util.graph;

import org.apache.flink.api.dag.Transformation;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.streaming.api.transformations.PhysicalTransformation;

/**
 * Utility class that contains helper methods to generating {@link
 * org.apache.flink.streaming.api.graph.StreamGraph}.
 */
public final class StreamGraphUtils {

    /**
     * 当禁用自动生成uid时，如果没有设置{@link PhysicalTransformation}的uid或散列，则抛出
     * {@link IllegalStateException}。
     *
     * Throw {@link IllegalStateException} if the {@link PhysicalTransformation}'s uid or hash is
     * not set when auto generate uid is disabled.
     *
     * @param streamGraph The given graph that the transformation is added to
     * @param transformation The transformation needed to validate
     */
    public static void validateTransformationUid(
            StreamGraph streamGraph, Transformation<?> transformation) {
        if (!streamGraph.getExecutionConfig().hasAutoGeneratedUIDsEnabled()) {
            if (transformation instanceof PhysicalTransformation
                    && transformation.getUserProvidedNodeHash() == null
                    && transformation.getUid() == null) {
                throw new IllegalStateException(
                        "Auto generated UIDs have been disabled "
                                + "but no UID or hash has been assigned to operator "
                                + transformation.getName());
            }
        }
    }

    /**
     * 根据给定的转换配置流节点的缓冲区超时
     *
     * Configure a stream node's buffer timeout according to the given transformation.
     *
     * @param streamGraph The StreamGraph the node belongs to
     * @param nodeId The node's id
     * @param transformation A given transformation
     * @param defaultBufferTimeout The default buffer timeout value
     */
    public static <T> void configureBufferTimeout(
            StreamGraph streamGraph,
            int nodeId,
            Transformation<T> transformation,
            long defaultBufferTimeout) {

        if (transformation.getBufferTimeout() >= 0) {
            streamGraph.setBufferTimeout(nodeId, transformation.getBufferTimeout());
        } else {
            streamGraph.setBufferTimeout(nodeId, defaultBufferTimeout);
        }
    }
}
