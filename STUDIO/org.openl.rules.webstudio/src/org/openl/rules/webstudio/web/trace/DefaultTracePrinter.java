package org.openl.rules.webstudio.web.trace;

import org.openl.vm.trace.ITracerObject;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class DefaultTracePrinter implements TracePrinter {

    private TraceFormatter formatter = new RawStringTraceFormatter();

    public TraceFormatter getFormatter() {
        return formatter;
    }

    public void setFormatter(TraceFormatter formatter) {
        this.formatter = formatter;
    }

    public void print(ITracerObject tracer, Writer writer) throws IOException {

        Iterable<ITracerObject> tracerObjects = tracer.getChildren();
        String formattedString = formatter.format(tracerObjects);

        writer.write(formattedString);
    }

    public String print(ITracerObject tracer) throws IOException {

        StringWriter writer = new StringWriter();
        print(tracer, writer);

        return writer.toString();
    }
}
