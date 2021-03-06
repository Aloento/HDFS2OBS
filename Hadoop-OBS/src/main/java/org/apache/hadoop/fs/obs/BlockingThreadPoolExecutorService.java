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

package org.apache.hadoop.fs.obs;

import com.google.common.util.concurrent.MoreExecutors;
import com.sun.istack.NotNull;
import org.apache.hadoop.classification.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This ExecutorService blocks the submission of new tasks when its queue is
 * already full by using a semaphore. Task submissions require permits, task
 * completions release permits.
 *
 * <p>This is inspired by s4 thread pool (see https://github
 * .com/apache/incubator-s4/blob/master/subprojects/s4-comm/src/main/java/org
 * /apache/s4/comm/staging/BlockingThreadPoolExecutorService.java)
 */
@InterfaceAudience.Private
final class BlockingThreadPoolExecutorService extends SemaphoredDelegatingExecutor {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BlockingThreadPoolExecutorService.class);

    /**
     * Number of thread pools.
     */
    private static final AtomicInteger POOLNUMBER = new AtomicInteger(1);

    /**
     * Thread pool executor for event processing.
     */
    private final ThreadPoolExecutor eventProcessingExecutor;

    private BlockingThreadPoolExecutorService(final int permitCount, final ThreadPoolExecutor executor) {
        super(MoreExecutors.listeningDecorator(executor), permitCount, false);
        this.eventProcessingExecutor = executor;
    }

    /**
     * Returns a {@link java.util.concurrent.ThreadFactory} that names each
     * created thread uniquely, with a common prefix.
     *
     * @param prefix The prefix of every created Thread's name
     * @return a {@link java.util.concurrent.ThreadFactory} that names threads
     */
    private static ThreadFactory getNamedThreadFactory(final String prefix) {
        return new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            private final int poolNum = POOLNUMBER.getAndIncrement();

            @Override
            public Thread newThread(@NotNull final Runnable r) {
                final String name = prefix + "-pool" + poolNum + "-t" + threadNumber.getAndIncrement();
                return new Thread(r, name);
            }
        };
    }

    /**
     * Get a named {@link ThreadFactory} that just builds daemon threads.
     *
     * @param prefix name prefix for all threads created from the factory
     * @return a thread factory that creates named, daemon threads with the
     * supplied exception handler and normal priority
     */
    static ThreadFactory newDaemonThreadFactory(final String prefix) {
        final ThreadFactory namedFactory = getNamedThreadFactory(prefix);
        return r -> {
            Thread t = namedFactory.newThread(r);
            if (!t.isDaemon()) {
                t.setDaemon(true);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        };
    }

    /**
     * A thread pool that that blocks clients submitting additional tasks if
     * there are already {@code activeTasks} running threads and {@code
     * waitingTasks} tasks waiting in its queue.
     *
     * @param activeTasks   maximum number of active tasks
     * @param waitingTasks  maximum number of waiting tasks
     * @param keepAliveTime time until threads are cleaned up in seconds
     * @param prefixName    prefix of name for threads
     * @return new instance of BlockingThreadPoolExecutorService
     */
    static BlockingThreadPoolExecutorService newInstance(final int activeTasks, final int waitingTasks,
                                                         final long keepAliveTime, final String prefixName) {

    /* Although we generally only expect up to waitingTasks tasks in the
    queue, we need to be able to buffer all tasks in case dequeueing is
    slower than enqueueing. */
        final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(waitingTasks + activeTasks);
        ThreadPoolExecutor eventProcessingExecutor = new ThreadPoolExecutor(activeTasks, activeTasks, keepAliveTime,
                TimeUnit.SECONDS, workQueue, newDaemonThreadFactory(prefixName), (r, executor) -> {
            // This is not expected to happen.
            LOG.error("Could not submit task to executor {}", executor.toString());
        });
        eventProcessingExecutor.allowCoreThreadTimeOut(true);
        return new BlockingThreadPoolExecutorService(waitingTasks + activeTasks, eventProcessingExecutor);
    }

    /**
     * Get the actual number of active threads.
     *
     * @return the active thread count
     */
    private int getActiveCount() {
        return eventProcessingExecutor.getActiveCount();
    }

    @Override
    public String toString() {
        return "BlockingThreadPoolExecutorService{" + super.toString() + ", activeCount=" + getActiveCount() + '}';
    }
}
