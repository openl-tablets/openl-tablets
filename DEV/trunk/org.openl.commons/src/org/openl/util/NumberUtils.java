package org.openl.util;

/**
 * @author Andrei Astrouski
 */
public class NumberUtils {

    private NumberUtils() {}

    public static Object intOrDouble(double value) {
        int intValue = (int) value;
        Object res = value;
        if (value == intValue)
            res = (Integer) intValue;

        return res;
    }

    public static Number getMinValue(Class<?> numberClass) {
        Number minValue = null;
        if (numberClass == byte.class || numberClass == Byte.class) {
            minValue = Byte.MIN_VALUE;
        } else if (numberClass == short.class || numberClass == Short.class) {
            minValue = Short.MIN_VALUE;
        } else if (numberClass == int.class || numberClass == Integer.class) {
            minValue = Integer.MIN_VALUE;
        } else if (numberClass == long.class || numberClass == Long.class) {
            minValue = Long.MIN_VALUE;
        } else if (numberClass == float.class || numberClass == Float.class) {
            minValue = Float.MIN_VALUE;
        } else if (numberClass == double.class || numberClass == Double.class) {
            minValue = Double.MIN_VALUE;
        }
        return minValue;
    }

    public static Number getMaxValue(Class<?> numberClass) {
        Number minValue = null;
        if (numberClass == byte.class || numberClass == Byte.class) {
            minValue = Byte.MAX_VALUE;
        } else if (numberClass == short.class || numberClass == Short.class) {
            minValue = Short.MAX_VALUE;
        } else if (numberClass == int.class || numberClass == Integer.class) {
            minValue = Integer.MAX_VALUE;
        } else if (numberClass == long.class || numberClass == Long.class) {
            minValue = Long.MAX_VALUE;
        } else if (numberClass == float.class || numberClass == Float.class) {
            minValue = Float.MAX_VALUE;
        } else if (numberClass == double.class || numberClass == Double.class) {
            minValue = Double.MAX_VALUE;
        }
        return minValue;
    }

}
