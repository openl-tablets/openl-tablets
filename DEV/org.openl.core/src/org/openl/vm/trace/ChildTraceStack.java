package org.openl.vm.trace;

import java.util.ArrayList;
import java.util.List;

/**
 * This trace stack after reset removes only child trace objects (trace objects
 * added to decorated tracer will not be removed)
 * 
 * @author NSamatov
 * 
 */
public class ChildTraceStack implements TraceStack {
    private final TraceStack tracer;
    private int pushed = 0;
    private final List<TraceListener> traceListeners = new ArrayList<TraceListener>();
    private final ChildTraceListener traceListenerForDelegate = new ChildTraceListener();

    public ChildTraceStack(TraceStack tracer) {
        this.tracer = tracer;

        if (tracer instanceof ChildTraceStack) {
            ((ChildTraceStack) tracer).addListener(traceListenerForDelegate);
        }
    }

    @Override
    public void push(ITracerObject tracerObject) {
        traceListenerForDelegate.setEnabled(false);
        tracer.push(tracerObject);
        traceListenerForDelegate.setEnabled(true);

        pushed++;

        for (TraceListener traceListener : traceListeners) {
            traceListener.onPush();
        }
    }

    @Override
    public void pop() {
        traceListenerForDelegate.setEnabled(false);

        tracer.pop();
        traceListenerForDelegate.setEnabled(true);

        pushed--;

        for (TraceListener traceListener : traceListeners) {
            traceListener.onPop();
        }
    }

    @Override
    public void reset() {
        while (pushed > 0) {
            pop();
        }
    }

    public void addListener(TraceListener traceListener) {
        traceListeners.add(traceListener);
    }

    public void removeListener(TraceListener traceListener) {
        traceListeners.remove(traceListener);
    }

    private class ChildTraceListener implements TraceListener {
        private boolean enabled = true;

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void onPush() {
            if (enabled) {
                pushed++;
            }
        }

        @Override
        public void onPop() {
            if (enabled) {
                pushed--;
            }
        }
    }
}
