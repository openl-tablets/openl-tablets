package org.openl.rules.dt.type;

import org.openl.types.IOpenMethod;

public class BooleanMethodAdaptor extends BooleanTypeAdaptor {

    private IOpenMethod method;

    public BooleanMethodAdaptor(IOpenMethod method) {
        this.method = method;
    }

    @Override
    public boolean extractBooleanValue(Object target) {
        return (Boolean) method.invoke(target, new Object[0], null);
    }

}
