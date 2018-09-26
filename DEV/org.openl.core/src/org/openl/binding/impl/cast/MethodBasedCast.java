package org.openl.binding.impl.cast;

import org.openl.types.IMethodCaller;

public class MethodBasedCast implements IOpenCast {

    private IMethodCaller caller;
    private boolean implicit;
    private int distance;
    private Object nullObject;

    public MethodBasedCast(IMethodCaller caller, boolean implicit, int distance, Object nullObject) {
        this.caller = caller;
        this.implicit = implicit;
        this.distance = distance;
        this.nullObject = nullObject;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenCast#convert(java.lang.Object)
     */
    public Object convert(Object from) {
        if (from == null){
            return null;
        }
        Object[] params = new Object[] { from, nullObject };
        return caller.invoke(null, params, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass,
     *      org.openl.types.IOpenClass)
     */
    public int getDistance() {
        return distance;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenCast#isImplicit()
     */
    public boolean isImplicit() {
        return implicit;
    }

}
