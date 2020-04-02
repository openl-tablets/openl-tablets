package org.openl.util;

import java.lang.reflect.Array;

public class RepackArrayUtils {

    /**
     * Repacks an array to the given class
     * 
     * @param o array
     * @param expectedClass classTo
     * @return transformed array if it's possible to convert
     */
    public static Object repackArray(Object o, Class<?> expectedClass) {
        Class<?> returnType = o.getClass();
        int dim1 = 0;
        while (returnType.isArray()) {
            returnType = returnType.getComponentType();
            dim1++;
        }
        int dim2 = 0;
        Class<?> expectedType = expectedClass;
        while (expectedType.isArray()) {
            expectedType = expectedType.getComponentType();
            dim2++;
        }
        if (!returnType.equals(expectedType) && o.getClass().isArray() && expectedType
            .isAssignableFrom(returnType) && dim1 == dim2) {
            return convert(o, expectedClass);
        } else {
            return o;
        }
    }

    public static Object convert(Object o, Class<?> newType) {
        int dimension = getDimension(o);
        return convert(o, newType, dimension);
    }

    public static Object convert(Object o, Class<?> newType, int dimension) {
        if (dimension == 0) {
            return Array.newInstance(newType, 0);
        } else {
            int size = Array.getLength(o);
            Object result = Array.newInstance(newType.getComponentType(), size);
            if (dimension == 1) {
                for (int i = 0; i < size; i++) {
                    Array.set(result, i, Array.get(o, i));
                }
            } else {
                for (int i = 0; i < size; i++) {
                    Array.set(result, i, convert(Array.get(o, i), newType.getComponentType(), dimension - 1));
                }
            }
            return result;
        }
    }

    public static int getDimension(Object ndArray) {
        int dim = 0;
        Class<?> aClass = ndArray.getClass();
        while (aClass.isArray()) {
            aClass = aClass.getComponentType();
            dim++;
        }
        return dim;
    }
}
