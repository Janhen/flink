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

package org.apache.flink.api.common.typeinfo;

import org.apache.flink.annotation.Public;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.functions.InvalidTypesException;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.util.FlinkRuntimeException;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * TypeInformation 是 Flink 类型系统的核心类。Flink 需要用于用户函数输入或返回类型的所有类型的类型信息。这个类型信息类
 * 充当生成序列化器和比较器的工具，并执行语义检查，例如用作联接分组键的字段是否实际存在。
 *
 * <p>类型信息还在编程语言对象模型和逻辑平面模式之间架起一座桥梁。它将字段从类型映射到平面模式中的列（字段）。并非一个
 * 类型的所有字段都映射到平面模式中的单独字段，并且通常整个类型都映射到一个字段。重要的是要注意模式必须适用于一个类型
 * 的所有实例。因此，列表和数组中的元素不会分配给单独的字段，而是将列表和数组视为一个字段，以说明数组中的不同长度。
 *
 * ...
 *
 * <p>类型“id”、“text”和“timestamp”是占用一个字段的基本类型。 “InnerType”有两个，也有两个字段。 “OuterType”
 *   有两个字段，总共三个字段（通过递归展平包含“id”、“text”和“timestamp”）。
 *
 * TypeInformation is the core class of Flink's type system. Flink requires a type information for
 * all types that are used as input or return type of a user function. This type information class
 * acts as the tool to generate serializers and comparators, and to perform semantic checks such as
 * whether the fields that are used as join/grouping keys actually exist.
 *
 * <p>The type information also bridges between the programming languages object model and a logical
 * flat schema. It maps fields from the types to columns (fields) in a flat schema. Not all fields
 * from a type are mapped to a separate fields in the flat schema and often, entire types are mapped
 * to one field. It is important to notice that the schema must hold for all instances of a type.
 * For that reason, elements in lists and arrays are not assigned to individual fields, but the
 * lists and arrays are considered to be one field in total, to account for different lengths in the
 * arrays.
 *
 * <ul>
 *   <li>Basic types are indivisible and are considered a single field.
 *   <li>Arrays and collections are one field
 *   <li>Tuples and case classes represent as many fields as the class has fields
 * </ul>
 *
 * <p>To represent this properly, each type has an <i>arity</i> (the number of fields it contains
 * directly), and a <i>total number of fields</i> (number of fields in the entire schema of this
 * type, including nested types).
 *
 * <p>Consider the example below:
 *
 * <pre>{@code
 * public class InnerType {
 *   public int id;
 *   public String text;
 * }
 *
 * public class OuterType {
 *   public long timestamp;
 *   public InnerType nestedType;
 * }
 * }</pre>
 *
 * <p>The types "id", "text", and "timestamp" are basic types that take up one field. The
 * "InnerType" has an arity of two, and also two fields totally. The "OuterType" has an arity of two
 * fields, and a total number of three fields ( it contains "id", "text", and "timestamp" through
 * recursive flattening).
 *
 * @param <T> The type represented by this type information.
 */
@Public
public abstract class TypeInformation<T> implements Serializable {

    private static final long serialVersionUID = -7742311969684489493L;

    /**
     * Checks if this type information represents a basic type. Basic types are defined in {@link
     * BasicTypeInfo} and are primitives, their boxing types, Strings, Date, Void, ...
     *
     * @return True, if this type information describes a basic type, false otherwise.
     */
    @PublicEvolving
    public abstract boolean isBasicType();

    /**
     * Checks if this type information represents a Tuple type. Tuple types are subclasses of the
     * Java API tuples.
     *
     * @return True, if this type information describes a tuple type, false otherwise.
     */
    @PublicEvolving
    public abstract boolean isTupleType();

    /**
     * 获取此类型的 arity - 没有嵌套的字段数。
     *
     * Gets the arity of this type - the number of fields without nesting.
     *
     * @return Gets the number of fields in this type without nesting.
     */
    @PublicEvolving
    public abstract int getArity();

    /**
     * Gets the number of logical fields in this type. This includes its nested and transitively
     * nested fields, in the case of composite types. In the example above, the OuterType type has
     * three fields in total.
     *
     * <p>The total number of fields must be at least 1.
     *
     * @return The number of fields in this type, including its sub-fields (for composite types)
     */
    @PublicEvolving
    public abstract int getTotalFields();

    /**
     * Gets the class of the type represented by this type information.
     *
     * @return The class of the type represented by this type information.
     */
    @PublicEvolving
    public abstract Class<T> getTypeClass();

    /**
     * Optional method for giving Flink's type extraction system information about the mapping of a
     * generic type parameter to the type information of a subtype. This information is necessary in
     * cases where type information should be deduced from an input type.
     *
     * <p>For instance, a method for a {@link Tuple2} would look like this: <code>
     * Map m = new HashMap();
     * m.put("T0", this.getTypeAt(0));
     * m.put("T1", this.getTypeAt(1));
     * return m;
     * </code>
     *
     * @return map of inferred subtypes; it does not have to contain all generic parameters as key;
     *     values may be null if type could not be inferred
     */
    @PublicEvolving
    public Map<String, TypeInformation<?>> getGenericParameters() {
        // return an empty map as the default implementation
        return Collections.emptyMap();
    }

    /**
     * Checks whether this type can be used as a key. As a bare minimum, types have to be hashable
     * and comparable to be keys.
     *
     * @return True, if the type can be used as a key, false otherwise.
     */
    @PublicEvolving
    public abstract boolean isKeyType();

    /**
     * Checks whether this type can be used as a key for sorting. The order produced by sorting this
     * type must be meaningful.
     */
    @PublicEvolving
    public boolean isSortKeyType() {
        return isKeyType();
    }

    /**
     * Creates a serializer for the type. The serializer may use the ExecutionConfig for
     * parameterization.
     *
     * @param config The config used to parameterize the serializer.
     * @return A serializer for this type.
     */
    @PublicEvolving
    public abstract TypeSerializer<T> createSerializer(ExecutionConfig config);

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    /**
     * Returns true if the given object can be equaled with this object. If not, it returns false.
     *
     * @param obj Object which wants to take part in the equality relation
     * @return true if obj can be equaled with this, otherwise false
     */
    public abstract boolean canEqual(Object obj);

    // ------------------------------------------------------------------------

    /**
     * Creates a TypeInformation for the type described by the given class.
     *
     * <p>This method only works for non-generic types. For generic types, use the {@link
     * #of(TypeHint)} method.
     *
     * @param typeClass The class of the type.
     * @param <T> The generic type.
     * @return The TypeInformation object for the type described by the hint.
     */
    public static <T> TypeInformation<T> of(Class<T> typeClass) {
        try {
            return TypeExtractor.createTypeInfo(typeClass);
        } catch (InvalidTypesException e) {
            throw new FlinkRuntimeException(
                    "Cannot extract TypeInformation from Class alone, because generic parameters are missing. "
                            + "Please use TypeInformation.of(TypeHint) instead, or another equivalent method in the API that "
                            + "accepts a TypeHint instead of a Class. "
                            + "For example for a Tuple2<Long, String> pass a 'new TypeHint<Tuple2<Long, String>>(){}'.");
        }
    }

    /**
     * 通过实用程序“类型提示”为泛型类型创建 TypeInformation。此方法可按如下方式使用：
     *
     * Creates a TypeInformation for a generic type via a utility "type hint". This method can be
     * used as follows:
     *
     * <pre>{@code
     * TypeInformation<Tuple2<String, Long>> info = TypeInformation.of(new TypeHint<Tuple2<String, Long>>(){});
     * }</pre>
     *
     * @param typeHint The hint for the generic type.
     * @param <T> The generic type.
     * @return The TypeInformation object for the type described by the hint.
     */
    public static <T> TypeInformation<T> of(TypeHint<T> typeHint) {
        return typeHint.getTypeInfo();
    }
}
