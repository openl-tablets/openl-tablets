package org.openl.vm.trace;

import org.apache.commons.io.FilenameUtils;
import org.openl.base.INamedThing;
import org.openl.main.SourceCodeURLConstants;

public class RawStringTraceFormatter implements TraceFormatter {

    public String format(ITracerObject[] tracerObjects) {

        StringBuilder buffer = new StringBuilder();
        
        for (ITracerObject tracerObject : tracerObjects) {
            buffer.append(print(tracerObject, 0));
        }
        
        return buffer.toString();
    }

    private String print(ITracerObject tracerObject, int level) {
        
        StringBuilder buffer = new StringBuilder();
        String indent = getIndent(level);
        
        buffer.append(indent);
        buffer.append("TRACE: " + tracerObject.getDisplayName(INamedThing.REGULAR));
        buffer.append("\n");
        buffer.append(indent);
        buffer.append(SourceCodeURLConstants.AT_PREFIX + FilenameUtils.getBaseName(tracerObject.getUri()) + "&" + SourceCodeURLConstants.OPENL + "=");

        ITracerObject[] children = tracerObject.getTracerObjects();
        
        for (ITracerObject child : children) { 
            buffer.append("\n");
            buffer.append(print(child, level + 1));
        }
            
        return buffer.toString() + "\n";
    } 
    
    private String getIndent(int level) {
        
        StringBuilder buffer = new StringBuilder();
        
        for (int i = 0; i < level; i++) {
            buffer.append("\t");
        }
        
        return buffer.toString();
    }
    
}
