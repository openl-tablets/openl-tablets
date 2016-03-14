package org.openl.extension.xmlrules.utils;

import java.lang.reflect.Array;

public class HelperFunctions {
    public static <T> T[][] transpose(T[][] arr) {
        if (arr == null || arr.length == 0) {
            return arr;
        }

        Class clazz = arr[0].getClass().getComponentType();
        @SuppressWarnings("unchecked")
        T[][] newArr = (T[][]) Array.newInstance(clazz, arr[0].length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            T[] row = arr[i];
            for (int j = 0; j < row.length; j++) {
                newArr[j][i] = row[j];
            }
        }
        return newArr;
    }

    public static Double toDouble(Object x) {
        if (x == null) {
            return null;
        }

        if (x instanceof Double) {
            return (Double) x;
        }

        if (x instanceof String) {
            return Double.valueOf((String) x);
        }

        // Other number types
        if (x instanceof Number) {
            return ((Number) x).doubleValue();
        }

        if (x.getClass().isArray() && Array.getLength(x) == 1) {
            return toDouble(Array.get(x, 0));
        }

        throw new IllegalArgumentException("Can't convert to double");
    }

    public static Object convertArgument(Class<?> expectedClass, Object value) {
        if (value != null) {
            Class<?> valueClass = value.getClass();
            if (!expectedClass.isAssignableFrom(valueClass)) {
                if (String.class == expectedClass && value instanceof Double) {
                    value = String.valueOf(value);
                } else if (Double.class == expectedClass && value instanceof String) {
                    value = Double.valueOf((String) value);
                } else if (String.class == expectedClass && value instanceof Integer) {
                    value = String.valueOf(value);
                } else if (Integer.class == expectedClass && value instanceof String) {
                    value = Integer.valueOf((String) value);
                } else if (expectedClass.isArray() && valueClass.isArray()) {
                    // For example expected: Rider[], but actual: Object[] with Rider objects
                    int size = Array.getLength(value);

                    Object newValue = Array.newInstance(expectedClass.getComponentType(), size);
                    for (int i = 0; i < size; ++i) {
                        Array.set(newValue, i, Array.get(value, i));
                    }

                    value = newValue;
                } else if (!expectedClass.isArray() && valueClass.isArray()) {
                    if (Array.getLength(value) == 1) {
                        value = convertArgument(expectedClass, Array.get(value, 0));
                    }
                }
            }
        }
        return value;
    }
}
