/**
 * 
 */
package org.openl.rules.tbasic.runtime.debug;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;

/**
 * @author User
 * 
 */
public class TBasicMethodTraceObject extends ATBasicTraceObjectLeaf {

    private Object result;

    /**
     * @param traceObject
     */
    public TBasicMethodTraceObject(AlgorithmSubroutineMethod traceObject) {
        super(traceObject);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.vm.ITracerObject.SimpleTracerObject#getUri()
     */
    @Override
    public String getUri() {
        AlgorithmSubroutineMethod method = (AlgorithmSubroutineMethod) getTraceObject();
        return method.getSourceUrl();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.util.ITreeElement#getType()
     */
    public String getType() {
        return "tbasicAlgorithmMethod";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.base.INamedThing#getDisplayName(int)
     */
    public String getDisplayName(int mode) {
        AlgorithmSubroutineMethod method = (AlgorithmSubroutineMethod) getTraceObject();
        String displayName = method.getHeader().getDisplayName(mode);
        return "Algorithm Method " + displayName;
    }

    /**
     * @param result the result to set
     */
    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * @return the result
     */
    public Object getResult() {
        return result;
    }

    public IGridRegion getGridRegion() {
        AlgorithmSubroutineMethod method = (AlgorithmSubroutineMethod) getTraceObject();
        return method.getGridRegion();
    }

}
