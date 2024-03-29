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

package org.apache.flink.api.common.operators;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.InvalidProgramException;
import org.apache.flink.api.common.operators.util.FieldSet;

import java.io.Serializable;

/** Container for the semantic properties associated to an operator. */
// 与运算符关联的语义属性的容器
@Internal
public interface SemanticProperties extends Serializable {

    /**
     * 返回源字段未被函数复制到的所有目标字段的索引。
     *
     * Returns the indexes of all target fields to which a source field has been unmodified copied
     * by a function.
     *
     * @param input The input id for the requested source field (0 for first input, 1 for second
     *     input)
     * @param sourceField The index of the field for which the target position index is requested.
     * @return A set containing the indexes of all target fields to which the source field has been
     *     unmodified copied.
     */
    public FieldSet getForwardingTargetFields(int input, int sourceField);

    /**
     * Returns the index of the source field on the given input from which the target field has been
     * unmodified copied by a function.
     *
     * @param input The input id for the requested source field (0 for first input, 1 for second
     *     input)
     * @param targetField The index of the target field to which the source field has been copied.
     * @return The index of the source field on the given index that was copied to the given target
     *     field. -1 if the target field was not copied from any source field of the given input.
     */
    public int getForwardingSourceField(int input, int targetField);

    /**
     * 返回函数访问的输入的所有字段的位置索引。
     *
     * Returns the position indexes of all fields of an input that are accessed by a function.
     *
     * @param input The input id for which accessed fields are requested.
     * @return A set of fields of the specified input which have been accessed by the function. Null
     *     if no information is available.
     */
    public FieldSet getReadFields(int input);

    // ----------------------------------------------------------------------

    // J: 属性接口的内部异常
    public static class InvalidSemanticAnnotationException extends InvalidProgramException {

        private static final long serialVersionUID = 1L;

        public InvalidSemanticAnnotationException(String s) {
            super(s);
        }

        public InvalidSemanticAnnotationException(String s, Throwable e) {
            super(s, e);
        }
    }

    public static class EmptySemanticProperties implements SemanticProperties {

        private static final long serialVersionUID = 1L;

        @Override
        public FieldSet getForwardingTargetFields(int input, int sourceField) {
            return FieldSet.EMPTY_SET;
        }

        @Override
        public int getForwardingSourceField(int input, int targetField) {
            return -1;
        }

        @Override
        public FieldSet getReadFields(int input) {
            return null;
        }
    }
}
