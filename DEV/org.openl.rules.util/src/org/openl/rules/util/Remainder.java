package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Truncated remainder: the result has the same sign as the dividend, matching Java's {@code %} operator
 * and {@link BigInteger#remainder(BigInteger)} / {@link BigDecimal#remainder(BigDecimal)}. Satisfies the
 * identity {@code remainder = dividend - divisor * quotient}, where {@code quotient} truncates toward zero
 * (see {@link Quotient}).
 * <p>
 * Distinct from {@link Modular} (Excel's {@code MOD}, floor-mod, same sign as divisor); the two agree when
 * operands share a sign and differ by {@code divisor} otherwise.
 * <p>
 * Contract:
 * <ul>
 *   <li>{@code remainder(a, b) == 0} when {@code a} is exactly divisible by {@code b}.</li>
 *   <li>Division by zero throws {@link ArithmeticException}.</li>
 *   <li>Reference-type overloads ({@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float},
 *       {@link Double}, {@link java.math.BigInteger}, {@link java.math.BigDecimal}) return {@code null}
 *       when any argument is {@code null}.</li>
 * </ul>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Remainder">Remainder (Wikipedia)</a>
 */
public class Remainder {

    public static byte remainder(byte dividend, byte divisor) {
        return (byte) remainder((int) dividend, (int) divisor);
    }

    public static Byte remainder(Byte dividend, Byte divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return remainder((byte) dividend, (byte) divisor);
    }

    public static short remainder(short dividend, short divisor) {
        return (short) remainder((int) dividend, (int) divisor);
    }

    public static Short remainder(Short dividend, Short divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return remainder((short) dividend, (short) divisor);
    }

    public static int remainder(int dividend, int divisor) {
        return dividend % divisor;
    }

    public static Integer remainder(Integer dividend, Integer divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return remainder((int) dividend, (int) divisor);
    }

    public static long remainder(long dividend, long divisor) {
        return dividend % divisor;
    }

    public static Long remainder(Long dividend, Long divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return remainder((long) dividend, (long) divisor);
    }

    public static float remainder(float dividend, float divisor) {
        if (divisor == 0.0f) {
            throw new ArithmeticException("/ by zero");
        }
        // Java's float % is defined (JLS 15.17.3) as a - b * trunc(a/b), the same identity with
        // Quotient, but implemented in IEEE 754 so NaN/Infinity propagate naturally.
        return dividend % divisor;
    }

    public static Float remainder(Float dividend, Float divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return remainder((float) dividend, (float) divisor);
    }

    public static double remainder(double dividend, double divisor) {
        if (divisor == 0.0d) {
            throw new ArithmeticException("/ by zero");
        }
        return dividend % divisor;
    }

    public static Double remainder(Double dividend, Double divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return remainder((double) dividend, (double) divisor);
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
}
