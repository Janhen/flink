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

package org.apache.flink.sql.parser;

import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.util.NlsString;

import java.util.LinkedHashMap;

/** Utils methods for partition DDLs. */
// 分区ddl的Utils方法。
public class SqlPartitionUtils {

    private SqlPartitionUtils() {}

    /**
     * 获取静态分区键值对作为字符串。
     *
     * <p>对于字符字面值，我们返回未加引号和未转义的值。对于其他类型，我们使用{@link SqlLiteral#toString()}来
     *   获取值literal的字符串格式。
     *
     * Get static partition key value pair as strings.
     *
     * <p>For character literals we return the unquoted and unescaped values. For other types we use
     * {@link SqlLiteral#toString()} to get the string format of the value literal.
     *
     * @return the mapping of column names to values of partition specifications, returns an empty
     *     map if there is no partition specifications.
     */
    public static LinkedHashMap<String, String> getPartitionKVs(SqlNodeList partitionSpec) {
        if (partitionSpec == null) {
            return null;
        }
        LinkedHashMap<String, String> ret = new LinkedHashMap<>();
        if (partitionSpec.size() == 0) {
            return ret;
        }
        for (SqlNode node : partitionSpec.getList()) {
            SqlProperty sqlProperty = (SqlProperty) node;
            Comparable comparable = SqlLiteral.value(sqlProperty.getValue());
            String value =
                    comparable instanceof NlsString
                            ? ((NlsString) comparable).getValue()
                            : comparable.toString();
            ret.put(sqlProperty.getKey().getSimple(), value);
        }
        return ret;
    }
}
