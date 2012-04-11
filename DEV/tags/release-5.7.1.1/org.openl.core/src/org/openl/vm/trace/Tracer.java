/**
 * Created Dec 3, 2006
 */
package org.openl.vm.trace;

import java.io.PrintStream;
import java.util.Stack;

import org.openl.base.INamedThing;
import org.openl.main.SourceCodeURLConstants;

/**
 * @author snshor
 * 
 */
public class Tracer {

    private static ThreadLocal<Tracer> tracer = new ThreadLocal<Tracer>();

    private Stack<ITracerObject> stack = new Stack<ITracerObject>();
    private ITracerObject root;

    public Tracer() {
        init();
    }

    public static Tracer getTracer() {
        return tracer.get();
    }

    public static boolean isTracerOn() {
        return tracer.get() != null;
    }

    public static void setTracer(Tracer t) {
        tracer.set(t);
    }

    public ITracerObject getRoot() {
        return root;
    }

    public void setRoot(ITracerObject root) {
        this.root = root;
    }

    public ITracerObject[] getTracerObjects() {
        return root.getTracerObjects();
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
        };
    }

    private void addTracerObject(ITracerObject to) {
        root.addChild(to);
    }

    public void pop() {
        stack.pop();
    }

    public void push(ITracerObject obj) {
        if (stack.size() == 0) {
            addTracerObject(obj);
        } else {
            ITracerObject to = stack.peek();
            to.addChild(obj);
            obj.setParent(to);
        }

        stack.push(obj);
    }

    public void reset() {
        init();
    }
//
//    public void print(PrintStream ps) {
//        ITracerObject[] tt = getTracerObjects();
//        for (int i = 0; i < tt.length; i++) {
//            printTO(tt[i], 0, ps);
//        }
//    }
//
//    public void printTO(ITracerObject to, int level, PrintStream ps) {
//        for (int i = 0; i < level * 2; i++) {
//            ps.print(' ');
//        }
//
//        ps.println("TRACE: " + to.getDisplayName(INamedThing.REGULAR));
//        ps.println(SourceCodeURLConstants.AT_PREFIX + to.getUri() + "&" + SourceCodeURLConstants.OPENL + "=");
//
//        ITracerObject[] tt = to.getTracerObjects();
//
//        for (int i = 0; i < tt.length; i++) {
//            printTO(tt[i], level + 1, ps);
//        }
//    }

}
