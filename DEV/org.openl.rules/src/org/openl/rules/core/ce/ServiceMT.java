package org.openl.rules.core.ce;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.vm.ce.SimpleRulesRuntimeEnvMT;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleRuntimeEnv;
import org.openl.vm.Tracer;

public final class ServiceMT {

    // Virtual threads for parallel rule evaluation, named for thread-dump/leak diagnostics. They do not inherit
    // thread-locals: tasks carry their context in the runtime environment, not via the caller's thread-locals (the
    // former ForkJoinPool workers did not propagate them either).
    private static final ThreadFactory THREAD_FACTORY = Thread.ofVirtual()
            .name("openl-rules-mt-", 0)
            .inheritInheritableThreadLocals(false)
            .factory();

    private volatile ExecutorService executor;

    private static class ServiceMTHolder {
        private static final ServiceMT INSTANCE = new ServiceMT();
    }

    public static ServiceMT getInstance() {
        return ServiceMTHolder.INSTANCE;
    }

    private ExecutorService executor() {
        ExecutorService service = executor;
        if (service == null || service.isShutdown()) {
            synchronized (this) {
                service = executor;
                if (service == null || service.isShutdown()) {
                    // One virtual thread per task, intentionally unbounded: parallel evaluations nest, so a bounded
                    // pool would deadlock with every worker blocked on its own children. Carrier threads still cap
                    // real CPU parallelism at the processor count.
                    service = Executors.newThreadPerTaskExecutor(THREAD_FACTORY);
                    executor = service;
                }
            }
        }
        return service;
    }

    /**
     * Stops the parallel-execution service and waits briefly for its in-flight tasks to finish.
     *
     * <p>Each parallel rule evaluation runs on its own virtual thread, so there are no long-lived worker threads. Call
     * this when the owning context is closed (e.g. on web application undeploy) to stop accepting new tasks and let the
     * running ones complete, cancelling any that overrun the grace period, so no rule logic keeps executing after the
     * context is gone. A later {@link #execute(IRuntimeEnv, Runnable)} lazily recreates the service.
     */
    public void shutdown() {
        ExecutorService service;
        synchronized (this) {
            service = executor;
            executor = null;
        }
        if (service == null) {
            return;
        }
        service.shutdown();
        try {
            if (!service.awaitTermination(10, TimeUnit.SECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void execute(IRuntimeEnv env, Runnable runnable) {
        if (Tracer.isEnabled()) { // Avoid parallelism for tracing
            runnable.run(env);
            return;
        }
        SimpleRuntimeEnv simpleRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
        ParallelTask task = new ParallelTask(runnable,
                simpleRuntimeEnv,
                Thread.currentThread().getContextClassLoader());
        try {
            simpleRuntimeEnv.pushAction(executor().submit(task));
        } catch (RejectedExecutionException e) {
            // The service can be shut down concurrently (e.g. on context close) between executor() and submit(). Run
            // the task inline so it still completes and join() has nothing left to wait on.
            task.run();
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

    private record ParallelTask(Runnable runnable, SimpleRuntimeEnv env, ClassLoader classLoader)
            implements java.lang.Runnable {

        @Override
        public void run() {
            final Thread currentThread = Thread.currentThread();
            final ClassLoader oldClassLoader = currentThread.getContextClassLoader();
            try {
                currentThread.setContextClassLoader(classLoader);
                if (env instanceof SimpleRulesRuntimeEnvMT) {
                    runnable.run(env.clone());
                } else {
                    runnable.run(new SimpleRulesRuntimeEnvMT(env));
                }
            } finally {
                currentThread.setContextClassLoader(oldClassLoader);
            }
        }
    }
}
