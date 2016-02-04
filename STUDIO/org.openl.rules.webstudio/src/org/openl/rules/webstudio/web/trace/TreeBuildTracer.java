/**
 * Created Dec 3, 2006
 */
package org.openl.rules.webstudio.web.trace;

import org.openl.vm.trace.ITracerObject;
import org.openl.vm.trace.SimpleTracerObject;
import org.openl.vm.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yury Molchan
 */
public final class TreeBuildTracer extends Tracer {

    private final Logger log = LoggerFactory.getLogger(TreeBuildTracer.class);
    private static ThreadLocal<ITracerObject> tree = new ThreadLocal<ITracerObject>();

    static {
        Tracer.instance = new TreeBuildTracer();
    }

    @Override
    protected void doPut(ITracerObject obj) {
        ITracerObject current = tree.get();
        current.addChild(obj);
    }

    @Override
    protected void doBegin(ITracerObject obj) {
        doPut(obj);
        tree.set(obj);
    }

    @Override
    protected void doEnd() {
        ITracerObject current = tree.get();
        if (current != null) {
            tree.set(current.getParent());
        } else {
            log.warn("Something is wrong. Current trace object is null. Can't pop trace object.");
        }
    }

    @Override
    protected boolean isOn() {
        return true;
    }

    private static ITracerObject createRoot() {
        return new SimpleTracerObject("traceroot") {

            @Override
            public String getUri() {
                return null;
            }

            public Object getResult() {
                return null;
            }
        };
    }

    public static ITracerObject initialize() {
        ITracerObject root = createRoot();
        tree.set(root);
        return root;
    }

    public static void destroy() {
        tree.set(null);
    }
}
