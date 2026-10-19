package org.openl.studio.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;

import org.openl.studio.audit.service.AuditClient;

/**
 * Builds the thread pools OpenL Studio runs background work on: tests, compilations, comparisons, merges.
 *
 * <p>Every pool runs a task as the user who started it and for the client of the request that started it, so the audit
 * log names both for what the task does. A pool queues up to ten tasks before it refuses another, and lets the running
 * tasks end when the application stops. A pool is a bean of the application, which starts it and shuts it down.
 *
 * <p>A pool of this kind grows past its core threads only once its queue is full, so a pool meant to run several
 * tasks at once is sized by its core threads.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AsyncExecutors {

    private static final int QUEUE_CAPACITY = 10;
    private static final int AWAIT_TERMINATION_SECONDS = 5;

    /**
     * Builds a pool.
     *
     * @param threadNamePrefix the prefix of the names of its threads
     * @param corePoolSize     how many tasks it runs at once while its queue has room
     * @param maxPoolSize      how many tasks it runs at once when its queue is full
     * @return the pool, running each task with the security context and the client of the caller
     */
    public static ThreadPoolTaskExecutor create(String threadNamePrefix, int corePoolSize, int maxPoolSize) {
        var exec = new ThreadPoolTaskExecutor();
        exec.setThreadNamePrefix(threadNamePrefix);
        exec.setCorePoolSize(corePoolSize);
        exec.setMaxPoolSize(maxPoolSize);
        exec.setQueueCapacity(QUEUE_CAPACITY);
        exec.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setTaskDecorator(task -> new DelegatingSecurityContextRunnable(AuditClient.carry(task)));
        return exec;
    }
}
