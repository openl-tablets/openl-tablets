package org.openl.vm;

import org.openl.types.Invokable;

/**
 * @author Yury Molchan
 */
public class Tracer {
    protected static Tracer instance = new Tracer();

    protected void doPut(Object source, String id, Object... args) {
        // Nothing
    }

    protected <T, E extends IRuntimeEnv, R> R doInvoke(Invokable<? super T, E> executor,
            T target,
            Object[] params,
            E env,
            Object source) {
        return executor.invoke(target, params, env);
    }

    protected <T> T doWrap(Object source, T target, Object[] args) {
        return target;
    }

    protected <T, E extends IRuntimeEnv> void doResolveTraceNode(Invokable<? super T, E> executor,
            T target,
            Object[] params,
            E env,
            Object source) {

    }

    /*
     * Overloaded put() methods. Varargs method is not used to avoid unnecessary temporary array creation and primitives
     * boxing. Trace methods are invoked multiple times during one request so we aim to reduce garbage when tracer is
     * not enabled.
     */

    public static void put(Object source, String id, Object arg1) {
        if (isEnabled()) {
            instance.doPut(source, id, arg1);
        }
    }

    public static void put(Object source, String id, Object arg1, int arg2, boolean arg3) {
        if (isEnabled()) {
            instance.doPut(source, id, arg1, arg2, arg3);
        }
    }

    public static void put(Object source, String id, Object arg1, Object arg2, boolean arg3) {
        if (isEnabled()) {
            instance.doPut(source, id, arg1, arg2, arg3);
        }
    }

    public static void put(Object source, String id, Object arg1, int arg2, Object arg3) {
        if (isEnabled()) {
            instance.doPut(source, id, arg1, arg2, arg3);
        }
    }

    public static void put(Object source, String id, Object arg1, Object arg2, int arg3, int arg4) {
        if (isEnabled()) {
            instance.doPut(source, id, arg1, arg2, arg3, arg4);
        }
    }

    public static void put(Object source, String id, Object arg1, Object arg2, int arg3, Object arg4) {
        if (isEnabled()) {
            instance.doPut(source, id, arg1, arg2, arg3, arg4);
        }
    }

    public boolean isOn() {
        return false;
    }

    public static boolean isEnabled() {
        return instance.isOn();
    }

    public static <T, E extends IRuntimeEnv, R> R invoke(Invokable<? super T, E> executor,
            T target,
            Object[] params,
            E env,
            Object source) {
        return instance.doInvoke(executor, target, params, env, source);
    }

    public static <T> T wrap(Object source, T target, Object arg1) {
        if (isEnabled()) {
            return instance.doWrap(source, target, new Object[] { arg1 });
        } else {
            return target;
        }
    }

    public static <T, E extends IRuntimeEnv> void resolveTraceNode(Invokable<? super T, E> executor,
            T target,
            Object[] params,
            E env,
            Object source) {
        instance.doResolveTraceNode(executor, target, params, env, source);
    }
}
