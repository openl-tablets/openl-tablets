package org.openl.rules.ruleservice.core;

import java.util.concurrent.Semaphore;

public final class MaxThreadsForCompileSemaphore {
    private final Semaphore limitCompilationThreadsSemaphore = new Semaphore(
            RuleServiceStaticConfigurationUtil.getMaxThreadsForCompile());
    private final ThreadLocal<Object> threadsMarker = new ThreadLocal<>();

    private MaxThreadsForCompileSemaphore() {
    }

    private static class MaxThreadsForCompileSemaphoreHolder {
        private static final MaxThreadsForCompileSemaphore INSTANCE = new MaxThreadsForCompileSemaphore();
    }

    public static MaxThreadsForCompileSemaphore getInstance() {
        return MaxThreadsForCompileSemaphoreHolder.INSTANCE;
    }

    public <T> T run(Callable<T> callable) throws Exception {
        var requiredSemaphore = threadsMarker.get() == null;
        try {
            if (requiredSemaphore) {
                threadsMarker.set(Thread.currentThread());
                limitCompilationThreadsSemaphore.acquire();
            }
            return callable.call();
        } finally {
            if (requiredSemaphore) {
                threadsMarker.remove();
                limitCompilationThreadsSemaphore.release();
            }
        }
    }

    public interface Callable<T> {
        T call() throws Exception;
    }
}
