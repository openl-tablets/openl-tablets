/**
 * Created Dec 3, 2006
 */
package org.openl.vm.trace;

/**
 * @author Yury Molchan
 */
public class Tracer {
    protected static Tracer instance = new Tracer();

    protected void doPut(ITracerObject obj) {
        // Nothing
    }

    protected void doBegin(ITracerObject obj) {
        // Nothing
    }

    protected void doEnd() {
        // Nothing
    }

    protected boolean isOn() {
        return false;
    }

    public static void put(ITracerObject obj) {
        instance.doPut(obj);
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
