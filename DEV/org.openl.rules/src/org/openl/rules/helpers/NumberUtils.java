package org.openl.rules.helpers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.openl.meta.*;

public final class NumberUtils {

    private NumberUtils() {
    }

    public static boolean isObjectFloatPointNumber(Object value) {
        if (value != null) {
            return isFloatPointType(value.getClass());
        }
        return false;
    }

    public static boolean isObjectNonFloatPointNumber(Object value) {
        if (value != null) {
            return isObjectNonFloatPointNumber(value);
        }
        return false;
    }

    public static boolean isFloatPointType(Class<?> cls) {
        return cls != null && (float.class.equals(cls) || double.class.equals(cls) || Float.class
            .isAssignableFrom(cls) || FloatValue.class.isAssignableFrom(cls) || Double.class
                .isAssignableFrom(cls) || DoubleValue.class.isAssignableFrom(
                    cls) || BigDecimal.class.isAssignableFrom(cls) || BigDecimalValue.class.isAssignableFrom(cls));
    }

    public static boolean isNonFloatPointType(Class<?> clazz) {
        return clazz != null && (byte.class.equals(clazz) || short.class.equals(clazz) || int.class
            .equals(clazz) || long.class.equals(clazz) || Byte.class.isAssignableFrom(clazz) || ByteValue.class
                .isAssignableFrom(clazz) || Short.class
                    .isAssignableFrom(clazz) || ShortValue.class.isAssignableFrom(clazz) || Integer.class
                        .isAssignableFrom(clazz) || IntValue.class.isAssignableFrom(clazz) || Long.class
                            .isAssignableFrom(clazz) || LongValue.class.isAssignableFrom(clazz) || BigInteger.class
                                .isAssignableFrom(clazz) || BigIntegerValue.class.isAssignableFrom(clazz));
    }

    public static Double convertToDouble(Object object) {

        if (Float.class.equals(object.getClass())) {
            return Double.parseDouble(object.toString());
        }

        if (FloatValue.class.isAssignableFrom(object.getClass())) {
            return Double.parseDouble(object.toString());
        }

        if (Double.class.equals(object.getClass())) {
            return (Double) object;
        }

        if (DoubleValue.class.isAssignableFrom(object.getClass())) {
            return ((DoubleValue) object).doubleValue();
        }

        if (BigDecimal.class.equals(object.getClass())) {
            return ((BigDecimal) object).doubleValue();
        }

        if (BigDecimalValue.class.equals(object.getClass())) {
            return ((BigDecimalValue) object).doubleValue();
        }

        return null;
    }

    public static DoubleValue convertToDoubleValue(Object object) {
        if (FloatValue.class.isAssignableFrom(object.getClass())) {
            return FloatValue.autocast((FloatValue) object, (DoubleValue) null);
        }
        if (DoubleValue.class.isAssignableFrom(object.getClass())) {
            return (DoubleValue) object;
        }

        if (BigDecimalValue.class.isAssignableFrom(object.getClass())) {
            return BigDecimalValue.cast((BigDecimalValue) object, (DoubleValue) null);
        }

        return new DoubleValue(convertToDouble(object));
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
     * Gets the scale of the income value. Note that if the value will be of type {@link Float} or {@link FloatValue},
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

        if (value instanceof BigDecimalValue) {
            /**
             * If BigDecimalValue the scale can be taken directly
             */
            return ((BigDecimalValue) value).getValue().scale();
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

    public static Class<?> getNumericPrimitive(Class<?> wrapperClass) {
        if (Byte.class.equals(wrapperClass)) {
            return byte.class;
        } else if (Short.class.equals(wrapperClass)) {
            return short.class;
        } else if (Integer.class.equals(wrapperClass)) {
            return int.class;
        } else if (Long.class.equals(wrapperClass)) {
            return long.class;
        } else if (Float.class.equals(wrapperClass)) {
            return float.class;
        } else if (Double.class.equals(wrapperClass)) {
            return double.class;
        }
        return null;
    }

    public static boolean isNumberType(Class<?> cls) {
        return isFloatPointType(cls) || isNonFloatPointType(cls);
    }

}
