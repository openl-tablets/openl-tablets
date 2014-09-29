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

    public String getDisplayName(int mode) {

        String returnValue = "";
        IOpenClass returnType = method.getType();
        if (!JavaOpenClass.isVoid(returnType)) {
            returnValue = String.format("%s = %s", returnType.getDisplayName(mode), getResult() != null ? getResult().toString()
                    : "null");
        }

        String displayName = method.getHeader().getDisplayName(mode);

        return String.format("Algorithm Method %s %s", returnValue, displayName);
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
