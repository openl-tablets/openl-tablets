package org.openl.binding.impl.cast;

import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

final class MethodBasedCast implements IOpenCast {

    private final IMethodCaller caller;
    private final boolean implicit;
    private final int distance;
    private final Object nullObject;
    private final IOpenClass destType;

    MethodBasedCast(IMethodCaller caller, boolean implicit, int distance, IOpenClass destType, Object nullObject) {
        this.caller = caller;
        this.implicit = implicit;
        this.distance = distance;
        this.nullObject = nullObject;
        this.destType = destType;
    }

    @Override
    public Object convert(Object from) {
        if (from == null) {
            //WARNING:
            //do not use this.nullObject as a result
            //it's just temp result for CastOperators.autocast() methods
            //to avoid ambiguous method call and choose a right methods
            return destType.nullObject();
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
