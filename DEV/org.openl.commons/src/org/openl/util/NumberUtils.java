package org.openl.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

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

    public static boolean isObjectFloatPointNumber(Object value) {
        if (value != null) {
            return isFloatPointType(value.getClass());
        }
        return false;
    }

    public static boolean isFloatPointType(Class<?> cls) {
        return cls != null && (float.class == cls || double.class == cls || Float.class == cls || Double.class == cls
                || BigDecimal.class.isAssignableFrom(cls));
    }

    public static boolean isNonFloatPointType(Class<?> cls) {
        return cls != null && (byte.class == cls || short.class == cls || int.class == cls || long.class == cls
                || Byte.class == cls || Short.class == cls || Integer.class == cls || Long.class == cls || BigInteger.class == cls);
    }

    public static Double convertToDouble(Object object) {
        if (object instanceof Float) {
            return Double.parseDouble(object.toString());
        } else if (object instanceof Double) {
            return (Double) object;
        } else if (object instanceof BigDecimal) {
            return ((BigDecimal) object).doubleValue();
        } else if (object instanceof Byte) {
            return ((Byte) object).doubleValue();
        } else if (object instanceof Short) {
            return ((Short) object).doubleValue();
        } else if (object instanceof Integer) {
            return ((Integer) object).doubleValue();
        } else if (object instanceof Long) {
            return ((Long) object).doubleValue();
        } else if (object instanceof BigInteger) {
            return ((BigInteger) object).doubleValue();
        } else {
            return null;
        }
    }

    public static Double roundValue(Double value, int scale) {

        if (value != null) {
            if (value.isInfinite() || value.isNaN()) {
                return value;
            }
            BigDecimal roundedValue = BigDecimal.valueOf(value);
            roundedValue = roundedValue.setScale(scale, RoundingMode.HALF_UP);

            return roundedValue.doubleValue();
        }

        return null;
    }

    /**
     * Gets the scale of the income value. Note that if the value will be of type {@link Float},
     * the scale will be defined via value.doubleValue() method call. And the scale will differ from the income.
     *
     * @param value
     * @return number of values after the comma
     *
     * @throws {@link NullPointerException} if the income is <code>null</code>
     */
    public static int getScale(Number value) {
        if (value == null) {
            throw new NullPointerException("Null value is not supported");
        }

        if (value instanceof BigDecimal) {
            /**
             * If BigDecimal the scale can be taken directly
             */
            return ((BigDecimal) value).scale();
        }

        if (isObjectFloatPointNumber(value)) {
            /**
             * Process as float point value
             */
            return getScale(convertToDouble(value).doubleValue());
        } else {
            /**
             * Process as integer value
             */
            return BigDecimal.valueOf(value.longValue()).scale();
        }
    }

    public static int getScale(double value) {
        if (!Double.isNaN(value) && !Double.isInfinite(value)) {
            BigDecimal decimal = BigDecimal.valueOf(value);

            return decimal.scale();
        }
        return 0;
    }

    public static int getScale(float value) {
        return getScale(Double.parseDouble(Float.toString(value)));
    }

    public static boolean isNumberType(Class<?> cls) {
        return isFloatPointType(cls) || isNonFloatPointType(cls);
    }
}
