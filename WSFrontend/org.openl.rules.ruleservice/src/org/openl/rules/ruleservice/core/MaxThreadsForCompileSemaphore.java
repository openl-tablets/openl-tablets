package org.openl.rules.ruleservice.core;

import java.util.concurrent.Semaphore;

public final class MaxThreadsForCompileSemaphore {
    private Semaphore limitCompilationThreadsSemaphore = new Semaphore(
        RuleServiceStaticConfigurationUtil.getMaxThreadsForCompile());
    private ThreadLocal<Object> threadsMarker = new ThreadLocal<>();

    private MaxThreadsForCompileSemaphore() {
    }

    private static class MaxThreadsForCompileSemaphoreHolder {
        private static final MaxThreadsForCompileSemaphore INSTANCE = new MaxThreadsForCompileSemaphore();
    }

    public static MaxThreadsForCompileSemaphore getInstance() {
        return MaxThreadsForCompileSemaphoreHolder.INSTANCE;
    }

    private Semaphore getLimitCompilationThreadsSemaphore() {
        return limitCompilationThreadsSemaphore;
    }

    private ThreadLocal<Object> getThreadsMarker() {
        return threadsMarker;
    }

    public <T> T run(Callable<T> callable) throws Exception {
        boolean requiredSemaphore = MaxThreadsForCompileSemaphore.getInstance().getThreadsMarker().get() == null;
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
