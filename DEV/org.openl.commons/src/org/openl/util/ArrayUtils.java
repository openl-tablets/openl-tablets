package org.openl.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public final class ArrayUtils {

    private ArrayUtils() {
    }

    public static boolean deepEquals(Object e1, Object e2) {
        return switch (e1) {
            case Object[] objects when e2 instanceof Object[] objects1 -> Arrays.deepEquals(objects, objects1);
            case byte[] bytes when e2 instanceof byte[] bytes1 -> Arrays.equals(bytes, bytes1);
            case short[] shorts when e2 instanceof short[] shorts1 -> Arrays.equals(shorts, shorts1);
            case int[] ints when e2 instanceof int[] ints1 -> Arrays.equals(ints, ints1);
            case long[] longs when e2 instanceof long[] longs1 -> Arrays.equals(longs, longs1);
            case char[] chars when e2 instanceof char[] chars1 -> Arrays.equals(chars, chars1);
            case float[] floats when e2 instanceof float[] floats1 -> Arrays.equals(floats, floats1);
            case double[] doubles when e2 instanceof double[] doubles1 -> Arrays.equals(doubles, doubles1);
            case boolean[] booleans when e2 instanceof boolean[] booleans1 -> Arrays.equals(booleans, booleans1);
            case null, default -> Objects.equals(e1, e2);
        };
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
        var dim1 = 0;
        while (returnType.isArray()) {
            returnType = returnType.getComponentType();
            dim1++;
        }
        var dim2 = 0;
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
        var size = Array.getLength(o);
        var cache = new Object[size];
        Class<?> componentType = null;
        for (var i = 0; i < size; i++) {
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
        var size = Array.getLength(o);
        Object result = Array.newInstance(newType, size);
        if (dimension == 1) {
            for (var i = 0; i < size; i++) {
                Array.set(result, i, Array.get(o, i));
            }
        } else {
            for (var i = 0; i < size; i++) {
                Array.set(result, i, convert(Array.get(o, i), newType.getComponentType(), dimension - 1));
            }
        }
        return result;
    }
}
