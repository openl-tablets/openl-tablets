package org.openl.rules.core.ce;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.vm.ce.SimpleRulesRuntimeEnvMT;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleRuntimeEnv;
import org.openl.vm.Tracer;

public final class ServiceMT {

    private volatile ForkJoinPool forkJoinPool;

    private static class ServiceMTHolder {
        private static final ServiceMT INSTANCE = new ServiceMT();
    }

    public static ServiceMT getInstance() {
        return ServiceMTHolder.INSTANCE;
    }

    private ForkJoinPool pool() {
        ForkJoinPool pool = forkJoinPool;
        if (pool == null || pool.isShutdown()) {
            synchronized (this) {
                pool = forkJoinPool;
                if (pool == null || pool.isShutdown()) {
                    pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
                    forkJoinPool = pool;
                }
            }
        }
        return pool;
    }

    /**
     * Shuts down the parallel-execution pool and waits briefly for its worker threads to terminate.
     *
     * <p>Call this when the owning context is closed (e.g. on web application undeploy) so the pool threads do not
     * outlive it — the servlet container reports such threads as a leak. A later {@link #execute(IRuntimeEnv, Runnable)}
     * lazily recreates the pool.
     */
    public void shutdown() {
        ForkJoinPool pool;
        synchronized (this) {
            pool = forkJoinPool;
            forkJoinPool = null;
        }
        if (pool == null) {
            return;
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void execute(IRuntimeEnv env, Runnable runnable) {
        if (Tracer.isEnabled()) { // Avoid parallelism for tracing
            runnable.run(env);
            return;
        }
        SimpleRuntimeEnv simpleRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
        RunnableRecursiveAction action = new RunnableRecursiveAction(runnable,
                simpleRuntimeEnv,
                Thread.currentThread().getContextClassLoader());
        simpleRuntimeEnv.pushAction(action);
        if (simpleRuntimeEnv instanceof SimpleRulesRuntimeEnvMT) {
            action.fork();
        } else {
            try {
                pool().execute(action);
            } catch (RejectedExecutionException e) {
                // The pool can be shut down concurrently (e.g. on context close) between pool() and execute(). Run the
                // already-pushed action inline so it still completes and join() does not block on an unscheduled task.
                action.quietlyInvoke();
            }
        }
    }

    public void join(IRuntimeEnv env) {
        SimpleRuntimeEnv simpleRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
        try {
            while (simpleRuntimeEnv.joinActionIfExists()) {
            }
        } finally {
            while (simpleRuntimeEnv.cancelActionIfExists()) {
            }
        }
    }

    private SimpleRuntimeEnv extractSimpleRulesRuntimeEnv(IRuntimeEnv env) {
        if (env instanceof TBasicContextHolderEnv holderEnv) {
            IRuntimeEnv env1 = holderEnv.getEnv();
            if (env1 instanceof TBasicContextHolderEnv) {
                return extractSimpleRulesRuntimeEnv(env1);
            } else {
                return (SimpleRuntimeEnv) env1;
            }
        } else {
            return (SimpleRuntimeEnv) env;
        }
    }

    private static class RunnableRecursiveAction extends RecursiveAction {
        private static final long serialVersionUID = -6827837658658403954L;
        private final Runnable runnable;
        private final SimpleRuntimeEnv env;
        private final ClassLoader classLoader;

        private RunnableRecursiveAction(Runnable runnable, SimpleRuntimeEnv env, ClassLoader classLoader) {
            this.runnable = runnable;
            this.env = env;
            this.classLoader = classLoader;
        }

        @Override
        protected void compute() {
            final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                if (env instanceof SimpleRulesRuntimeEnvMT) {
                    runnable.run(env.clone());
                } else {
                    runnable.run(new SimpleRulesRuntimeEnvMT(env));
                }
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
    }
}
