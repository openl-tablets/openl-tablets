package org.openl.util;

import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public final class OpenClassUtils {

    private OpenClassUtils() {
    }

    public static IOpenClass getRootComponentClass(IOpenClass fieldType) {
        if (!fieldType.isArray()) {
            return fieldType;
        }
        // Get the component type of the array
        //
        return getRootComponentClass(fieldType.getComponentClass());
    }

    public static boolean isVoid(IOpenClass type) {
        return type == JavaOpenClass.VOID || type == JavaOpenClass.CLS_VOID;
    }
}
