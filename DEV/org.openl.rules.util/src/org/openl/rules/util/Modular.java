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
 *   <li>Division by zero throws {@link ArithmeticException} (Excel's {@code #DIV/0!}).</li>
 *   <li>Reference-type overloads ({@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float},
 *       {@link Double}, {@link java.math.BigInteger}, {@link java.math.BigDecimal}) return {@code null}
 *       when any argument is {@code null}.</li>
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
public class Modular {

    public static byte mod(byte dividend, byte divisor) {
        return (byte) mod(dividend, (int) divisor);
    }

    public static Byte mod(Byte dividend, Byte divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return mod((byte) dividend, (byte) divisor);
    }

    public static short mod(short dividend, short divisor) {
        return (short) mod(dividend, (int) divisor);
    }

    public static Short mod(Short dividend, Short divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return mod((short) dividend, (short) divisor);
    }

    public static int mod(int dividend, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("/ by zero");
        }

        int remainder = dividend % divisor;
        if (remainder == 0) {
            return 0;
        }

        return hasDifferentSigns(remainder, divisor)
                ? remainder + divisor
                : remainder;
    }

    public static Integer mod(Integer dividend, Integer divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return mod((int) dividend, (int) divisor);
    }

    public static long mod(long dividend, long divisor) {
        if (divisor == 0L) {
            throw new ArithmeticException("/ by zero");
        }

        long remainder = dividend % divisor;
        if (remainder == 0L) {
            return 0L;
        }

        return hasDifferentSigns(remainder, divisor)
                ? remainder + divisor
                : remainder;
    }

    public static Long mod(Long dividend, Long divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return mod((long) dividend, (long) divisor);
    }

    public static float mod(float dividend, float divisor) {
        if (divisor == 0.0f) {
            throw new ArithmeticException("/ by zero");
        }

        float remainder = dividend % divisor;
        if (remainder == 0.0f || Float.isInfinite(divisor) || Float.isNaN(remainder)) {
            // Infinite divisor: Java's % already gives the IEEE-correct result (|remainder| < |divisor|
            // trivially holds); no floor-mod adjustment applies. NaN propagates.
            return remainder;
        }

        return hasDifferentSigns(remainder, divisor)
                ? remainder + divisor
                : remainder;
    }

    public static Float mod(Float dividend, Float divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return mod((float) dividend, (float) divisor);
    }

    public static double mod(double dividend, double divisor) {
        if (divisor == 0.0d) {
            throw new ArithmeticException("/ by zero");
        }

        double remainder = dividend % divisor;
        if (remainder == 0.0d || Double.isInfinite(divisor) || Double.isNaN(remainder)) {
            // Infinite divisor: Java's % already gives the IEEE-correct result; no floor-mod adjustment
            // applies. NaN propagates.
            return remainder;
        }

        return hasDifferentSigns(remainder, divisor)
                ? remainder + divisor
                : remainder;
    }

    public static Double mod(Double dividend, Double divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return mod((double) dividend, (double) divisor);
    }

    public static BigInteger mod(BigInteger dividend, BigInteger divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor.signum() == 0) {
            throw new ArithmeticException("BigInteger divide by zero");
        }

        BigInteger remainder = dividend.remainder(divisor);
        if (remainder.signum() == 0) {
            return BigInteger.ZERO;
        }

        return remainder.signum() != divisor.signum()
                ? remainder.add(divisor)
                : remainder;
    }

    public static BigDecimal mod(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        if (divisor.signum() == 0) {
            throw new ArithmeticException("BigDecimal divide by zero");
        }

        BigDecimal remainder = dividend.remainder(divisor);
        if (remainder.signum() == 0) {
            return BigDecimal.ZERO;
        }

        return remainder.signum() != divisor.signum()
                ? remainder.add(divisor)
                : remainder;
    }

    private static boolean hasDifferentSigns(int a, int b) {
        return (a < 0 && b > 0) || (a > 0 && b < 0);
    }

    private static boolean hasDifferentSigns(long a, long b) {
        return (a < 0L && b > 0L) || (a > 0L && b < 0L);
    }

    private static boolean hasDifferentSigns(float a, float b) {
        return (a < 0.0f && b > 0.0f) || (a > 0.0f && b < 0.0f);
    }

    private static boolean hasDifferentSigns(double a, double b) {
        return (a < 0.0d && b > 0.0d) || (a > 0.0d && b < 0.0d);
    }
}
