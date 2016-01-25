package org.openl.rules.webstudio.web.trace;

import org.openl.vm.trace.ITracerObject;

public interface TraceFormatter {

    String format(Iterable<ITracerObject> tracerObjects);
}
