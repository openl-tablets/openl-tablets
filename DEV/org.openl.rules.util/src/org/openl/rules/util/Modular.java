package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Excel-compatible {@code MOD} (floor-mod): the result has the same sign as the divisor, unlike Java's
 * {@code %} operator which takes the sign of the dividend.
 * <p>
 * Contract:
 * <ul>
 *   <li>{@code MOD(a, b) == 0} when {@code a} is exactly divisible by {@code b}.</li>
 *   <li>{@code MOD(a, 0) == a} (returns the dividend instead of throwing; deviates from Excel's
 *       {@code #DIV/0!} for rule-engine ergonomics).</li>
 *   <li>All overloads return {@code null} when any argument is {@code null}.</li>
 * </ul>
 * <p>
 * Formula (derived from Java's truncated remainder):
 * <pre>{@code
 *   r = a % b            // or remainder() for BigInteger/BigDecimal
 *   if (r == 0)               return 0
 *   if (sign(r) != sign(b))   r += b
 *   return r
 * }</pre>
 */
public final class Modular {

    private Modular() {
        // Utility class
    }

    public static Byte mod(Byte dividend, Byte divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor == 0) {
            return dividend;
        }
        return (byte) Math.floorMod((int) dividend, (int) divisor);
    }

    public static Short mod(Short dividend, Short divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor == 0) {
            return dividend;
        }
        return (short) Math.floorMod((int) dividend, (int) divisor);
    }

    public static Integer mod(Integer dividend, Integer divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor == 0) {
            return dividend;
        }
        return Math.floorMod(dividend, divisor);
    }

    public static Long mod(Long dividend, Long divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor == 0L) {
            return dividend;
        }
        return Math.floorMod(dividend, divisor);
    }

    public static Float mod(Float dividend, Float divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor == 0.0f) {
            return dividend;
        }
        return modFloat(dividend, divisor);
    }

    public static Double mod(Double dividend, Double divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor == 0.0d) {
            return dividend;
        }
        return modDouble(dividend, divisor);
    }

    public static BigInteger mod(BigInteger dividend, BigInteger divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor.signum() == 0) {
            return dividend;
        }
        // BigInteger.mod requires a positive divisor, so use the sign-agnostic fallback form.
        return dividend.remainder(divisor).add(divisor).remainder(divisor);
    }

    public static BigDecimal mod(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor.signum() == 0) {
            return dividend;
        }
        BigDecimal remainder = dividend.remainder(divisor);
        if (remainder.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return remainder.signum() != divisor.signum() ? remainder.add(divisor) : remainder;
    }

    private static float modFloat(float dividend, float divisor) {
        if (divisor == 0.0f) {
            throw new ArithmeticException("/ by zero");
        }
        float remainder = dividend % divisor;
        if (remainder == 0.0f || Float.isInfinite(divisor) || Float.isNaN(remainder)) {
            return remainder;
        }
        return hasDifferentSigns(remainder, divisor) ? remainder + divisor : remainder;
    }

    private static double modDouble(double dividend, double divisor) {
        if (divisor == 0.0d) {
            throw new ArithmeticException("/ by zero");
        }
        double remainder = dividend % divisor;
        if (remainder == 0.0d || Double.isInfinite(divisor) || Double.isNaN(remainder)) {
            return remainder;
        }
        return hasDifferentSigns(remainder, divisor) ? remainder + divisor : remainder;
    }

    private static boolean hasDifferentSigns(float a, float b) {
        return (a < 0.0f && b > 0.0f) || (a > 0.0f && b < 0.0f);
    }

    private static boolean hasDifferentSigns(double a, double b) {
        return (a < 0.0d && b > 0.0d) || (a > 0.0d && b < 0.0d);
    }
}
