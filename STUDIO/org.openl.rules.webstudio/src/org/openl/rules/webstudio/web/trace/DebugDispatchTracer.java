package org.openl.rules.webstudio.web.trace;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.webstudio.web.trace.debug.DebugHook;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

/**
 * The installed {@link Tracer} for OpenL Studio: routes each traced table invocation to an interactive
 * debug hook.
 *
 * <p>When a thread runs a debug session, {@link #enableDebug} binds a {@link DebugHook} to it and every
 * traced invocation is forwarded to that hook. Otherwise invocations pass straight through, so execution
 * outside a debug session carries no tracing overhead.
 *
 * <p>Installed once as {@link Tracer#instance}.
 *
 * @author Yury Molchan
 */
public final class DebugDispatchTracer extends Tracer {

    /**
     * When set on a thread, that thread runs an interactive debug session: invocations are routed to the
     * hook. See {@link DebugHook} and the {@code web.trace.debug} package.
     */
    private static final ThreadLocal<DebugHook> debugHook = new ThreadLocal<>();

    static {
        Tracer.instance = new DebugDispatchTracer();
    }

    /** Route this thread's traced invocations to the given debug hook. */
    public static void enableDebug(DebugHook hook) {
        debugHook.set(hook);
    }

    /** Stop routing this thread's invocations to a debug hook. */
    public static void disableDebug() {
        debugHook.remove();
    }

    @Override
    protected void doPut(Object source, String id, Object... args) {
        DebugHook hook = debugHook.get();
        if (hook != null) {
            hook.onPut(source, id, args);
        }
    }

    @Override
    protected <T, E extends IRuntimeEnv, R> R doInvoke(Invokable<? super T, E> executor,
                                                       T target,
                                                       Object[] params,
                                                       E env,
                                                       Object source) {
        DebugHook hook = debugHook.get();
        if (hook != null) {
            return hook.bracketInvoke(executor, target, params, env, source);
        }
        return executor.invoke(target, params, env);
    }

    @Override
    protected <T, E extends IRuntimeEnv> boolean doResolveTraceNode(Invokable<? super T, E> executor,
                                                                    T target,
                                                                    Object[] params,
                                                                    E env,
                                                                    Object source) {
        DebugHook hook = debugHook.get();
        return hook != null && hook.onResolveNode(executor);
    }

    @Override
    protected <T> T doWrap(Object source, T target, Object[] args) {
        // In debug mode, wrap int selectors so a decision table's per-rule condition checks
        // (success/failure) are recorded via Tracer.put.
        if (debugHook.get() != null && target instanceof IIntSelector selector) {
            return (T) new IntSelectorTracer(selector, (ICondition) args[0]);
        }
        return target;
    }

    @Override
    public boolean isOn() {
        return debugHook.get() != null;
    }
}
