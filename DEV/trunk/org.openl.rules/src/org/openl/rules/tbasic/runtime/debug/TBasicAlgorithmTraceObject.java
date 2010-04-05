/**
 *
 */
package org.openl.rules.tbasic.runtime.debug;

import java.util.List;

import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.Algorithm;

/**
 * @author User
 *
 */
public class TBasicAlgorithmTraceObject extends ATableTracerNode {
    /**
     * @param traceObject
     */
    public TBasicAlgorithmTraceObject(Algorithm traceObject, Object[] inputParams) {
        super(traceObject, inputParams);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getDisplayName(int)
     */
    public String getDisplayName(int mode) {
        Algorithm algorithm = (Algorithm) getTraceObject();
        return String.format("Algorithm %s", asString(algorithm, mode));
    }

    public List<IGridRegion> getGridRegions() {
        // regions of sub-elements should be combined
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.util.ITreeElement#getType()
     */
    public String getType() {
        return "tbasic";
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
}
