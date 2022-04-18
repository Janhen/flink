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

import org.apache.flink.annotation.Internal;

/**
 * 该接口将类型标记为在遍历期间可访问。中心方法<i>accept(…)<i>包含了关于如何在可访问对象上调用所提供的
 * {@link Visitor}以及如何进一步遍历的逻辑。
 *
 * <p>这个概念使其易于实现，例如在遍历过程中使用不同类型的逻辑对树或DAG进行深度优先遍历。<i>accept(…)<i>方法调用
 *   访问器，然后将访问器发送给它的子代(或前辈)。使用不同类型的访问者，可以在遍历过程中执行不同的操作，而实际的遍历
 *   代码只编写一次。
 *
 * This interface marks types as visitable during a traversal. The central method <i>accept(...)</i>
 * contains the logic about how to invoke the supplied {@link Visitor} on the visitable object, and
 * how to traverse further.
 *
 * <p>This concept makes it easy to implement for example a depth-first traversal of a tree or DAG
 * with different types of logic during the traversal. The <i>accept(...)</i> method calls the
 * visitor and then send the visitor to its children (or predecessors). Using different types of
 * visitors, different operations can be performed during the traversal, while writing the actual
 * traversal code only once.
 *
 * @see Visitor
 */
@Internal
public interface Visitable<T extends Visitable<T>> {

    /**
     * 包含调用访问者并继续遍历的逻辑。通常调用访问器的访问前方法，然后将访问器发送给子节点(或前节点)，然后调用访问后方法。
     *
     * Contains the logic to invoke the visitor and continue the traversal. Typically invokes the
     * pre-visit method of the visitor, then sends the visitor to the children (or predecessors) and
     * then invokes the post-visit method.
     *
     * <p>A typical code example is the following:
     *
     * <pre>{@code
     * public void accept(Visitor<Operator> visitor) {
     *     boolean descend = visitor.preVisit(this);
     *     if (descend) {
     *         if (this.input != null) {
     *             this.input.accept(visitor);
     *         }
     *         visitor.postVisit(this);
     *     }
     * }
     * }</pre>
     *
     * @param visitor The visitor to be called with this object as the parameter.
     * @see Visitor#preVisit(Visitable)
     * @see Visitor#postVisit(Visitable)
     */
    void accept(Visitor<T> visitor);
}
