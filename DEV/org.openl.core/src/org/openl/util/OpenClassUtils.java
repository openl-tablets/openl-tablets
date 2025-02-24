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

    /**
     * The method calculates the dimension of the given type.
     *
     * @param type The type to calculate the dimension. If the parameter is null, the method fails with NPE.
     * @return the dimension
     */
    public static int getDimension(IOpenClass type) {
        int dim = 0;
        while (type.isArray()) {
            type = type.getComponentClass();
            dim++;
        }
        return dim;
    }

    /**
     * If provided open class is a primitive then the method returns wrapper class for the provided primitive class,
     * otherwise returns provided object as input parameter.
     *
     * @param openClass the open class
     * @return
     */
    public static IOpenClass toWrapperIfPrimitive(IOpenClass openClass) {
        if (openClass.getInstanceClass() != null && openClass.getInstanceClass().isPrimitive()) {
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(openClass.getInstanceClass()));
        }
        return openClass;
    }

    public static boolean isVoid(IOpenClass type) {
        return type == JavaOpenClass.VOID || type == JavaOpenClass.CLS_VOID;
    }
}
