package org.openl.vm.trace;

public interface TraceListener {
    void onPush();
    void onPop();
}
