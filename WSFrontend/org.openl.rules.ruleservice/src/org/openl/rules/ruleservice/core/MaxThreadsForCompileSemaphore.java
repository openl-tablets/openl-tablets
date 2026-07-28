package org.openl.rules.ruleservice.core;

import java.util.concurrent.Semaphore;

import lombok.AccessLevel;
import lombok.Getter;

public final class MaxThreadsForCompileSemaphore {
    @Getter(AccessLevel.PRIVATE)
    private final Semaphore limitCompilationThreadsSemaphore = new Semaphore(
            RuleServiceStaticConfigurationUtil.getMaxThreadsForCompile());
    @Getter(AccessLevel.PRIVATE)
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
        var requiredSemaphore = MaxThreadsForCompileSemaphore.getInstance().getThreadsMarker().get() == null;
        try {
            if (requiredSemaphore) {
                MaxThreadsForCompileSemaphore.getInstance().getThreadsMarker().set(Thread.currentThread());
                MaxThreadsForCompileSemaphore.getInstance().getLimitCompilationThreadsSemaphore().acquire();
            }
            return callable.call();
        } finally {
            if (requiredSemaphore) {
                MaxThreadsForCompileSemaphore.getInstance().getThreadsMarker().remove();
                MaxThreadsForCompileSemaphore.getInstance().getLimitCompilationThreadsSemaphore().release();
            }
        }
    }

    public interface Callable<T> {
        T call() throws Exception;
    }
}
