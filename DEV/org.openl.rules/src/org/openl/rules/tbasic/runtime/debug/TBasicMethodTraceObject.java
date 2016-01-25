package org.openl.rules.tbasic.runtime.debug;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

import java.util.List;

public class TBasicMethodTraceObject extends ATBasicTraceObjectLeaf {

    private AlgorithmSubroutineMethod method;

    public TBasicMethodTraceObject(AlgorithmSubroutineMethod method) {
        super("tbasicMethod");
        this.method = method;
    }

    public AlgorithmSubroutineMethod getMethod() {
        return method;
    }

    public List<IGridRegion> getGridRegions() {
        // regions of sub-elements should be combined
        return null;
    }

    @Override
    public String getUri() {
        return method.getSourceUrl();
    }
}
