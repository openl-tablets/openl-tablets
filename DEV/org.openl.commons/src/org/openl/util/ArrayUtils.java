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
        if (e1 instanceof Object[] objects && e2 instanceof Object[] objects1) {
            eq = Arrays.deepEquals(objects, objects1);
        } else if (e1 instanceof byte[] bytes && e2 instanceof byte[] bytes1) {
            eq = Arrays.equals(bytes, bytes1);
        } else if (e1 instanceof short[] shorts && e2 instanceof short[] shorts1) {
            eq = Arrays.equals(shorts, shorts1);
        } else if (e1 instanceof int[] ints && e2 instanceof int[] ints1) {
            eq = Arrays.equals(ints, ints1);
        } else if (e1 instanceof long[] longs && e2 instanceof long[] longs1) {
            eq = Arrays.equals(longs, longs1);
        } else if (e1 instanceof char[] chars && e2 instanceof char[] chars1) {
            eq = Arrays.equals(chars, chars1);
        } else if (e1 instanceof float[] floats && e2 instanceof float[] floats1) {
            eq = Arrays.equals(floats, floats1);
        } else if (e1 instanceof double[] doubles && e2 instanceof double[] doubles1) {
            eq = Arrays.equals(doubles, doubles1);
        } else if (e1 instanceof boolean[] booleans && e2 instanceof boolean[] booleans1) {
            eq = Arrays.equals(booleans, booleans1);
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
