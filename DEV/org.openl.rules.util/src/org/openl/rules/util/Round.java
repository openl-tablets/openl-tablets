package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.jspecify.annotations.Nullable;

/**
 * A set of rounding util methods.
 * <p>
 * Note: For OpenL rules only! Don't use it in Java code.
 *
 * @author Yury Molchan
 */
public final class Round {

    private Round() {
        // Utility class
    }

    public static final RoundingMode UP = RoundingMode.UP;
    public static final RoundingMode DOWN = RoundingMode.DOWN;
    public static final RoundingMode CEILING = RoundingMode.CEILING;
    public static final RoundingMode FLOOR = RoundingMode.FLOOR;
    public static final RoundingMode HALF_UP = RoundingMode.HALF_UP;
    public static final RoundingMode HALF_DOWN = RoundingMode.HALF_DOWN;
    public static final RoundingMode HALF_EVEN = RoundingMode.HALF_EVEN;
    public static final RoundingMode UNNECESSARY = RoundingMode.UNNECESSARY;

    /**
     * 2<sup>31</sup>, the first {@code float} beyond the {@code int} range.
     */
    private static final float INT_LIMIT = 0x1p31f;

    private static final BigDecimal MIN_INT = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigDecimal MAX_INT = BigDecimal.valueOf(Integer.MAX_VALUE);

    /**
     * Rounds the value to the closest whole number, with ties rounding away from zero as {@link #HALF_UP} does.
     * <p>
     * A fraction stored just below a half, such as 2.4999999999999996, rounds as that half does: the closest
     * {@code double} to a decimal number can be slightly smaller than it.
     * <p>
     * NaN gives 0. A value beyond the {@code Integer} range, including an infinity, gives {@link Integer#MAX_VALUE} or
     * {@link Integer#MIN_VALUE}, so round such a value with {@link #round(Double, int)}, which keeps its type. An empty
     * value gives an empty result.
     *
     * @param value the value to round
     * @return the closest whole number
     */
    public static @Nullable Integer round(@Nullable Double value) {
        if (value == null) {
            return null;
        }
        return round((double) value);
    }

    /**
     * Rounds the value to a whole number with the given rounding mode.
     * <p>
     * NaN gives 0. A value beyond the {@code Integer} range, including an infinity, gives {@link Integer#MAX_VALUE} or
     * {@link Integer#MIN_VALUE} in every rounding mode, so round such a value with
     * {@link #round(Double, int, RoundingMode)}, which keeps its type. An empty value gives an empty result.
     *
     * @param value        the value to round
     * @param roundingMode the rounding mode, such as {@link #DOWN}
     * @return the whole number
     * @throws ArithmeticException if the rounding mode is {@link #UNNECESSARY} and the value has a fraction
     */
    public static @Nullable Integer round(@Nullable Double value, RoundingMode roundingMode) {
        if (value == null) {
            return null;
        }
        return round((double) value, roundingMode);
    }

    private static int round(double value) {
        if (Double.isNaN(value)) {
            return 0;
        } else if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        var corrected = correct(value);
        // Make rounding symmetrical: 0.5 ==> 1 and -0.5 ==> -1
        return (int) (corrected < 0 ? -Math.round(-corrected) : Math.round(corrected));
    }

    private static int round(double x, RoundingMode rounding) {
        if (!Double.isFinite(x)) {
            return round(x);
        }
        return toInt(BigDecimal.valueOf(x).setScale(0, rounding));
    }

    /**
     * Rounds the value to the closest whole number, with ties rounding away from zero as {@link #HALF_UP} does.
     * <p>
     * NaN gives 0. A value beyond the {@code Integer} range, including an infinity, gives {@link Integer#MAX_VALUE} or
     * {@link Integer#MIN_VALUE}. An empty value gives an empty result.
     *
     * @param value the value to round
     * @return the closest whole number
     */
    public static @Nullable Integer round(@Nullable Float value) {
        if (value == null) {
            return null;
        }
        return round((float) value);
    }

    /**
     * Rounds the value to a whole number with the given rounding mode.
     * <p>
     * NaN gives 0. A value beyond the {@code Integer} range, including an infinity, gives {@link Integer#MAX_VALUE} or
     * {@link Integer#MIN_VALUE} in every rounding mode. An empty value gives an empty result.
     *
     * @param value        the value to round
     * @param roundingMode the rounding mode, such as {@link #DOWN}
     * @return the whole number
     * @throws ArithmeticException if the rounding mode is {@link #UNNECESSARY} and the value has a fraction
     */
    public static @Nullable Integer round(@Nullable Float value, RoundingMode roundingMode) {
        if (value == null) {
            return null;
        }
        return round((double) value, roundingMode);
    }

    private static int round(float value) {
        if (Float.isNaN(value)) {
            return 0;
        } else if (value >= INT_LIMIT) {
            return Integer.MAX_VALUE;
        } else if (value <= -INT_LIMIT) {
            return Integer.MIN_VALUE;
        }
        // Make rounding symmetrical: 0.5 ==> 1 and -0.5 ==> -1
        return value < 0 ? -Math.round(-value) : Math.round(value);
    }

    /**
     * Rounds a whole number, which returns it as it is.
     * <p>
     * Every whole number, such as an {@code Integer}, a {@code Long}, a {@code Short} or a {@code Byte}, is rounded
     * here and gives an {@code Integer}. A number beyond the {@code Integer} range gives {@link Integer#MAX_VALUE} or
     * {@link Integer#MIN_VALUE}. An empty value gives an empty result.
     *
     * @param value the whole number to round
     * @return the same whole number
     */
    public static @Nullable Integer round(@Nullable Long value) {
        return round(value, HALF_UP);
    }

    /**
     * Rounds a whole number with the given rounding mode, which returns it as it is.
     * <p>
     * Every whole number, such as an {@code Integer}, a {@code Long}, a {@code Short} or a {@code Byte}, is rounded
     * here and gives an {@code Integer}. A number beyond the {@code Integer} range gives {@link Integer#MAX_VALUE} or
     * {@link Integer#MIN_VALUE}. An empty value gives an empty result.
     *
     * @param value        the whole number to round
     * @param roundingMode the rounding mode, such as {@link #DOWN}
     * @return the same whole number
     */
    public static @Nullable Integer round(@Nullable Long value, RoundingMode roundingMode) {
        if (value == null) {
            return null;
        }
        return toInt(BigDecimal.valueOf(value).setScale(0, roundingMode));
    }

    /**
     * Returns the closest integer to the argument, with ties rounding up. The value is rounded like using the
     * {@link RoundingMode#HALF_UP} method.
     */
    public static @Nullable BigInteger round(@Nullable BigDecimal value) {
        return Round.round(value, HALF_UP);
    }

    /**
     * Rounds the value to the specified number of decimal places, with ties rounding away from zero as
     * {@link #HALF_UP} does.
     * <p>
     * A fraction stored just below a half rounds as that half does: 2.675 is stored as 2.67499999..., yet rounds to
     * 2.68. A whole number is not changed by that, so with zero or more decimal places it is returned as it is.
     * <p>
     * NaN, an infinity and an empty value are returned as they are.
     *
     * @param value the value to round
     * @param scale the number of digits to the right of the decimal point
     * @return the rounded value
     */
    public static @Nullable Double round(@Nullable Double value, int scale) {
        if (value == null) {
            return null;
        }
        return round((double) value, scale);
    }

    private static double round(double value, int scale) {
        return round(correct(value), scale, HALF_UP);
    }

    /**
     * Round the given value to the specified number of decimal places. The value is rounded using the
     * {@link RoundingMode#HALF_UP} method.
     *
     * @param value the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @return the rounded value.
     */
    public static @Nullable BigDecimal round(@Nullable BigDecimal value, int scale) {
        return round(value, scale, HALF_UP);
    }

    /**
     * Round the given value to the specified number of decimal places. The value is rounded using the
     * {@link RoundingMode#HALF_UP} method. An empty value gives an empty result.
     *
     * @param value the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @return the rounded value.
     */
    public static @Nullable Float round(@Nullable Float value, int scale) {
        if (value == null) {
            return null;
        }
        return round((float) value, scale);
    }

    private static float round(float value, int scale) {
        return round(value, scale, HALF_UP);
    }

    /**
     * Rounds a whole number to the specified number of decimal places, with ties rounding away from zero as
     * {@link #HALF_UP} does, and gives a {@code Double} as {@link #round(Double, int)} does.
     * <p>
     * Every whole number, such as an {@code Integer}, a {@code Long}, a {@code Short} or a {@code Byte}, is rounded
     * here, so it is not rounded as a {@code Float}. Zero or more decimal places keep the number, and a negative number
     * of them rounds it to tens, hundreds and so on.
     * <p>
     * An empty value gives an empty result.
     *
     * @param value the whole number to round
     * @param scale the number of digits to the right of the decimal point
     * @return the rounded number
     */
    public static @Nullable Double round(@Nullable Long value, int scale) {
        return round(value, scale, HALF_UP);
    }

    /**
     * Round the given value to the specified number of decimal places. The value is rounded using the given method
     * which is any method defined in {@link RoundingMode}. An empty value gives an empty result.
     *
     * @param x        the value to round.
     * @param scale    the number of digits to the right of the decimal point.
     * @param rounding the rounding method as defined in {@link RoundingMode}.
     * @return the rounded value.
     */
    public static @Nullable Double round(@Nullable Double x, int scale, RoundingMode rounding) {
        if (x == null) {
            return null;
        }
        return round((double) x, scale, rounding);
    }

    private static double round(double x, int scale, RoundingMode rounding) {
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
     * Round the given value to the specified number of decimal places. The value is rounded using the given method
     * which is any method defined in {@link RoundingMode}. An empty value gives an empty result.
     *
     * @param x        the value to round.
     * @param scale    the number of digits to the right of the decimal point.
     * @param rounding the rounding method as defined in {@link RoundingMode}.
     * @return the rounded value.
     */
    public static @Nullable Float round(@Nullable Float x, int scale, RoundingMode rounding) {
        if (x == null) {
            return null;
        }
        return round((float) x, scale, rounding);
    }

    private static float round(float x, int scale, RoundingMode rounding) {
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
     * Rounds a whole number to the specified number of decimal places with the given rounding mode, and gives a
     * {@code Double} as {@link #round(Double, int, RoundingMode)} does.
     * <p>
     * Every whole number, such as an {@code Integer}, a {@code Long}, a {@code Short} or a {@code Byte}, is rounded
     * here, so it is not rounded as a {@code Float}. Zero or more decimal places keep the number, and a negative number
     * of them rounds it to tens, hundreds and so on.
     * <p>
     * An empty value gives an empty result.
     *
     * @param value    the whole number to round
     * @param scale    the number of digits to the right of the decimal point
     * @param rounding the rounding mode, such as {@link #DOWN}
     * @return the rounded number
     * @throws ArithmeticException if the rounding mode is {@link #UNNECESSARY} and the number has to be rounded
     */
    public static @Nullable Double round(@Nullable Long value, int scale, RoundingMode rounding) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(scale, rounding).doubleValue();
    }

    /**
     * Round the given value to the specified number of decimal places. The value is rounded using the given method
     * which is any method defined in {@link RoundingMode}.
     *
     * @param x        the value to round.
     * @param scale    the number of digits to the right of the decimal point.
     * @param rounding the rounding method as defined in {@link RoundingMode}.
     * @return the rounded value.
     */
    public static @Nullable BigDecimal round(@Nullable BigDecimal x, int scale, RoundingMode rounding) {
        if (x == null) {
            return null;
        }
        return x.setScale(scale, rounding);
    }

    public static @Nullable BigInteger round(@Nullable BigDecimal x, RoundingMode rounding) {
        if (x == null) {
            return null;
        }
        return x.setScale(0, rounding).toBigInteger();
    }

    /**
     * For backward compatibility
     */
    public static @Nullable Double round(@Nullable Double x, int scale, int rounding) {
        return round(x, scale, RoundingMode.valueOf(rounding));
    }

    /**
     * For backward compatibility
     */
    public static @Nullable Float round(@Nullable Float x, int scale, int rounding) {
        return round(x, scale, RoundingMode.valueOf(rounding));
    }

    /**
     * Like {@link #round(Long, int, RoundingMode)}, with the rounding mode given by its number: 0 for {@link #UP} to
     * 7 for {@link #UNNECESSARY}.
     */
    public static @Nullable Double round(@Nullable Long x, int scale, int rounding) {
        return round(x, scale, RoundingMode.valueOf(rounding));
    }

    /**
     * For backward compatibility
     */
    public static @Nullable BigDecimal round(@Nullable BigDecimal x, int scale, int rounding) {
        return round(x, scale, RoundingMode.valueOf(rounding));
    }

    /**
     * Like {@link #round(Double)} but without a ulp amendment, and with the {@code Long} range
     */
    public static @Nullable Long roundStrict(@Nullable Double value) {
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

    /**
     * Like {@link #round(Double, int)} but without a ulp amendment
     */
    public static @Nullable Double roundStrict(@Nullable Double value, int scale) {
        return round(value, scale, HALF_UP);
    }

    /**
     * Moves a fraction one ulp away from zero, so that a fraction stored just below a half rounds as that half does.
     * A whole number is stored exactly and is returned as it is.
     */
    private static double correct(double value) {
        return value == Math.rint(value) ? value : value + Math.copySign(Math.ulp(value), value);
    }

    /**
     * Converts a whole number to an {@code int}. A number beyond the {@code int} range gives the limit of the range.
     */
    private static int toInt(BigDecimal value) {
        return value.max(MIN_INT).min(MAX_INT).intValue();
    }
}
