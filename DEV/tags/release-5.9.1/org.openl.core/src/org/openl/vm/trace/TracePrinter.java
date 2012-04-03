package org.openl.vm.trace;

import java.io.IOException;
import java.io.Writer;

public interface TracePrinter {

    String print(Tracer tracer) throws IOException;
    
    void print(Tracer tracer, Writer writer) throws IOException;
}
