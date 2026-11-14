package org.openl.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * @author Andrei Astrouski
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NumberUtils {

    /**
     * Minimal negative float value.
     */
    public static final double FLOAT_MIN_VALUE = -Float.MAX_VALUE;

    /**
     * Minimal negative double value.
     */
    public static final double DOUBLE_MIN_VALUE = -Double.MAX_VALUE;

    /** The smallest {@code long}, -2<sup>63</sup>, which a {@code double} holds exactly. */
    private static final double LONG_RANGE_START = Long.MIN_VALUE;

    /**
     * 2<sup>63</sup>, the first {@code double} beyond the largest {@code long}: the largest {@code long} as a
     * {@code double} rounds up to it.
     */
    private static final double LONG_RANGE_END = 0x1p63;

    /** The bits of the significand of a {@code double}, the hidden leading one included. */
    private static final int DOUBLE_SIGNIFICAND_BITS = 53;

    public static Object intOrDouble(double value) {
        var intValue = (int) value;
        if (value == intValue) {
            return intValue;
        }

        return value;
    }

    /**
     * Whether a {@code double} holds a number exactly, as a cell of a workbook keeps a number.
     *
     * <p>A cell of a number is read back by the type of its value: a decimal as the shortest decimal of the
     * {@code double}, and a whole number within the range of a {@code long} as the {@code long} the {@code double}
     * converts to. A number is held when it reads back the same either way.
     *
     * <p>A {@link BigDecimal} of more digits than a {@code double} has, such as {@code 0.66666666666666663}, is not
     * held, nor is a {@link Long} or a {@link BigInteger} that a {@code double} rounds, such as
     * {@code 1000000000000000100}, or whose shortest decimal is another number, such as {@link Long#MIN_VALUE}. Neither
     * are {@code NaN} and the infinities. Any other number is held, a {@link BigDecimal} of trailing zeros, such as
     * {@code 100.00}, too.
     *
     * @param number the number
     * @return whether its {@code double} is the same number
     */
    public static boolean isHeldByDouble(Number number) {
        var value = number.doubleValue();
        if (!Double.isFinite(value)) {
            return false;
        }
        var exact = switch (number) {
            case BigDecimal decimal -> decimal;
            case BigInteger integer -> new BigDecimal(integer);
            case Long whole -> BigDecimal.valueOf(whole);
            // A double, a float and a smaller whole number are what their double holds.
            default -> null;
        };
        return exact == null || readsBackAs(value, exact);
    }

    /** Whether a cell of the {@code double} reads back as the number, whatever the type it is read as. */
    private static boolean readsBackAs(double value, BigDecimal exact) {
        var whole = value == Math.rint(value) && isInLongRange(value);
        return BigDecimal.valueOf(value).compareTo(exact) == 0 && (!whole || fitsLong(exact));
    }

    /** Whether a number is a {@code long} that a {@code double} holds exactly. */
    private static boolean fitsLong(BigDecimal number) {
        try {
            return fitsSignificand(number.longValueExact());
        } catch (ArithmeticException e) {
            // Beyond the range of a long, or a fraction.
            return false;
        }
    }

    /**
     * Whether a {@code double} holds a {@code long} exactly: its bits from the highest to the lowest one set fit the
     * significand of a {@code double}.
     */
    private static boolean fitsSignificand(long whole) {
        // The smallest long has no positive counterpart and stays itself, read as 2^63 unsigned: one bit.
        var magnitude = Math.abs(whole);
        var bits = Long.SIZE - Long.numberOfLeadingZeros(magnitude) - Long.numberOfTrailingZeros(magnitude);
        return bits <= DOUBLE_SIGNIFICAND_BITS;
    }

    /**
     * Whether a {@code double} is within the range of a {@code long}, so that it converts to a {@code long} without
     * being clamped to the smallest or the largest one.
     *
     * @param value the number
     * @return whether it is at least -2<sup>63</sup> and below 2<sup>63</sup>
     */
    public static boolean isInLongRange(double value) {
        return value >= LONG_RANGE_START && value < LONG_RANGE_END;
    }

    /**
     * Whether a {@code double} is a whole number that a {@code long} holds with every digit the {@code double}
     * shows: below 2<sup>53</sup> in magnitude, where each whole number is held exactly.
     *
     * @param value the number
     * @return whether it converts to the same whole {@code long}
     */
    public static boolean isExactWhole(double value) {
        return value == Math.rint(value) && Math.abs(value) < 0x1p53;
    }

    /**
     * The number a text holds when a {@code double} does not hold it exactly, as a cell keeps such a number.
     *
     * <p>The text is a number in plain or scientific notation, such as {@code 0.6666666666666666666666666666666667} or
     * {@code 1E999999999}, with no surrounding whitespace counted.
     *
     * @param text the text
     * @return the number, or {@code null} when the text is no number or a {@code double} holds it
     */
    public static @Nullable BigDecimal beyondDouble(String text) {
        try {
            var number = new BigDecimal(text.strip());
            return isHeldByDouble(number) ? null : number;
        } catch (NumberFormatException e) {
            return null;
        }
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
