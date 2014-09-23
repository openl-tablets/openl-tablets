/**
 * Created Dec 3, 2006
 */
package org.openl.vm.trace;

/**
 * @author Yury Molchan
 */
public class Tracer implements TraceStack {

    private static ThreadLocal<Tracer> tracer = new ThreadLocal<Tracer>();

    private ITracerObject root;
    private ITracerObject current;

    private boolean active = true;

    private Tracer() {
        init();
    }

    public static void put(ITracerObject obj) {
        if (isTracerOn()) {
            ITracerObject current = tracer.get().current;
            current.addChild(obj);
        }
    }

    public static void begin(ITracerObject obj) {
        if (isTracerOn()) {
            put(obj);
            tracer.get().current = obj;
        }
    }

    public static void end() {
        if (isTracerOn()) {
            tracer.get().current = tracer.get().current.getParent();
        }
    }

    public static Tracer getTracer() {
        return tracer.get();
    }

    public static boolean isTracerOn() {
        return tracer.get() != null && tracer.get().active;
    }

    public static void disableTrace() {
        if (tracer.get() != null) {
            tracer.get().active = false;
        }
    }

    public static void enableTrace() {
        if (tracer.get() != null) {
            tracer.get().active = true;
        }
    }

    public static ITracerObject getRoot() {
        return tracer.get().root;
    }

    private void init() {

        root = new SimpleTracerObject() {

            public String getDisplayName(int mode) {
                return "Trace";
            }

            public String getType() {
                return "traceroot";
            }

            @Override
            public String getUri() {
                return null;
            }

            public Object getResult() {
                return null;
            }
        };
        current = root;
    }

    @Override
    public void pop() {
        current = current.getParent();
    }

    @Override
    public void push(ITracerObject obj) {
        current.addChild(obj);
        current = obj;
    }

    @Override
    public void reset() {
        init();
    }

    public static void initialize() {
        if (tracer.get() == null) {
            tracer.set(new Tracer());
        } else {
            tracer.get().init();
        }
    }

    public static void destroy() {
        tracer.set(null);
    }
}
