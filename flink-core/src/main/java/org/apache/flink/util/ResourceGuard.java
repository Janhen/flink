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

package org.apache.flink.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 此类是具有以下不变量的共享资源的警卫。多个客户端可以通过 {@link #acquireResource()} 调用并行获取资源。作为调用的
 * 结果，每个客户都会获得一个 {@link Lease}。租约的 {@link #close()} 方法释放资源并减少 {@link ResourceGuard}
 * 对象中的客户端数量。受保护的资源只能在相应的资源守卫成功关闭后才被释放，但只有在所有获得受保护资源租用的客户端都已释放
 * 该受保护资源后，才能关闭守卫。在这发生之前，对 {@link #close()} 的调用将被阻塞，直到触发零开放租赁条件。资源守卫关
 * 闭后，对 {@link #acquireResource()} 的调用将失败并出现异常。请注意，显然客户端有责任在使用后释放资源。所有客户端
 * 都被认为是平等的，即只有一个全局计数维护了资源被获取的次数，而不是由谁获取。
 *
 * This class is a guard for shared resources with the following invariants. The resource can be
 * acquired by multiple clients in parallel through the {@link #acquireResource()} call. As a result
 * of the call, each client gets a {@link Lease}. The {@link #close()} method of the lease releases
 * the resources and reduces the client count in the {@link ResourceGuard} object. The protected
 * resource should only be disposed once the corresponding resource guard is successfully closed,
 * but the guard can only be closed once all clients that acquired a lease for the guarded resource
 * have released it. Before this is happened, the call to {@link #close()} will block until the
 * zero-open-leases condition is triggered. After the resource guard is closed, calls to {@link
 * #acquireResource()} will fail with exception. Notice that, obviously clients are responsible to
 * release the resource after usage. All clients are considered equal, i.e. there is only a global
 * count maintained how many times the resource was acquired but not by whom.
 */
public class ResourceGuard implements AutoCloseable, Serializable {

    private static final long serialVersionUID = 1L;

    /** The object that serves as lock for count and the closed-flag. */
    // 用作计数和关闭标志锁的对象。
    private final SerializableObject lock;

    /** Number of clients that have ongoing access to the resource. */
    // 持续访问资源的客户端数量。
    private volatile int leaseCount;

    /** This flag indicated if it is still possible to acquire access to the resource. */
    private volatile boolean closed;

    public ResourceGuard() {
        this.lock = new SerializableObject();
        this.leaseCount = 0;
        this.closed = false;
    }

    /**
     * Acquired access from one new client for the guarded resource.
     *
     * @throws IOException when the resource guard is already closed.
     */
    public Lease acquireResource() throws IOException {

        synchronized (lock) {
            if (closed) {
                throw new IOException("Resource guard was already closed.");
            }

            ++leaseCount;
        }

        return new Lease();
    }

    /**
     * Releases access for one client of the guarded resource. This method must only be called after
     * a matching call to {@link #acquireResource()}.
     */
    private void releaseResource() {

        synchronized (lock) {
            --leaseCount;

            if (closed && leaseCount == 0) {
                lock.notifyAll();
            }
        }
    }

    public void closeInterruptibly() throws InterruptedException {
        synchronized (lock) {
            closed = true;

            while (leaseCount > 0) {
                lock.wait();
            }
        }
    }

    /**
     * If the current thread is {@linkplain Thread#interrupt interrupted} while waiting for the
     * close method to complete, then it will continue to wait. When the thread does return from
     * this method its interrupt status will be set.
     */
    @SuppressWarnings("WeakerAccess")
    public void closeUninterruptibly() {

        boolean interrupted = false;
        synchronized (lock) {
            closed = true;

            while (leaseCount > 0) {

                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Closed the resource guard. This method will block until all calls to {@link
     * #acquireResource()} have seen their matching call to {@link #releaseResource()}.
     */
    @Override
    public void close() {
        closeUninterruptibly();
    }

    /** Returns true if the resource guard is closed, i.e. after {@link #close()} was called. */
    public boolean isClosed() {
        return closed;
    }

    /** Returns the current count of open leases. */
    public int getLeaseCount() {
        return leaseCount;
    }

    /**
     * A lease is issued by the {@link ResourceGuard} as result of calls to {@link
     * #acquireResource()}. The owner of the lease can release it via the {@link #close()} call.
     */
    public class Lease implements AutoCloseable {

        private final AtomicBoolean closed;

        private Lease() {
            this.closed = new AtomicBoolean(false);
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                releaseResource();
            }
        }
    }
}
