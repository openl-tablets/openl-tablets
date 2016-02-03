/**
 * Created Dec 3, 2006
 */
package org.openl.vm.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yury Molchan
 */
public final class Tracer {

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
            ITracerObject current = tracer.get().current;
            if (current != null) {
                tracer.get().current = current.getParent();
            } else {
                log().warn("Something is wrong. Current trace object is null. Can't pop trace object.");
            }
        }
    }

    public static boolean isTracerOn() {
        return tracer.get() != null && tracer.get().active;
    }

    public static ITracerObject getRoot() {
        return tracer.get().root;
    }

    private void init() {

        root = new SimpleTracerObject("traceroot") {

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

    private static Logger log() {
        return LoggerFactory.getLogger(Tracer.class);
    }
}
