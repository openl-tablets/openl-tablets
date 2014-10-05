package org.openl.vm.trace;

public interface TraceFormatter {

    String format(Iterable<ITracerObject> tracerObjects);
}
