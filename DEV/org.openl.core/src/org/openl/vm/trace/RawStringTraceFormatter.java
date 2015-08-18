package org.openl.vm.trace;

import org.openl.base.INamedThing;
import org.openl.main.SourceCodeURLConstants;

public class RawStringTraceFormatter implements TraceFormatter {

    public String format(Iterable<ITracerObject> tracerObjects) {

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
        buffer.append(SourceCodeURLConstants.AT_PREFIX + getBaseName(tracerObject.getUri()) + "&" + SourceCodeURLConstants.OPENL + "=");

        Iterable<ITracerObject> children = tracerObject.getChildren();

        for (ITracerObject child : children) {
            buffer.append("\n");
            buffer.append(print(child, level + 1));
        }

        return buffer.toString() + "\n";
    }

    private static String getBaseName(String uri) {
        if (uri == null) {
            return null;
        }
        int winSep = uri.lastIndexOf('\\');
        int unixSep = uri.lastIndexOf('/');
        int dot = uri.lastIndexOf('.');
        int sep = winSep > unixSep ? winSep : unixSep;
        if (dot > sep) {
            return uri.substring(sep + 1, dot);
        } else {
            return uri.substring(sep + 1);
        }
    }

    private String getIndent(int level) {

        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < level; i++) {
            buffer.append("\t");
        }

        return buffer.toString();
    }

}
