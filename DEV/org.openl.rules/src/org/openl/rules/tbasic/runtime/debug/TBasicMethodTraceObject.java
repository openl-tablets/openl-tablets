package org.openl.rules.tbasic.runtime.debug;

import org.openl.rules.table.ATableTracerLeaf;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;

public class TBasicMethodTraceObject extends ATableTracerLeaf {

    private AlgorithmSubroutineMethod method;

    public TBasicMethodTraceObject(AlgorithmSubroutineMethod method) {
        super("tbasicMethod");
        this.method = method;
    }

    public AlgorithmSubroutineMethod getMethod() {
        return method;
    }

    @Override
    public String getUri() {
        return method.getSourceUrl();
    }
}
