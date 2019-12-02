package org.openl.rules.helpers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;

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
            return isNonFloatPointType(value.getClass());
        }
        return false;
    }

    public static boolean isFloatPointType(Class<?> cls) {
        return cls != null && (float.class == cls || double.class == cls || Float.class == cls || FloatValue.class
            .isAssignableFrom(cls) || Double.class == cls || DoubleValue.class.isAssignableFrom(cls) || BigDecimal.class
                .isAssignableFrom(cls) || BigDecimalValue.class.isAssignableFrom(cls));
    }

    public static boolean isNonFloatPointType(Class<?> cls) {
        return cls != null && (byte.class == cls || short.class == cls || int.class == cls || long.class == cls || Byte.class == cls || ByteValue.class
            .isAssignableFrom(cls) || Short.class == cls || ShortValue.class
                .isAssignableFrom(cls) || Integer.class == cls || IntValue.class
                    .isAssignableFrom(cls) || Long.class == cls || LongValue.class.isAssignableFrom(
                        cls) || BigInteger.class == cls || BigIntegerValue.class.isAssignableFrom(cls));
    }

    public static Double convertToDouble(Object object) {
        if (object instanceof Float) {
            return Double.parseDouble(object.toString());
        } else if (object instanceof FloatValue) {
            return Double.parseDouble(object.toString());
        } else if (object instanceof Double) {
            return (Double) object;
        } else if (object instanceof DoubleValue) {
            return ((DoubleValue) object).doubleValue();
        } else if (object instanceof BigDecimal) {
            return ((BigDecimal) object).doubleValue();
        } else if (object instanceof BigDecimalValue) {
            return ((BigDecimalValue) object).doubleValue();
        } else if (object instanceof Byte) {
            return ((Byte) object).doubleValue();
        } else if (object instanceof ByteValue) {
            return ((ByteValue) object).doubleValue();
        } else if (object instanceof Short) {
            return ((Short) object).doubleValue();
        } else if (object instanceof ShortValue) {
            return ((ShortValue) object).doubleValue();
        } else if (object instanceof Integer) {
            return ((Integer) object).doubleValue();
        } else if (object instanceof IntValue) {
            return ((IntValue) object).doubleValue();
        } else if (object instanceof Long) {
            return ((Long) object).doubleValue();
        } else if (object instanceof LongValue) {
            return ((LongValue) object).doubleValue();
        } else if (object instanceof BigInteger) {
            return ((BigInteger) object).doubleValue();
        } else if (object instanceof BigIntegerValue) {
            return ((BigIntegerValue) object).doubleValue();
        } else {
            return null;
        }
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
        if (Byte.class == wrapperClass) {
            return byte.class;
        } else if (Short.class == wrapperClass) {
            return short.class;
        } else if (Integer.class == wrapperClass) {
            return int.class;
        } else if (Long.class == wrapperClass) {
            return long.class;
        } else if (Float.class == wrapperClass) {
            return float.class;
        } else if (Double.class == wrapperClass) {
            return double.class;
        }
        return null;
    }

    public static boolean isNumberType(Class<?> cls) {
        return isFloatPointType(cls) || isNonFloatPointType(cls);
    }

}
