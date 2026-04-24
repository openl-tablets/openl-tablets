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
 * Formula for floating-point:
 * <pre>{@code
 *   floor(dividend / divisor)
 * }</pre>
 * <p>
 * Contract:
 * <ul>
 *   <li>Return type matches the input type: {@link Byte}/{@link Short}/{@link Integer} → {@link Integer};
 *       {@link Long} → {@link Long}; {@link Float} → {@link Float}; {@link Double} → {@link Double};
 *       {@link java.math.BigInteger} → {@link java.math.BigInteger};
 *       {@link java.math.BigDecimal} → {@link java.math.BigDecimal}.</li>
 *   <li>Integer / BigInteger / BigDecimal divide-by-zero throws {@link ArithmeticException}.</li>
 *   <li>Float / double divide-by-zero follows IEEE 754: returns {@code ±Infinity} or {@code NaN}.</li>
 *   <li>{@code NaN} and {@code ±Infinity} inputs propagate naturally through {@link Math#floor(double)}.</li>
 *   <li>All overloads return {@code null} when any argument is {@code null}.</li>
 * </ul>
 */
public final class FloorDiv {

    private FloorDiv() {
        // Utility class
    }

    public static Integer floorDiv(Byte dividend, Byte divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return Math.floorDiv(dividend, divisor);
    }

    public static Integer floorDiv(Short dividend, Short divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return Math.floorDiv(dividend, divisor);
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
        return Math.floor(dividend / divisor);
    }

    public static BigInteger floorDiv(BigInteger dividend, BigInteger divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        BigInteger[] divMod = dividend.divideAndRemainder(divisor);
        BigInteger quotient = divMod[0];
        BigInteger remainder = divMod[1];
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
