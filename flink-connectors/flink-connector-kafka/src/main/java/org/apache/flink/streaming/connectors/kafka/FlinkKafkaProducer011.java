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

package org.apache.flink.streaming.connectors.kafka;

import org.apache.flink.api.common.typeutils.SimpleTypeSerializerSnapshot;

/**
 * 兼容类使从0.11连接器迁移到通用连接器成为可能。
 *
 * <p>问题是，FlinkKafkaProducer (universal)和FlinkKafkaProducer011有不同的名称，他们都定义静态类
 * NextTransactionalIdHint, KafkaTransactionState和KafkaTransactionContext在父类内部。这将导致不兼容问题，
 * 例如FlinkKafkaProducer011.KafkaTransactionState, FlinkKafkaProducer.KafkaTransactionState被视为完全不
 * 兼容的类，尽管它们是相同的。
 *
 * <p>这个问题通过使用自定义序列化逻辑来解决:保持一个假的FlinkKafkaProducer011。通用连接器(这个类)中的序列化器类，
 * 作为反序列化的入口点，并将它们转换为FlinkKafkaProducer。序列化程序计数器部分。在所有这些情况下，序列化的二进制数据
 * 都是完全相同的。
 *
 *
 * Compatibility class to make migration possible from the 0.11 connector to the universal one.
 *
 * <p>Problem is that FlinkKafkaProducer (universal) and FlinkKafkaProducer011 have different names
 * and they both defined static classes NextTransactionalIdHint, KafkaTransactionState and
 * KafkaTransactionContext inside the parent classes. This is causing incompatibility problems since
 * for example FlinkKafkaProducer011.KafkaTransactionState and
 * FlinkKafkaProducer.KafkaTransactionState are treated as completely incompatible classes, despite
 * being identical.
 *
 * <p>This issue is solved by using custom serialization logic: keeping a fake/dummy
 * FlinkKafkaProducer011.*Serializer classes in the universal connector (this class), as entry
 * points for the deserialization and converting them to FlinkKafkaProducer.*Serializer counter
 * parts. After all serialized binary data are exactly the same in all of those cases.
 *
 * <p>For more details check FLINK-11249 and the discussion in the pull requests.
 */
// CHECKSTYLE:OFF: JavadocType
public class FlinkKafkaProducer011 {
    public static class NextTransactionalIdHintSerializer {
        public static final class NextTransactionalIdHintSerializerSnapshot
                extends SimpleTypeSerializerSnapshot<FlinkKafkaProducer.NextTransactionalIdHint> {
            public NextTransactionalIdHintSerializerSnapshot() {
                super(FlinkKafkaProducer.NextTransactionalIdHintSerializer::new);
            }
        }
    }

    public static class ContextStateSerializer {
        public static final class ContextStateSerializerSnapshot
                extends SimpleTypeSerializerSnapshot<FlinkKafkaProducer.KafkaTransactionContext> {
            public ContextStateSerializerSnapshot() {
                super(FlinkKafkaProducer.ContextStateSerializer::new);
            }
        }
    }

    public static class TransactionStateSerializer {
        public static final class TransactionStateSerializerSnapshot
                extends SimpleTypeSerializerSnapshot<FlinkKafkaProducer.KafkaTransactionState> {
            public TransactionStateSerializerSnapshot() {
                super(FlinkKafkaProducer.TransactionStateSerializer::new);
            }
        }
    }

    public static class NextTransactionalIdHint
            extends FlinkKafkaProducer.NextTransactionalIdHint {}
}
// CHECKSTYLE:ON: JavadocType
