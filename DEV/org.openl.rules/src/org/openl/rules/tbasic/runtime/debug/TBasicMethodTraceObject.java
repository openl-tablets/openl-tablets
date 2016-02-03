package org.openl.rules.tbasic.runtime.debug;

import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;

public class TBasicMethodTraceObject extends ATableTracerNode {

    public TBasicMethodTraceObject(AlgorithmSubroutineMethod method) {
        super("tbasicMethod", "Algorithm Method", method, null);
    }

    @Override
    public String getUri() {
        return getTraceObject().getSourceUrl();
    }
}
