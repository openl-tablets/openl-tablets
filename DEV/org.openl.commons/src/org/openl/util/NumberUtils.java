package org.openl.util;

/**
 * @author Andrei Astrouski
 */
public class NumberUtils {

    /**
     * Minimal negative float value.
     */
    public static final double FLOAT_MIN_VALUE = -Float.MAX_VALUE;

    /**
     * Minimal negative double value.
     */
    public static final double DOUBLE_MIN_VALUE = -Double.MAX_VALUE;

    private NumberUtils() {
    }

    public static Object intOrDouble(double value) {
        int intValue = (int) value;
        if (value == intValue) {
            return intValue;
        }

        return value;
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
            minValue = FLOAT_MIN_VALUE;
        } else if (numberClass == double.class || numberClass == Double.class) {
            minValue = DOUBLE_MIN_VALUE;
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
