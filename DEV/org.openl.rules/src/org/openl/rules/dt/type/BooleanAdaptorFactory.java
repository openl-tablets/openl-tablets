package org.openl.rules.dt.type;

import org.openl.types.IOpenClass;

public final class BooleanAdaptorFactory {

    private BooleanAdaptorFactory() {
    }

    private static final String BOOLEAN_VALUE = "booleanValue";

    public static BooleanTypeAdaptor getAdaptor(IOpenClass openClass) {

        if (boolean.class == openClass.getInstanceClass() || Boolean.class == openClass.getInstanceClass()) {
            return new BooleanTypeAdaptor();
        }

        var method = openClass.getMethod(BOOLEAN_VALUE, IOpenClass.EMPTY);
        if (method != null) {
            return new BooleanMethodAdaptor(method);
        }

        var field = openClass.getField(BOOLEAN_VALUE, true);

        if (field != null) {
            return new BooleanFieldAdaptor(field);
        }

        return null;
    }
}
