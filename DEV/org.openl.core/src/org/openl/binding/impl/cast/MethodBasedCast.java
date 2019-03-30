package org.openl.binding.impl.cast;

import org.openl.types.IMethodCaller;

final class MethodBasedCast implements IOpenCast {

    private IMethodCaller caller;
    private boolean implicit;
    private int distance;
    private Object nullObject;

    MethodBasedCast(IMethodCaller caller, boolean implicit, int distance, Object nullObject) {
        this.caller = caller;
        this.implicit = implicit;
        this.distance = distance;
        this.nullObject = nullObject;
    }

    @Override
    public Object convert(Object from) {
        if (from == null) {
            return null;
        }
        Object[] params = new Object[] { from, nullObject };
        return caller.invoke(null, params, null);
    }

    @Override
    public int getDistance() {
        return distance;
    }

    @Override
    public boolean isImplicit() {
        return implicit;
    }

}
