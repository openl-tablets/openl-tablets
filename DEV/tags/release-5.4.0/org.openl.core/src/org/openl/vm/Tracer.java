/**
 * Created Dec 3, 2006
 */
package org.openl.vm;

import java.io.PrintStream;
import java.util.Stack;

import org.openl.base.INamedThing;
import org.openl.main.SourceCodeURLConstants;

/**
 * @author snshor
 *
 */
public class Tracer {

    static ThreadLocal<Tracer> tracer = new ThreadLocal<Tracer>();

    Stack<ITracerObject> stack = new Stack<ITracerObject>();

    ITracerObject root = makeRoot();

    static public Tracer getTracer() {
        return tracer.get();
    }

    static public boolean isTracerOn() {
        return tracer.get() != null;
    }

    static public void setTracer(Tracer t) {
        tracer.set(t);
    }

    void addTracerObject(ITracerObject to) {
        root.addChild(to);
    }

    // ArrayList list = new ArrayList(100);

    public ITracerObject getRoot() {
        return root;
    }

    public ITracerObject[] getTracerObjects() {

        return root.getTracerObjects();
    }

    /**
     * @return
     */
    private ITracerObject makeRoot() {
        return new ITracerObject.SimpleTracerObject() {

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

    /**
     *
     */
    public void pop() {
        stack.pop();
    }

    public void print(PrintStream ps) {
        ITracerObject[] tt = getTracerObjects();
        for (int i = 0; i < tt.length; i++) {
            printTO(tt[i], 0, ps);
        }
    }

    public void printTO(ITracerObject to, int level, PrintStream ps) {
        for (int i = 0; i < level * 2; i++) {
            ps.print(' ');
        }

        ps.println("TRACE: " + to.getDisplayName(INamedThing.REGULAR));
        ps.println(SourceCodeURLConstants.AT_PREFIX + to.getUri() + "&" + SourceCodeURLConstants.OPENL + "=");

        ITracerObject[] tt = to.getTracerObjects();

        for (int i = 0; i < tt.length; i++) {
            printTO(tt[i], level + 1, ps);
        }

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
        root = makeRoot();
    }

    public void setRoot(ITracerObject root) {
        this.root = root;
    }

}
