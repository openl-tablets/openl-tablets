package org.openl.rules.core.ce;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.vm.SimpleRuntimeEnv;
import org.openl.rules.vm.ce.SimpleRulesRuntimeEnvMT;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

public final class ServiceMT {

    private final ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

    private static class ServiceMTHolder {
        private static final ServiceMT INSTANCE = new ServiceMT();
    }

    public static ServiceMT getInstance() {
        return ServiceMTHolder.INSTANCE;
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
            forkJoinPool.execute(action);
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
        if (env instanceof TBasicContextHolderEnv) {
            IRuntimeEnv env1 = ((TBasicContextHolderEnv) env).getEnv();
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
