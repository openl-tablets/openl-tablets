package org.openl.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public final class ArrayUtils {

    private ArrayUtils() {
    }

    public static boolean deepEquals(Object e1, Object e2) {
        boolean eq;
        if (e1 instanceof Object[] && e2 instanceof Object[]) {
            eq = Arrays.deepEquals((Object[]) e1, (Object[]) e2);
        } else if (e1 instanceof byte[] && e2 instanceof byte[]) {
            eq = Arrays.equals((byte[]) e1, (byte[]) e2);
        } else if (e1 instanceof short[] && e2 instanceof short[]) {
            eq = Arrays.equals((short[]) e1, (short[]) e2);
        } else if (e1 instanceof int[] && e2 instanceof int[]) {
            eq = Arrays.equals((int[]) e1, (int[]) e2);
        } else if (e1 instanceof long[] && e2 instanceof long[]) {
            eq = Arrays.equals((long[]) e1, (long[]) e2);
        } else if (e1 instanceof char[] && e2 instanceof char[]) {
            eq = Arrays.equals((char[]) e1, (char[]) e2);
        } else if (e1 instanceof float[] && e2 instanceof float[]) {
            eq = Arrays.equals((float[]) e1, (float[]) e2);
        } else if (e1 instanceof double[] && e2 instanceof double[]) {
            eq = Arrays.equals((double[]) e1, (double[]) e2);
        } else if (e1 instanceof boolean[] && e2 instanceof boolean[]) {
            eq = Arrays.equals((boolean[]) e1, (boolean[]) e2);
        } else {
            eq = Objects.equals(e1, e2);
        }
        return eq;
    }

    /**
     * Repacks an array to the given class
     *
     * @param o             array
     * @param expectedClass classTo
     * @return transformed array if it's possible to convert
     */
    public static Object repackArray(Object o, Class<?> expectedClass) {
        if (o == null) {
            return null;
        }
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
                .isAssignableFrom(returnType) && dim1 == dim2 && dim1 > 0) {
            return convert(o, expectedClass.getComponentType(), dim1);
        } else {
            return o;
        }
    }

    public static Object convert(Object o, Function<Object, Object> converter) {
        if (o == null || !o.getClass().isArray()) {
            return converter.apply(o);
        }
        int size = Array.getLength(o);
        var cache = new Object[size];
        Class<?> componentType = null;
        for (int i = 0; i < size; i++) {
            var element = Array.get(o, i);
            element = convert(element, converter);
            cache[i] = element;
            componentType = ClassUtils.commonType(componentType, element != null ? element.getClass() : null);
        }
        if (componentType == null || componentType.equals(Object.class)) {
            return cache;
        }
        var result = Array.newInstance(componentType, size);
        System.arraycopy(cache, 0, result, 0, size);
        return result;
    }

    private static Object convert(Object o, Class<?> newType, int dimension) {
        int size = Array.getLength(o);
        Object result = Array.newInstance(newType, size);
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
