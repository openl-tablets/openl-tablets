package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Excel-compatible {@code QUOTIENT}: returns the integer portion of the division, truncated toward zero.
 * Unlike Excel's {@code INT} (which floors toward negative infinity), {@code QUOTIENT} always discards the
 * fractional part: {@code QUOTIENT(-10, 3) == -3}, whereas {@code INT(-10/3) == -4}.
 * <p>
 * Formula for {@link Float}:
 * <pre>{@code
 *   val = dividend / divisor
 *   return (val >= 0) ? floor(val) : ceil(val)
 * }</pre>
 * This preserves the original floating-point magnitude — use {@code .longValue()} or {@code .intValue()}
 * at the call site if an integer type is needed.
 * <p>
 * Contract:
 * <ul>
 *   <li>Return type matches the input type (e.g. {@link Byte} → {@link Byte}, {@link Integer} →
 *       {@link Integer}, {@link java.math.BigDecimal} → {@link java.math.BigDecimal}, …).</li>
 *   <li>Integer divide-by-zero throws {@link ArithmeticException} (Java's {@code /} operator).</li>
 *   <li>Float/double divide-by-zero follows IEEE 754: returns {@code ±Infinity} or {@code NaN}.</li>
 *   <li>Double operands count as the decimals they show, a computed number of 16 or 17 digits as its double
 *       ({@link DecimalArithmetic}):
 *       {@code quotient(0.3, 0.1) == 3.0}, while Java's {@code 0.3 / 0.1} is {@code 2.9999999999999996}. The result
 *       is the whole number of divisors that {@link Remainder#remainder(Double, Double)} leaves the rest of, so
 *       {@code dividend == divisor * quotient + remainder} for the decimals: a dividend within a rounding of a
 *       multiple of the divisor counts as that multiple.</li>
 *   <li>{@code NaN} / {@code ±Infinity} inputs propagate naturally through the formula.</li>
 *   <li>All overloads return {@code null} when any argument is {@code null}.</li>
 * </ul>
 */
public final class Quotient {

    private Quotient() {
        // Utility class
    }

    public static Byte quotient(Byte dividend, Byte divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return (byte) quotientInt(dividend, divisor);
    }

    public static Short quotient(Short dividend, Short divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return (short) quotientInt(dividend, divisor);
    }

    public static Integer quotient(Integer dividend, Integer divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return quotientInt(dividend, divisor);
    }

    public static Long quotient(Long dividend, Long divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return dividend / divisor;
    }

    public static Float quotient(Float dividend, Float divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        var val = dividend / divisor;
        return val >= 0.0f ? (float) Math.floor(val) : (float) Math.ceil(val);
    }

    public static Double quotient(Double dividend, Double divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return DecimalArithmetic.wholeQuotient(dividend, divisor, false);
    }

    public static BigInteger quotient(BigInteger dividend, BigInteger divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return dividend.divide(divisor);
    }

    public static BigDecimal quotient(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return dividend.divideToIntegralValue(divisor);
    }

    private static int quotientInt(int dividend, int divisor) {
        return dividend / divisor;
    }
}
