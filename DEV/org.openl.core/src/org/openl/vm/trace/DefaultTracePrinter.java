package org.openl.vm.trace;

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

    public void print(Tracer tracer, Writer writer) throws IOException {

        ITracerObject[] tracerObjects = tracer.getTracerObjects();
        String formattedString = formatter.format(tracerObjects);
        
        writer.write(formattedString);
    }

    public String print(Tracer tracer) throws IOException {
        
        StringWriter writer = new StringWriter();
        print(tracer, writer);
        
        return writer.toString();
    }

}
