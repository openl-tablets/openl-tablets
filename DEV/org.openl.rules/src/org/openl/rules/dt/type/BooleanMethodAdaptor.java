package org.openl.rules.dt.type;

import org.openl.types.IOpenMethod;

public class BooleanMethodAdaptor extends BooleanTypeAdaptor {

    private static final Object[] NO_PARAMS = new Object[0];
    private IOpenMethod method;

    public BooleanMethodAdaptor(IOpenMethod method) {
        this.method = method;
    }

    @Override
    public boolean extractBooleanValue(Object target) {
        return (Boolean) method.invoke(target, NO_PARAMS, null);
    }

}
