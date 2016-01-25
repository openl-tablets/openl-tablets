package org.openl.rules.webstudio.web.trace;

import org.openl.vm.trace.ITracerObject;

import java.io.IOException;
import java.io.Writer;

public interface TracePrinter {

    String print(ITracerObject tracer) throws IOException;

    void print(ITracerObject tracer, Writer writer) throws IOException;
}
