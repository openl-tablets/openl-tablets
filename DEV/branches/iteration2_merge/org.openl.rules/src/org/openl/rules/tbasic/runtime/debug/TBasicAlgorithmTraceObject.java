/**
 * 
 */
package org.openl.rules.tbasic.runtime.debug;

import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.Algorithm;

/**
 * @author User
 * 
 */
public class TBasicAlgorithmTraceObject extends ATableTracerNode {
    private Object result;

    /**
     * @param traceObject
     */
    public TBasicAlgorithmTraceObject(Algorithm traceObject, Object[] inputParams) {
        super(traceObject, inputParams);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.vm.ITracerObject.SimpleTracerObject#getUri()
     */
    @Override
    public String getUri() {
        Algorithm algorithm = (Algorithm) getTraceObject();
        return algorithm.getSourceUrl();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.util.ITreeElement#getType()
     */
    public String getType() {
        return "tbasicAlgorithm";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.base.INamedThing#getDisplayName(int)
     */
    public String getDisplayName(int mode) {
        Algorithm algorithm = (Algorithm) getTraceObject();
        String displayName = algorithm.getHeader().getDisplayName(mode);
        return "Algorithm " + displayName;
    }

    public IGridRegion getGridRegion() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return the result
     */
    public Object getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(Object result) {
        this.result = result;
    }

}
