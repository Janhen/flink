/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.	See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.	You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.runtime.functions;

import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.table.data.binary.BinaryStringData;
import org.apache.flink.table.runtime.util.SegmentsUtil;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static org.apache.flink.table.data.binary.BinaryStringData.fromString;

/**
 * String Like util:匹配一个跳棋链序列。
 *
 * <p>它有4个链场景(没有转义或单个字符通配符)1)锚定左“abc%def%”2)锚定右“%abc%def”3)un锚定“%abc%def%”4)锚定
 *   在两边“abc%def”
 *
 * String Like util: Matches a chained sequence of checkers.
 *
 * <p>This has 4 chain scenarios cases in it (has no escaping or single char wildcards) 1) anchored
 * left "abc%def%" 2) anchored right "%abc%def" 3) unanchored "%abc%def%" 4) anchored on both sides
 * "abc%def"
 */
public class SqlLikeChainChecker {

    private final int minLen;
    private final BinaryStringData beginPattern;
    private final BinaryStringData endPattern;
    private final BinaryStringData[] middlePatterns;
    private final int[] midLens;
    private final int beginLen;
    private final int endLen;

    public SqlLikeChainChecker(String pattern) {
        final StringTokenizer tokens = new StringTokenizer(pattern, "%");
        final boolean leftAnchor = !pattern.startsWith("%");
        final boolean rightAnchor = !pattern.endsWith("%");
        int len = 0;
        // at least 2 checkers always
        BinaryStringData leftPattern = null;
        BinaryStringData rightPattern = null;
        int leftLen = 0; // not -1
        int rightLen = 0; // not -1
        final List<BinaryStringData> middleCheckers = new ArrayList<>(2);
        final List<Integer> lengths = new ArrayList<>(2);

        for (int i = 0; tokens.hasMoreTokens(); i++) {
            String chunk = tokens.nextToken();
            if (chunk.length() == 0) {
                // %% is folded in the .*?.*? regex usually into .*?
                continue;
            }
            len += utf8Length(chunk);
            if (leftAnchor && i == 0) {
                // first item
                leftPattern = fromString(chunk);
                leftLen = utf8Length(chunk);
            } else if (rightAnchor && !tokens.hasMoreTokens()) {
                // last item
                rightPattern = fromString(chunk);
                rightLen = utf8Length(chunk);
            } else {
                // middle items in order
                middleCheckers.add(fromString(chunk));
                lengths.add(utf8Length(chunk));
            }
        }
        midLens = ArrayUtils.toPrimitive(lengths.toArray(ArrayUtils.EMPTY_INTEGER_OBJECT_ARRAY));
        middlePatterns = middleCheckers.toArray(new BinaryStringData[0]);
        minLen = len;
        beginPattern = leftPattern;
        endPattern = rightPattern;
        beginLen = leftLen;
        endLen = rightLen;
    }

    public boolean check(BinaryStringData str) {
        MemorySegment[] segments = str.getSegments();
        int pos = str.getOffset();
        int mark = str.getSizeInBytes();
        if (str.getSizeInBytes() < minLen) {
            return false;
        }
        // prefix, extend start
        if (beginPattern != null && !checkBegin(beginPattern, segments, pos, mark)) {
            // no match
            return false;
        } else {
            pos += beginLen;
            mark -= beginLen;
        }
        // suffix, reduce len
        if (endPattern != null && !checkEnd(endPattern, segments, pos, mark)) {
            // no match
            return false;
        } else {
            // no pos change - no need since we've shrunk the string with same pos
            mark -= endLen;
        }
        // loop for middles
        for (int i = 0; i < middlePatterns.length; i++) {
            int index = indexMiddle(middlePatterns[i], segments, pos, mark);
            if (index == -1) {
                // no match
                return false;
            } else {
                mark -= index - pos + midLens[i];
                pos = index + midLens[i];
            }
        }
        // if all is good
        return true;
    }

    private static int utf8Length(String chunk) {
        return fromString(chunk).getSizeInBytes();
    }

    /** Matches the beginning of each string to a pattern. */
    private static boolean checkBegin(
            BinaryStringData pattern, MemorySegment[] segments, int start, int len) {
        int lenSub = pattern.getSizeInBytes();
        return len >= lenSub
                && SegmentsUtil.equals(pattern.getSegments(), 0, segments, start, lenSub);
    }

    /** Matches the ending of each string to its pattern. */
    private static boolean checkEnd(
            BinaryStringData pattern, MemorySegment[] segments, int start, int len) {
        int lenSub = pattern.getSizeInBytes();
        return len >= lenSub
                && SegmentsUtil.equals(
                        pattern.getSegments(), 0, segments, start + len - lenSub, lenSub);
    }

    /**
     * Matches the middle of each string to its pattern.
     *
     * @return Returns absolute offset of the match.
     */
    private static int indexMiddle(
            BinaryStringData pattern, MemorySegment[] segments, int start, int len) {
        return SegmentsUtil.find(
                segments,
                start,
                len,
                pattern.getSegments(),
                pattern.getOffset(),
                pattern.getSizeInBytes());
    }
}
