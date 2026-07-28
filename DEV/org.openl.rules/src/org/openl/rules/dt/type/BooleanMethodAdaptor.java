package org.openl.rules.dt.type;

import lombok.RequiredArgsConstructor;

import org.openl.types.IOpenMethod;

@RequiredArgsConstructor
public class BooleanMethodAdaptor extends BooleanTypeAdaptor {

    private static final Object[] NO_PARAMS = new Object[0];
    private final IOpenMethod method;

    @Override
    public boolean extractBooleanValue(Object target) {
        return (Boolean) method.invoke(target, NO_PARAMS, null);
    }

}
