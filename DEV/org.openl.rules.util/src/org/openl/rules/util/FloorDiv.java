package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Floor division: returns the largest integer less than or equal to the true mathematical quotient.
 * Unlike {@link Quotient} (which truncates toward zero), this rounds toward negative infinity, so the
 * two differ when operands have opposite signs and {@code a} is not a multiple of {@code b}. Example:
 * {@code floorDiv(-10, 3) == -4}, whereas {@code quotient(-10, 3) == -3}.
 * <p>
 * Matches {@link Math#floorDiv(int, int)} / {@link Math#floorDiv(long, long)} for integer types.
 * <p>
 * Formula for {@link Float}:
 * <pre>{@code
 *   floor(dividend / divisor)
 * }</pre>
 * <p>
 * Contract:
 * <ul>
 *   <li>Return type matches the input type (e.g. {@link Byte} → {@link Byte}, {@link Integer} →
 *       {@link Integer}, {@link java.math.BigDecimal} → {@link java.math.BigDecimal}, …).</li>
 *   <li>Integer / BigInteger / BigDecimal divide-by-zero throws {@link ArithmeticException}.</li>
 *   <li>Float / double divide-by-zero follows IEEE 754: returns {@code ±Infinity} or {@code NaN}.</li>
 *   <li>Double operands count as the decimals they show, a computed number of 16 or 17 digits as its double
 *       ({@link DecimalArithmetic}):
 *       {@code floorDiv(0.3, 0.1) == 3.0}, while Java's {@code 0.3 / 0.1} is {@code 2.9999999999999996}. The result
 *       is the whole number of divisors that {@link Modular#mod(Double, Double)} leaves the rest of, so
 *       {@code dividend == divisor * floorDiv + mod} for the decimals: a dividend within a rounding of a multiple of
 *       the divisor counts as that multiple. A dividend of the other sign, smaller than a rounding of the divisor,
 *       still gives {@code -1}: {@code floorDiv(-1e-20, 1.0) == -1.0}, and its mod rounds to the divisor.</li>
 *   <li>{@code NaN} and {@code ±Infinity} inputs propagate naturally through {@link Math#floor(double)}.</li>
 *   <li>All overloads return {@code null} when any argument is {@code null}.</li>
 * </ul>
 */
public final class FloorDiv {

    private FloorDiv() {
        // Utility class
    }

    public static Byte floorDiv(Byte dividend, Byte divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return (byte) Math.floorDiv(dividend, divisor);
    }

    public static Short floorDiv(Short dividend, Short divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return (short) Math.floorDiv(dividend, divisor);
    }

    public static Integer floorDiv(Integer dividend, Integer divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return Math.floorDiv(dividend, divisor);
    }

    public static Long floorDiv(Long dividend, Long divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return Math.floorDiv(dividend, divisor);
    }

    public static Float floorDiv(Float dividend, Float divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return (float) Math.floor(dividend / divisor);
    }

    public static Double floorDiv(Double dividend, Double divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return DecimalArithmetic.wholeQuotient(dividend, divisor, true);
    }

    public static BigInteger floorDiv(BigInteger dividend, BigInteger divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        var divMod = dividend.divideAndRemainder(divisor);
        var quotient = divMod[0];
        var remainder = divMod[1];
        // divideAndRemainder truncates toward zero; adjust only when signs differ and remainder != 0.
        if (remainder.signum() != 0 && remainder.signum() != divisor.signum()) {
            quotient = quotient.subtract(BigInteger.ONE);
        }
        return quotient;
    }

    public static BigDecimal floorDiv(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return dividend.divide(divisor, 0, RoundingMode.FLOOR);
    }
}
