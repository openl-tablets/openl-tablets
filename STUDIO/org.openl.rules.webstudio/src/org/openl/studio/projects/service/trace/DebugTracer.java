package org.openl.studio.projects.service.trace;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

/**
 * A {@link Tracer} that forwards every traced table invocation to an interactive {@link DebugHook}.
 *
 * <p>One instance holds the hook for one debug session. It is attached to that run's {@link IRuntimeEnv}
 * (via {@link IRuntimeEnv#setTracer}) and reached from there, so no thread-local state is needed and rules
 * that run without it attached carry no tracing overhead. Because it is attached only while tracing, every
 * callback here runs in a traced context and the hook is always present.
 *
 * @author Yury Molchan
 */
public final class DebugTracer extends Tracer {

    private final DebugHook hook;

    public DebugTracer(DebugHook hook) {
        this.hook = hook;
    }

    @Override
    public boolean isTracing() {
        return true;
    }

    @Override
    public <T, E extends IRuntimeEnv, R> R invoke(Invokable<? super T, E> executor,
                                                  T target,
                                                  Object[] params,
                                                  E env,
                                                  Object source) {
        return hook.bracketInvoke(executor, target, params, env, source);
    }

    @Override
    public <T, E extends IRuntimeEnv> boolean resolveTraceNode(Invokable<? super T, E> executor,
                                                               T target,
                                                               Object[] params,
                                                               E env,
                                                               Object source) {
        return hook.onResolveNode(executor);
    }

    @Override
    public <T> T wrap(Object source, T target, Object arg1) {
        // Wrap int selectors so a decision table's per-rule condition checks (success/failure) are recorded.
        if (target instanceof IIntSelector selector) {
            return (T) new IntSelectorTracer(selector, (ICondition) arg1, hook);
        }
        return target;
    }

    @Override
    protected void doPut(Object source, String id, Object... args) {
        hook.onPut(source, id, args);
    }
}
