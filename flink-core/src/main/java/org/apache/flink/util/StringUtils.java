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

package org.apache.flink.util;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.types.StringValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

/** Utility class to convert objects into strings in vice-versa. */
@PublicEvolving
public final class StringUtils {

    /**
     * An empty string array. There are just too many places where one needs an empty string array
     * and wants to save some object allocation.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final char[] HEX_CHARS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Given an array of bytes it will convert the bytes to a hex string representation of the
     * bytes.
     *
     * @param bytes the bytes to convert in a hex string
     * @param start start index, inclusively
     * @param end end index, exclusively
     * @return hex string representation of the byte array
     */
    public static String byteToHexString(final byte[] bytes, final int start, final int end) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes == null");
        }

        int length = end - start;
        char[] out = new char[length * 2];

        for (int i = start, j = 0; i < end; i++) {
            out[j++] = HEX_CHARS[(0xF0 & bytes[i]) >>> 4];
            out[j++] = HEX_CHARS[0x0F & bytes[i]];
        }

        return new String(out);
    }

    /**
     * Given an array of bytes it will convert the bytes to a hex string representation of the
     * bytes.
     *
     * @param bytes the bytes to convert in a hex string
     * @return hex string representation of the byte array
     */
    public static String byteToHexString(final byte[] bytes) {
        return byteToHexString(bytes, 0, bytes.length);
    }

    /**
     * Given a hex string this will return the byte array corresponding to the string .
     *
     * @param hex the hex String array
     * @return a byte array that is a hex string representation of the given string. The size of the
     *     byte array is therefore hex.length/2
     */
    public static byte[] hexStringToByte(final String hex) {
        final byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }

    /**
     * 通过调用{@link Object#toString()}并格式化(可能嵌套)数组和{@code null}将给定对象转换为字符串表示形式。
     *
     * <p>参见{@link Arrays#deepToString(Object[])}了解使用的格式的更多信息。
     *
     * Converts the given object into a string representation by calling {@link Object#toString()}
     * and formatting (possibly nested) arrays and {@code null}.
     *
     * <p>See {@link Arrays#deepToString(Object[])} for more information about the used format.
     */
    public static String arrayAwareToString(Object o) {
        final String arrayString = Arrays.deepToString(new Object[] {o});
        return arrayString.substring(1, arrayString.length() - 1);
    }

    /**
     * Replaces control characters by their escape-coded version. For example, if the string
     * contains a line break character ('\n'), this character will be replaced by the two characters
     * backslash '\' and 'n'. As a consequence, the resulting string will not contain any more
     * control characters.
     *
     * @param str The string in which to replace the control characters.
     * @return The string with the replaced characters.
     */
    public static String showControlCharacters(String str) {
        int len = str.length();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < len; i += 1) {
            char c = str.charAt(i);
            switch (c) {
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Creates a random string with a length within the given interval. The string contains only
     * characters that can be represented as a single code point.
     *
     * @param rnd The random used to create the strings.
     * @param minLength The minimum string length.
     * @param maxLength The maximum string length (inclusive).
     * @return A random String.
     */
    public static String getRandomString(Random rnd, int minLength, int maxLength) {
        int len = rnd.nextInt(maxLength - minLength + 1) + minLength;

        char[] data = new char[len];
        for (int i = 0; i < data.length; i++) {
            data[i] = (char) (rnd.nextInt(0x7fff) + 1);
        }
        return new String(data);
    }

    /**
     * Creates a random string with a length within the given interval. The string contains only
     * characters that can be represented as a single code point.
     *
     * @param rnd The random used to create the strings.
     * @param minLength The minimum string length.
     * @param maxLength The maximum string length (inclusive).
     * @param minValue The minimum character value to occur.
     * @param maxValue The maximum character value to occur.
     * @return A random String.
     */
    public static String getRandomString(
            Random rnd, int minLength, int maxLength, char minValue, char maxValue) {
        int len = rnd.nextInt(maxLength - minLength + 1) + minLength;

        char[] data = new char[len];
        int diff = maxValue - minValue + 1;

        for (int i = 0; i < data.length; i++) {
            data[i] = (char) (rnd.nextInt(diff) + minValue);
        }
        return new String(data);
    }

    /**
     * Creates a random alphanumeric string of given length.
     *
     * @param rnd The random number generator to use.
     * @param length The number of alphanumeric characters to append.
     */
    public static String generateRandomAlphanumericString(Random rnd, int length) {
        checkNotNull(rnd);
        checkArgument(length >= 0);

        StringBuilder buffer = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            buffer.append(nextAlphanumericChar(rnd));
        }
        return buffer.toString();
    }

    private static char nextAlphanumericChar(Random rnd) {
        int which = rnd.nextInt(62);
        char c;
        if (which < 10) {
            c = (char) ('0' + which);
        } else if (which < 36) {
            c = (char) ('A' - 10 + which);
        } else {
            c = (char) ('a' - 36 + which);
        }
        return c;
    }

    /**
     * 将一个字符串写入给定的输出。被写入的字符串可以通过{@link #readString(DataInputView)}读取。
     *
     * Writes a String to the given output. The written string can be read with {@link
     * #readString(DataInputView)}.
     *
     * @param str The string to write
     * @param out The output to write to
     * @throws IOException Thrown, if the writing or the serialization fails.
     */
    public static void writeString(@Nonnull String str, DataOutputView out) throws IOException {
        checkNotNull(str);
        StringValue.writeString(str, out);
    }

    /**
     * Reads a non-null String from the given input.
     *
     * @param in The input to read from
     * @return The deserialized String
     * @throws IOException Thrown, if the reading or the deserialization fails.
     */
    public static String readString(DataInputView in) throws IOException {
        return StringValue.readString(in);
    }

    /**
     * Writes a String to the given output. The string may be null. The written string can be read
     * with {@link #readNullableString(DataInputView)}-
     *
     * @param str The string to write, or null.
     * @param out The output to write to.
     * @throws IOException Thrown, if the writing or the serialization fails.
     */
    public static void writeNullableString(@Nullable String str, DataOutputView out)
            throws IOException {
        if (str != null) {
            out.writeBoolean(true);
            writeString(str, out);
        } else {
            out.writeBoolean(false);
        }
    }

    /**
     * Reads a String from the given input. The string may be null and must have been written with
     * {@link #writeNullableString(String, DataOutputView)}.
     *
     * @param in The input to read from.
     * @return The deserialized string, or null.
     * @throws IOException Thrown, if the reading or the deserialization fails.
     */
    public static @Nullable String readNullableString(DataInputView in) throws IOException {
        if (in.readBoolean()) {
            return readString(in);
        } else {
            return null;
        }
    }

    /**
     * Checks if the string is null, empty, or contains only whitespace characters. A whitespace
     * character is defined via {@link Character#isWhitespace(char)}.
     *
     * @param str The string to check
     * @return True, if the string is null or blank, false otherwise.
     */
    public static boolean isNullOrWhitespaceOnly(String str) {
        if (str == null || str.length() == 0) {
            return true;
        }

        final int len = str.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * If both string arguments are non-null, this method concatenates them with ' and '. If only
     * one of the arguments is non-null, this method returns the non-null argument. If both
     * arguments are null, this method returns null.
     *
     * @param s1 The first string argument
     * @param s2 The second string argument
     * @return The concatenated string, or non-null argument, or null
     */
    @Nullable
    public static String concatenateWithAnd(@Nullable String s1, @Nullable String s2) {
        if (s1 != null) {
            return s2 == null ? s1 : s1 + " and " + s2;
        } else {
            return s2;
        }
    }

    /**
     * Generates a string containing a comma-separated list of values in double-quotes. Uses
     * lower-cased values returned from {@link Object#toString()} method for each element in the
     * given array. Null values are skipped.
     *
     * @param values array of elements for the list
     * @return The string with quoted list of elements
     */
    public static String toQuotedListString(Object[] values) {
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(v -> v.toString().toLowerCase())
                .collect(Collectors.joining(", ", "\"", "\""));
    }

    // ------------------------------------------------------------------------

    /** Prevent instantiation of this utility class. */
    private StringUtils() {}
}
