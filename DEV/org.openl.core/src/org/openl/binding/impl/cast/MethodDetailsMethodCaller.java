package org.openl.binding.impl.cast;

import java.util.function.Function;

import org.openl.types.IMethodCaller;
import org.openl.types.impl.MethodCallerDelegator;
import org.openl.vm.IRuntimeEnv;

/**
 * This is a helper class to be used in {@link MethodCallerWrapper} implementations if additional details need to be
 * pass to a method implementation.
 * <p>
 * Missed details are put to the {@link ThreadLocal} variable and can be accessed via getMethodDetails method.
 */
public class MethodDetailsMethodCaller extends MethodCallerDelegator {

    private static class MethodDetailsHolder {
        private static final ThreadLocal<MethodDetails> METHOD_DETAILS_THREAD_LOCAL = new ThreadLocal<>();
    }

    public static MethodDetails getMethodDetails() {
        return MethodDetailsHolder.METHOD_DETAILS_THREAD_LOCAL.get();
    }

    private final Function<IRuntimeEnv, MethodDetails> func;

    public MethodDetailsMethodCaller(IMethodCaller methodCaller, Function<IRuntimeEnv, MethodDetails> func) {
        super(methodCaller);
        this.func = func;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        try {
            MethodDetailsHolder.METHOD_DETAILS_THREAD_LOCAL.set(func.apply(env));
            return super.invoke(target, params, env);
        } finally {
            MethodDetailsHolder.METHOD_DETAILS_THREAD_LOCAL.remove();
        }
    }
}