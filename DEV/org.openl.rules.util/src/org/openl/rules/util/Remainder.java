package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Truncated remainder: the result has the same sign as the dividend, matching Java's {@code %} operator
 * and {@link BigInteger#remainder(BigInteger)} / {@link BigDecimal#remainder(BigDecimal)}.
 * <p>
 * Formula:
 * <pre>{@code
 *   remainder(a, b) = a - b * truncate(a / b)
 * }</pre>
 * where {@code truncate} rounds toward zero. Pairs with {@link Quotient} via the identity
 * {@code a == b * quotient(a, b) + remainder(a, b)}.
 * <p>
 * <b>Not the same as</b> {@link Math#IEEEremainder(double, double)}, which uses
 * {@code a - b * round(a / b)} with IEEE 754 round-half-to-even. That operation produces a signed
 * remainder of minimum absolute magnitude (always within {@code [-|b|/2, |b|/2]}) and takes its sign
 * from the rounding direction rather than the dividend. Example for {@code a = 5, b = 3}:
 * <ul>
 *   <li>{@code remainder(5, 3) == 5 - 3 * trunc(5/3) == 5 - 3 * 1 == 2}</li>
 *   <li>{@code Math.IEEEremainder(5, 3) == 5 - 3 * round(5/3) == 5 - 3 * 2 == -1}</li>
 * </ul>
 * <p>
 * Distinct also from {@link Modular} (Excel's {@code MOD}, floor-mod, same sign as divisor); the two agree
 * when operands share a sign and differ by {@code divisor} otherwise.
 * <p>
 * Contract:
 * <ul>
 *   <li>{@code remainder(a, b) == 0} when {@code a} is exactly divisible by {@code b}.</li>
 *   <li>Integer divide-by-zero (byte, short, int, long, BigInteger, BigDecimal) throws
 *       {@link ArithmeticException}.</li>
 *   <li>Floating-point divide-by-zero (float, double) returns {@code NaN}, following IEEE 754.</li>
 *   <li>All overloads return {@code null} when any argument is {@code null}.</li>
 * </ul>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Remainder">Remainder (Wikipedia)</a>
 */
public final class Remainder {

    private Remainder() {
        // Utility class
    }

    public static Byte remainder(Byte dividend, Byte divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return (byte) remainderInt(dividend, divisor);
    }

    public static Short remainder(Short dividend, Short divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return (short) remainderInt(dividend, divisor);
    }

    public static Integer remainder(Integer dividend, Integer divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return remainderInt(dividend, divisor);
    }

    public static Long remainder(Long dividend, Long divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return dividend % divisor;
    }

    public static Float remainder(Float dividend, Float divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        // Java's float % is IEEE 754: x % 0.0f returns NaN, NaN propagates.
        return dividend % divisor;
    }

    public static Double remainder(Double dividend, Double divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return dividend % divisor;
    }

    public static BigInteger remainder(BigInteger dividend, BigInteger divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return dividend.remainder(divisor);
    }

    public static BigDecimal remainder(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return dividend.remainder(divisor);
    }

    private static int remainderInt(int dividend, int divisor) {
        return dividend % divisor;
    }
}
