package org.openl.vm;

import org.openl.types.Invokable;

/**
 * Observes rule execution: brackets table invocations and records current-line changes.
 *
 * <p>A tracer is carried by the {@link IRuntimeEnv} of the running rules and reached with
 * {@link IRuntimeEnv#getTracer()}, so callers invoke it as a normal object rather than through a static
 * facade. The base class is the no-op tracer used when nothing is tracing: it passes every call straight
 * through, so untraced rules carry no tracing cost. {@link #NONE} is the shared instance every environment
 * exposes by default, so callers never need a null check. A tracing caller attaches its own subclass with
 * {@link IRuntimeEnv#setTracer}.
 *
 * @author Yury Molchan
 */
public class Tracer {

    /** The no-op tracer: what an environment exposes when nothing is tracing. */
    public static final Tracer NONE = new Tracer();

    /** Whether this tracer actually observes execution (the no-op tracer does not). */
    public boolean isTracing() {
        return false;
    }

    public <T, E extends IRuntimeEnv, R> R invoke(Invokable<? super T, E> executor,
                                                  T target,
                                                  Object[] params,
                                                  E env,
                                                  Object source) {
        return executor.invoke(target, params, env);
    }

    public <T, E extends IRuntimeEnv> boolean resolveTraceNode(Invokable<? super T, E> executor,
                                                               T target,
                                                               Object[] params,
                                                               E env,
                                                               Object source) {
        return false;
    }

    public <T> T wrap(Object source, T target, Object arg1) {
        return target;
    }

    /*
     * The typed put() overloads let the no-op tracer take a recorded value without allocating a varargs array or
     * boxing primitives — the array is built only when a tracer is actually observing, funnelling into the single
     * overridable doPut(). put() runs many times per request, so this keeps garbage out of ordinary execution.
     */

    public final void put(Object source, String id, Object arg1) {
        if (isTracing()) {
            doPut(source, id, arg1);
        }
    }

    public final void put(Object source, String id, Object arg1, Object arg2, boolean arg3) {
        if (isTracing()) {
            doPut(source, id, arg1, arg2, arg3);
        }
    }

    public final void put(Object source, String id, Object arg1, int arg2, Object arg3) {
        if (isTracing()) {
            doPut(source, id, arg1, arg2, arg3);
        }
    }

    public final void put(Object source, String id, Object arg1, Object arg2, int arg3, Object arg4) {
        if (isTracing()) {
            doPut(source, id, arg1, arg2, arg3, arg4);
        }
    }

    protected void doPut(Object source, String id, Object... args) {
        // Nothing
    }
}
