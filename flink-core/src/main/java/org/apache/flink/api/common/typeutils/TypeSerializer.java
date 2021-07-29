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

package org.apache.flink.api.common.typeutils;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;

import java.io.IOException;
import java.io.Serializable;

/**
 * 该接口描述了 Flink 运行时处理数据类型所需的方法。具体来说，该接口包含序列化和复制方法。
 *
 * <p>此类中的方法不一定是线程安全的。为避免不可预知的副作用，建议调用 {@code duplicate()} 方法并为每个线程使用一个
 * 序列化器实例。
 *
 * <p><b>将 TypeSerializer 升级到新的 TypeSerializerSnapshot 模型<b>
 *
 * <p>如果你在Flink 1.6版本中实现了一个TypeSerializer，并且想让这个实现适应新的接口，以支持适当的状态模式演变，同时
 * 保持向后兼容性，那么这个部分是相关的。请遵循以下步骤:
 *
 *  ...
 *
 * This interface describes the methods that are required for a data type to be handled by the Flink
 * runtime. Specifically, this interface contains the serialization and copying methods.
 *
 * <p>The methods in this class are not necessarily thread safe. To avoid unpredictable side
 * effects, it is recommended to call {@code duplicate()} method and use one serializer instance per
 * thread.
 *
 * <p><b>Upgrading TypeSerializers to the new TypeSerializerSnapshot model</b>
 *
 * <p>This section is relevant if you implemented a TypeSerializer in Flink versions up to 1.6 and
 * want to adapt that implementation to the new interfaces that support proper state schema
 * evolution, while maintaining backwards compatibility. Please follow these steps:
 *
 * <ul>
 *   <li>Change the type serializer's config snapshot to implement {@link TypeSerializerSnapshot},
 *       rather than extending {@code TypeSerializerConfigSnapshot} (as previously).
 *   <li>If the above step was completed, then the upgrade is done. Otherwise, if changing to
 *       implement {@link TypeSerializerSnapshot} directly in-place as the same class isn't possible
 *       (perhaps because the new snapshot is intended to have completely different written contents
 *       or intended to have a different class name), retain the old serializer snapshot class
 *       (extending {@code TypeSerializerConfigSnapshot}) under the same name and give the updated
 *       serializer snapshot class (the one extending {@code TypeSerializerSnapshot}) a new name.
 *   <li>Override the {@code
 *       TypeSerializerConfigSnapshot#resolveSchemaCompatibility(TypeSerializer)} method to perform
 *       the compatibility check based on configuration written by the old serializer snapshot
 *       class.
 * </ul>
 *
 * @param <T> The data type that the serializer serializes.
 */
@PublicEvolving
public abstract class TypeSerializer<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    // --------------------------------------------------------------------------------------------
    // General information about the type and the serializer
    // --------------------------------------------------------------------------------------------

    /**
     * 获取该类型是否为不可变类型。
     *
     * Gets whether the type is an immutable type.
     *
     * @return True, if the type is immutable.
     */
    public abstract boolean isImmutableType();

    /**
     * 如果它是有状态的，则创建此序列化器的深层副本。如果序列化器不是有状态的，则返回本身。
     *
     * <p>我们需要这个，因为序列化器可能在多个线程中使用。无状态序列化器本质上是线程安全的，而有状态序列化器可能不是线程
     * 安全的。
     *
     * Creates a deep copy of this serializer if it is necessary, i.e. if it is stateful. This can
     * return itself if the serializer is not stateful.
     *
     * <p>We need this because Serializers might be used in several threads. Stateless serializers
     * are inherently thread-safe while stateful serializers might not be thread-safe.
     */
    public abstract TypeSerializer<T> duplicate();

    // --------------------------------------------------------------------------------------------
    // Instantiation & Cloning
    // --------------------------------------------------------------------------------------------

    /**
     * 创建数据类型的新实例。
     *
     * Creates a new instance of the data type.
     *
     * @return A new instance of the data type.
     */
    public abstract T createInstance();

    /**
     * 在新元素中创建给定元素的深层副本。
     *
     * Creates a deep copy of the given element in a new element.
     *
     * @param from The element reuse be copied.
     * @return A deep copy of the element.
     */
    public abstract T copy(T from);

    /**
     * 从给定元素创建一个副本。如果类型是可变的，则该方法尝试将副本存储在给定的重用元素中。然而，这并不能保证。
     *
     * Creates a copy from the given element. The method makes an attempt to store the copy in the
     * given reuse element, if the type is mutable. This is, however, not guaranteed.
     *
     * @param from The element to be copied.
     * @param reuse The element to be reused. May or may not be used.
     * @return A deep copy of the element.
     */
    public abstract T copy(T from, T reuse);

    // --------------------------------------------------------------------------------------------

    /**
     * 如果数据类型是固定长度数据类型，则获取数据类型的长度。
     *
     * Gets the length of the data type, if it is a fix length data type.
     *
     * @return The length of the data type, or <code>-1</code> for variable length data types.
     */
    public abstract int getLength();

    // --------------------------------------------------------------------------------------------

    /**
     * 将给定的记录序列化到给定的目标 output view
     *
     * Serializes the given record to the given target output view.
     *
     * @param record The record to serialize.
     * @param target The output view to write the serialized data to.
     * @throws IOException Thrown, if the serialization encountered an I/O related error. Typically
     *     raised by the output view, which may have an underlying I/O channel to which it
     *     delegates.
     */
    public abstract void serialize(T record, DataOutputView target) throws IOException;

    /**
     * 从给定的源输入视图反序列化一条记录。
     *
     * De-serializes a record from the given source input view.
     *
     * @param source The input view from which to read the data.
     * @return The deserialized element.
     * @throws IOException Thrown, if the de-serialization encountered an I/O related error.
     *     Typically raised by the input view, which may have an underlying I/O channel from which
     *     it reads.
     */
    public abstract T deserialize(DataInputView source) throws IOException;

    /**
     * 如果是可变的，则将记录从给定源输入视图反序列化到给定重用记录实例。
     *
     * De-serializes a record from the given source input view into the given reuse record instance
     * if mutable.
     *
     * @param reuse The record instance into which to de-serialize the data.
     * @param source The input view from which to read the data.
     * @return The deserialized element.
     * @throws IOException Thrown, if the de-serialization encountered an I/O related error.
     *     Typically raised by the input view, which may have an underlying I/O channel from which
     *     it reads.
     */
    public abstract T deserialize(T reuse, DataInputView source) throws IOException;

    /**
     * 将一条记录从源输入视图复制到目标输出视图。这个操作是对二进制数据进行操作，还是对记录进行部分反序列化以确定其
     * 长度(比如可变长度的记录)，这取决于实现者。二进制拷贝通常更快。包含两个整数(总共8字节)的记录的副本最有效地
     * 实现为 {@code target.write(source, 8);}.
     *
     * Copies exactly one record from the source input view to the target output view. Whether this
     * operation works on binary data or partially de-serializes the record to determine its length
     * (such as for records of variable length) is up to the implementer. Binary copies are
     * typically faster. A copy of a record containing two integer numbers (8 bytes total) is most
     * efficiently implemented as {@code target.write(source, 8);}.
     *
     * @param source The input view from which to read the record.
     * @param target The target output view to which to write the record.
     * @throws IOException Thrown if any of the two views raises an exception.
     */
    public abstract void copy(DataInputView source, DataOutputView target) throws IOException;

    public abstract boolean equals(Object obj);

    public abstract int hashCode();

    // --------------------------------------------------------------------------------------------
    // Serializer configuration snapshot for checkpoints/savepoints
    // --------------------------------------------------------------------------------------------

    /**
     * 快照这个 TypeSerializer 的配置。只有当序列化器用于在检查点或保存点中存储状态时，此方法才相关。
     *
     * <p> TypeSerializer的快照应该包含影响序列化器序列化格式的所有信息。快照有两个目的:第一，在恢复检查点保存点时
     * 重新生成序列化器，第二，检查序列化格式是否与恢复程序中使用的序列化器兼容。
     *
     * <p><b>重要:<b> TypeSerializerSnapshots 在 Flink 1.6 之后改变。在 Flink 1.6
     * 之前的版本中实现的序列化器仍然可以工作，但是需要根据新的模型进行调整，以支持状态演化，并具有可扩展性。请参阅类
     * 级注释“将t ypeserializer 升级到新的 TypeSerializerSnapshot 模型”一节了解详细信息。
     *
     * Snapshots the configuration of this TypeSerializer. This method is only relevant if the
     * serializer is used to state stored in checkpoints/savepoints.
     *
     * <p>The snapshot of the TypeSerializer is supposed to contain all information that affects the
     * serialization format of the serializer. The snapshot serves two purposes: First, to reproduce
     * the serializer when the checkpoint/savepoint is restored, and second, to check whether the
     * serialization format is compatible with the serializer used in the restored program.
     *
     * <p><b>IMPORTANT:</b> TypeSerializerSnapshots changed after Flink 1.6. Serializers implemented
     * against Flink versions up to 1.6 should still work, but adjust to new model to enable state
     * evolution and be future-proof. See the class-level comments, section "Upgrading
     * TypeSerializers to the new TypeSerializerSnapshot model" for details.
     *
     * @see TypeSerializerSnapshot#resolveSchemaCompatibility(TypeSerializer)
     * @return snapshot of the serializer's current configuration (cannot be {@code null}).
     */
    public abstract TypeSerializerSnapshot<T> snapshotConfiguration();
}
