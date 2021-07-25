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

package org.apache.flink.table.sinks;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableException;

/**
 * 定义一个外部的{@link TableSink}来产生一个带有插入、更新和删除更改的流{@link Table}。{@link Table}必须有唯一
 * 的关键字段(原子或复合)或只能追加。
 *
 * <p>如果{@link Table}没有唯一的键并且不是只追加，将抛出一个{@link TableException}。
 *
 * <p>表的唯一键由{@link UpsertStreamTableSink#setKeyFields(String[])}方法配置。
 *
 * <p> {@link Table} 将被转换成一个 upsert 和 delete 消息流，编码为{@link Tuple2}。第一个字段是一个表示消息类型的
 * {@link Boolean}标志。第二个字段保存请求类型{@link T}的记录。.
 *
 * <p>具有 true {@link Boolean}字段的消息是配置键的 upsert 消息。
 *
 * <p>带 false 标志的消息是对配置的密钥的删除消息。
 *
 * <p>如果表是仅追加的，那么所有消息都将有一个 true 标志，并且必须解释为插入。
 *
 * Defines an external {@link TableSink} to emit a streaming {@link Table} with insert, update, and
 * delete changes. The {@link Table} must be have unique key fields (atomic or composite) or be
 * append-only.
 *
 * <p>If the {@link Table} does not have a unique key and is not append-only, a {@link
 * TableException} will be thrown.
 *
 * <p>The unique key of the table is configured by the {@link
 * UpsertStreamTableSink#setKeyFields(String[])} method.
 *
 * <p>The {@link Table} will be converted into a stream of upsert and delete messages which are
 * encoded as {@link Tuple2}. The first field is a {@link Boolean} flag to indicate the message
 * type. The second field holds the record of the requested type {@link T}.
 *
 * <p>A message with true {@link Boolean} field is an upsert message for the configured key.
 *
 * <p>A message with false flag is a delete message for the configured key.
 *
 * <p>If the table is append-only, all messages will have a true flag and must be interpreted as
 * insertions.
 *
 * @param <T> Type of records that this {@link TableSink} expects and supports.
 */
@PublicEvolving
public interface UpsertStreamTableSink<T> extends StreamTableSink<Tuple2<Boolean, T>> {

    /**
     * 配置要写入的{@link Table}的唯一关键字段。该方法在{@link TableSink#configure(String[],TypeInformation[])}
     * 后面调用。
     *
     * <p>如果表由一条(更新的)记录组成，则键数组可能是空的。如果表没有键并且是只追加的，则keys属性为空。
     *
     * Configures the unique key fields of the {@link Table} to write. The method is called after
     * {@link TableSink#configure(String[], TypeInformation[])}.
     *
     * <p>The keys array might be empty, if the table consists of a single (updated) record. If the
     * table does not have a key and is append-only, the keys attribute is null.
     *
     * @param keys the field names of the table's keys, an empty array if the table has a single
     *     row, and null if the table is append-only and has no key.
     */
    void setKeyFields(String[] keys);

    /**
     * 指定要写入的 {@link Table} 是否为仅追加。
     *
     * Specifies whether the {@link Table} to write is append-only or not.
     *
     * @param isAppendOnly true if the table is append-only, false otherwise.
     */
    void setIsAppendOnly(Boolean isAppendOnly);

    /** Returns the requested record type. */
    // 返回请求的记录类型
    TypeInformation<T> getRecordType();

    @Override
    default TypeInformation<Tuple2<Boolean, T>> getOutputType() {
        return new TupleTypeInfo<>(Types.BOOLEAN, getRecordType());
    }
}
