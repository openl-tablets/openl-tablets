package org.openl.rules.tbasic.runtime.debug;

import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.tbasic.Algorithm;

public class TBasicAlgorithmTraceObject extends ATableTracerNode {

    public TBasicAlgorithmTraceObject(Algorithm traceObject, Object[] inputParams) {
        super("tbasic", "Algorithm", traceObject, inputParams);
    }
}
