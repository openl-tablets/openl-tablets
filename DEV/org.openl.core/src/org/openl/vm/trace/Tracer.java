/**
 * Created Dec 3, 2006
 */
package org.openl.vm.trace;

import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * @author Yury Molchan
 */
public class Tracer {
    protected static Tracer instance = new Tracer();

    protected void doPut(Object source, String id, Object... args) {
        // Nothing
    }

    protected void doBegin(ITracerObject obj) {
        // Nothing
    }

    protected void doEnd() {
        // Nothing
    }

    protected Object doInvoke(Invokable executor, Object target, Object[] params, IRuntimeEnv env, Object source) {
        return executor.invoke(target, params, env);
    }

    protected boolean isOn() {
        return false;
    }

    public static void put(Object source, String id, Object... args) {
        instance.doPut(source, id, args);
    }

    public static Object invoke(Invokable executor, Object target, Object[] params, IRuntimeEnv env, Object source) {
        return instance.doInvoke(executor, target, params, env, source);
    }

    public static void begin(ITracerObject obj) {
        instance.doBegin(obj);
    }

    public static void end() {
        instance.doEnd();
    }

    public static boolean isTracerOn() {
        return instance.isOn();
    }
}
