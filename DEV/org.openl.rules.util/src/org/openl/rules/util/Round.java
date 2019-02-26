package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A set of rounding util methods.
 *
 * Note: For OpenL rules only! Don't use it in Java code.
 *
 * @author Yury Molchan
 */
public final class Round {

    private Round() {
        // Utility class
    }

    public static RoundingMode UP = RoundingMode.UP;
    public static RoundingMode DOWN = RoundingMode.DOWN;
    public static RoundingMode CEILING = RoundingMode.CEILING;
    public static RoundingMode FLOOR = RoundingMode.FLOOR;
    public static RoundingMode HALF_UP = RoundingMode.HALF_UP;
    public static RoundingMode HALF_DOWN = RoundingMode.HALF_DOWN;
    public static RoundingMode HALF_EVEN = RoundingMode.HALF_EVEN;
    public static RoundingMode UNNECESSARY = RoundingMode.UNNECESSARY;

    /**
     * Like {@link #round(double)} but null-safe.
     */
    public static Long round(Double value) {
        if (value == null) {
            return null;
        }
        return round((double) value);
    }

    /**
     * Returns the closest {@code long} to the argument, with ties rounding up. The value is rounded like using the
     * {@link RoundingMode#HALF_UP} method.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If the argument is NaN, the result is 0.
     * <li>If the argument is negative infinity or any value less than or equal to the value of {@code Long.MIN_VALUE},
     * the result is equal to the value of {@code Long.MIN_VALUE}.
     * <li>If the argument is positive infinity or any value greater than or equal to the value of
     * {@code Long.MAX_VALUE}, the result is equal to the value of {@code Long.MAX_VALUE}.
     * </ul>
     *
     * @param value a floating-point value to be rounded to a {@code long}.
     * @return the value of the argument rounded to the nearest {@code long} value.
     */
    public static long round(double value) {
        if (value == 0.0 || Double.isNaN(value)) {
            return 0;
        }
        if (Double.POSITIVE_INFINITY == value) {
            return Long.MAX_VALUE;
        } else if (Double.NEGATIVE_INFINITY == value) {
            return Long.MIN_VALUE;
        } else if (value > 0) {
            // A workaround for rounding the closest to .5 numbers
            // ULP is used for fix imprecise operations of double values
            return Math.round(value + Math.ulp(value));
        } else {
            // Make rounding symmetrical: 0.5 ==> 1 and -0.5 ==> -1
            return -round(-value);
        }
    }

    /**
     * Like {@link #round(float)} but null-safe.
     */
    public static Integer round(Float value) {
        if (value == null) {
            return null;
        }
        return round((float) value);
    }

    /**
     * Returns the closest {@code int} to the argument, with ties rounding up. The value is rounded like using the
     * {@link RoundingMode#HALF_UP} method.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If the argument is NaN, the result is 0.
     * <li>If the argument is negative infinity or any value less than or equal to the value of
     * {@code Integer.MIN_VALUE}, the result is equal to the value of {@code Integer.MIN_VALUE}.
     * <li>If the argument is positive infinity or any value greater than or equal to the value of
     * {@code Integer.MAX_VALUE}, the result is equal to the value of {@code Integer.MAX_VALUE}.
     * </ul>
     *
     * @param value a floating-point value to be rounded to an integer.
     * @return the value of the argument rounded to the nearest {@code int} value.
     */
    public static int round(float value) {
        if (value == 0.0 || Float.isNaN(value)) {
            return 0;
        }
        if (Float.POSITIVE_INFINITY == value) {
            return Integer.MAX_VALUE;
        } else if (Float.NEGATIVE_INFINITY == value) {
            return Integer.MIN_VALUE;
        } else if (value > 0) {
            return Math.round(value);
        } else {
            // Make rounding symmetrical: 0.5 ==> 1 and -0.5 ==> -1
            return -round(-value);
        }
    }

    /**
     * Returns the closest integer to the argument, with ties rounding up. The value is rounded like using the
     * {@link RoundingMode#HALF_UP} method.
     */
    public static BigDecimal round(BigDecimal value) {
        return Round.round(value, 0);
    }

    /**
     * Like {@link #round(double, int)} but null-safe.
     */
    public static Double round(Double value, int scale) {
        if (value == null) {
            return null;
        }
        return round((double) value, scale);
    }

    /**
     * Round the given value to the specified number of decimal places. The value is rounded using the
     * {@link RoundingMode#HALF_UP} method.
     *
     * @param value the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @return the rounded value.
     */
    public static double round(double value, int scale) {
        // A workaround for rounding the closest to .5 numbers
        // ULP is used for fix imprecise operations of double values
        if (value > 0) {
            value += Math.ulp(value);
        } else if (value < 0) {
            value -= Math.ulp(value);
        }
        return round(value, scale, HALF_UP);
    }

    /**
     * Round the given value to the specified number of decimal places. The value is rounded using the
     * {@link RoundingMode#HALF_UP} method.
     *
     * @param value the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @return the rounded value.
     */
    public static BigDecimal round(BigDecimal value, int scale) {
        return round(value, scale, HALF_UP);
    }

    /**
     * Like {@link #round(float, int)} but null-safe.
     */
    public static Float round(Float value, int scale) {
        if (value == null) {
            return null;
        }
        return round((float) value, scale);
    }

    /**
     * Round the given value to the specified number of decimal places. The value is rounded using the
     * {@link RoundingMode#HALF_UP} method.
     *
     * @param value the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @return the rounded value.
     */
    public static float round(float value, int scale) {
        return round(value, scale, HALF_UP);
    }

    /**
     * Like {@link #round(double, int, RoundingMode)} but null-safe.
     */
    public static Double round(Double value, int scale, RoundingMode rounding) {
        if (value == null) {
            return null;
        }
        return round((double) value, scale, rounding);
    }

    /**
     * Round the given value to the specified number of decimal places. The value is rounded using the given method
     * which is any method defined in {@link RoundingMode}.
     *
     * @param x the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @param rounding the rounding method as defined in {@link RoundingMode}.
     * @return the rounded value.
     */
    public static double round(double x, int scale, RoundingMode rounding) {
        if (x == 0 || Double.isInfinite(x) || Double.isNaN(x)) {
            return x;
        }
        try {
            return BigDecimal.valueOf(x).setScale(scale, rounding).doubleValue();
        } catch (NumberFormatException var5) {
            return Double.NaN;
        }
    }

    /**
     * Like {@link #round(float, int, RoundingMode)} but null-safe.
     */
    public static Float round(Float value, int scale, RoundingMode rounding) {
        if (value == null) {
            return null;
        }
        return round((float) value, scale, rounding);
    }

    /**
     * Round the given value to the specified number of decimal places. The value is rounded using the given method
     * which is any method defined in {@link RoundingMode}.
     *
     * @param x the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @param rounding the rounding method as defined in {@link RoundingMode}.
     * @return the rounded value.
     */
    public static float round(float x, int scale, RoundingMode rounding) {
        if (x == 0 || Float.isInfinite(x) || Float.isNaN(x)) {
            return x;
        }
        try {
            String val = Float.toString(x);
            return new BigDecimal(val).setScale(scale, rounding).floatValue();
        } catch (NumberFormatException var4) {
            return Float.NaN;
        }
    }

    /**
     * Round the given value to the specified number of decimal places. The value is rounded using the given method
     * which is any method defined in {@link RoundingMode}.
     *
     * @param x the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @param rounding the rounding method as defined in {@link RoundingMode}.
     * @return the rounded value.
     */
    public static BigDecimal round(BigDecimal x, int scale, RoundingMode rounding) {
        if (x == null) {
            return x;
        }
        return x.setScale(scale, rounding);
    }

    /** For backward compatibility */
    public static Double round(Double x, int scale, int rounding) {
        return round(x, scale, RoundingMode.valueOf(rounding));
    }

    /** For backward compatibility */
    public static Float round(Float x, int scale, int rounding) {
        return round(x, scale, RoundingMode.valueOf(rounding));
    }

    /** For backward compatibility */
    public static BigDecimal round(BigDecimal x, int scale, int rounding) {
        return round(x, scale, RoundingMode.valueOf(rounding));
    }

    /** Like {@code round(double)} but without a ulp amendment */
    public static Long roundStrict(Double value) {
        if (value == null) {
            return null;
        } else if (value == 0.0 || Double.isNaN(value)) {
            return 0L;
        }
        if (Double.POSITIVE_INFINITY == value) {
            return Long.MAX_VALUE;
        } else if (Double.NEGATIVE_INFINITY == value) {
            return Long.MIN_VALUE;
        } else if (value > 0) {
            return Math.round(value);
        } else {
            // Make rounding symmetrical: 0.5 ==> 1 and -0.5 ==> -1
            return -roundStrict(-value);
        }
    }

    /** Like {@code round(double)} but without a ulp amendment */
    public static Double roundStrict(Double value, int scale) {
        return round(value, scale, HALF_UP);
    }
}
