/**
 *
 */
package org.openl.rules.tbasic.runtime.debug;

import java.util.List;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.types.IOpenClass;

/**
 * @author User
 *
 */
public class TBasicMethodTraceObject extends ATBasicTraceObjectLeaf {

    /**
     * @param traceObject
     */
    public TBasicMethodTraceObject(AlgorithmSubroutineMethod traceObject) {
        super(traceObject);
    }

    public String getDisplayName(int mode) {
        AlgorithmSubroutineMethod method = (AlgorithmSubroutineMethod) getTraceObject();

        String returnValue = "";
        IOpenClass returnType = method.getType();
        if (!returnType.isVoid()) {
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
    
    public String getType() {
        return "tbasicMethod";
    }

    @Override
    public String getUri() {
        AlgorithmSubroutineMethod method = (AlgorithmSubroutineMethod) getTraceObject();
        return method.getSourceUrl();
    }
}
