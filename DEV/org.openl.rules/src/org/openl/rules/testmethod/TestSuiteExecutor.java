package org.openl.rules.testmethod;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class TestSuiteExecutor {
    private static final int QUEUE_SIZE = 2000;
    public static final int DEFAULT_THREAD_COUNT = 4;

    private final ThreadPoolExecutor executor;
    private final int threadCount;

    public TestSuiteExecutor(int threadCount) {
        this.threadCount = threadCount;
        this.executor = new ThreadPoolExecutor(threadCount,
            threadCount,
            1L,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<Runnable>(QUEUE_SIZE),
            new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
    }

    public Executor getExecutor() {
        return executor;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void destroy() {
        executor.shutdownNow();
    }
}
