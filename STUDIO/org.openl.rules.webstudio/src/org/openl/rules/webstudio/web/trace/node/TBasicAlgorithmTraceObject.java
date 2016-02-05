package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.tbasic.Algorithm;

public class TBasicAlgorithmTraceObject extends ATableTracerNode {

    public TBasicAlgorithmTraceObject(Algorithm traceObject, Object[] inputParams) {
        super("tbasic", "Algorithm", traceObject, inputParams);
    }
}
