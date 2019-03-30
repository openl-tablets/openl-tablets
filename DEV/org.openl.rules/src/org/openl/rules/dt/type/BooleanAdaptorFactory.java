package org.openl.rules.dt.type;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

public class BooleanAdaptorFactory {
    
    private BooleanAdaptorFactory() {
    }

    private static final String BOOLEAN_VALUE = "booleanValue";

    public static BooleanTypeAdaptor getAdaptor(IOpenClass openClass) {

        if (openClass.getInstanceClass() == boolean.class || openClass.getInstanceClass() == Boolean.class) {
            return new BooleanTypeAdaptor();
        }

        IOpenMethod method = openClass.getMethod(BOOLEAN_VALUE, IOpenClass.EMPTY);
        if (method != null) {
            return new BooleanMethodAdaptor(method);
        }

        IOpenField field = openClass.getField(BOOLEAN_VALUE, true);

        if (field != null) {
            return new BooleanFieldAdaptor(field);
        }

        return null;
    }
}
