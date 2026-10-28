package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Excel-compatible {@code MOD} (floor-mod): for finite, non-zero divisors the result has the same sign
 * as the divisor, unlike Java's {@code %} operator which takes the sign of the dividend.
 * <p>
 * Contract:
 * <ul>
 *   <li>{@code MOD(a, b) == 0} when {@code a} is exactly divisible by {@code b}, a positive zero for
 *       floating-point types.</li>
 *   <li>{@code MOD(a, 0) == a} (returns the dividend instead of throwing; deviates from Excel's
 *       {@code #DIV/0!} for rule-engine ergonomics).</li>
 *   <li>All overloads return {@code null} when any argument is {@code null}.</li>
 *   <li>Double operands count as the decimals they show, a computed number of 16 or 17 digits as its double
 *       ({@link DecimalArithmetic}): {@code mod(1.1, 0.1) == 0.0}.
 *       The remainder plus the divisor is rounded once, and a remainder that rounds to the divisor is zero. A
 *       dividend of the other sign, smaller than a rounding of the divisor, leaves a sum nearest to the divisor
 *       itself: {@code mod(-1e-20, 1.0) == 1.0}, with {@code floorDiv(-1e-20, 1.0) == -1.0}.</li>
 *   <li><b>Floating-point edge cases</b> — the sign-of-divisor guarantee applies to finite divisors only.
 *       For {@link #mod(Float, Float)} / {@link #mod(Double, Double)}:
 *       <ul>
 *         <li>{@code mod(a, ±Infinity) == a % ±Infinity == a} — sign follows the dividend, no floor-mod
 *             adjustment is applied (it would produce {@code ±Infinity} and is not meaningful).</li>
 *         <li>{@code mod(a, NaN)} / {@code mod(NaN, b)} propagate {@code NaN}.</li>
 *         <li>{@code mod(±Infinity, b)} yields {@code NaN} via Java's {@code %}.</li>
 *       </ul>
 *   </li>
 * </ul>
 * <p>
 * Formula (derived from Java's truncated remainder; applies to finite, non-zero {@code b}):
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
        return (byte) Math.floorMod(dividend, divisor);
    }

    public static Short mod(Short dividend, Short divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor == 0) {
            return dividend;
        }
        return (short) Math.floorMod(dividend, divisor);
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
        var remainder = dividend.remainder(divisor);
        if (remainder.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return remainder.signum() != divisor.signum() ? remainder.add(divisor) : remainder;
    }

    private static float modFloat(float dividend, float divisor) {
        if (divisor == 0.0f) {
            throw new ArithmeticException("/ by zero");
        }
        var remainder = dividend % divisor;
        if (Float.isInfinite(divisor) || Float.isNaN(remainder)) {
            return remainder;
        }
        if (remainder == 0.0f) {
            return 0.0f;
        }
        return hasDifferentSigns(remainder, divisor) ? remainder + divisor : remainder;
    }

    private static double modDouble(double dividend, double divisor) {
        if (divisor == 0.0d) {
            throw new ArithmeticException("/ by zero");
        }
        var mod = DecimalArithmetic.floorMod(dividend, divisor);
        return mod == 0.0d && !Double.isInfinite(divisor) ? 0.0d : mod;
    }

    private static boolean hasDifferentSigns(float a, float b) {
        return (a < 0.0f && b > 0.0f) || (a > 0.0f && b < 0.0f);
    }
}
