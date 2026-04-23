package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Excel-compatible {@code QUOTIENT}: returns the integer portion of the division, truncated toward zero.
 * Unlike Excel's {@code INT} (which floors toward negative infinity), {@code QUOTIENT} always discards the
 * fractional part: {@code QUOTIENT(-10, 3) == -3}, whereas {@code INT(-10/3) == -4}.
 * <p>
 * Contract:
 * <ul>
 *   <li>Primitive overloads return the natural Java arithmetic type: {@code int} for
 *       {@code byte}/{@code short}/{@code int} inputs (Java widens smaller integer types to {@code int}
 *       for arithmetic), {@code long} for {@code long}/{@code float}/{@code double} inputs.</li>
 *   <li>{@link java.math.BigInteger} and {@link java.math.BigDecimal} overloads return
 *       {@link java.math.BigInteger} — full precision, no overflow.</li>
 *   <li>Division by zero throws {@link ArithmeticException} (Excel's {@code #DIV/0!}).</li>
 *   <li>Reference-type overloads ({@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float},
 *       {@link Double}, {@link java.math.BigInteger}, {@link java.math.BigDecimal}) return {@code null}
 *       when any argument is {@code null}.</li>
 * </ul>
 */
public class Quotient {

    public static int quotient(byte dividend, byte divisor) {
        return quotient(dividend, (int) divisor);
    }

    public static Integer quotient(Byte dividend, Byte divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return quotient((byte) dividend, (byte) divisor);
    }

    public static int quotient(short dividend, short divisor) {
        return quotient(dividend, (int) divisor);
    }

    public static Integer quotient(Short dividend, Short divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return quotient((short) dividend, (short) divisor);
    }

    public static int quotient(int dividend, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("/ by zero");
        }
        return dividend / divisor;
    }

    public static Integer quotient(Integer dividend, Integer divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return quotient((int) dividend, (int) divisor);
    }

    public static long quotient(long dividend, long divisor) {
        if (divisor == 0L) {
            throw new ArithmeticException("/ by zero");
        }
        return dividend / divisor;
    }

    public static Long quotient(Long dividend, Long divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return quotient((long) dividend, (long) divisor);
    }

    public static long quotient(float dividend, float divisor) {
        if (divisor == 0.0f) {
            throw new ArithmeticException("/ by zero");
        }
        if (Float.isNaN(dividend) || Float.isNaN(divisor)
                || Float.isInfinite(dividend) || Float.isInfinite(divisor)) {
            throw new ArithmeticException("NaN or Infinity has no integer quotient");
        }
        return (long) (dividend / divisor);
    }

    public static Long quotient(Float dividend, Float divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return quotient((float) dividend, (float) divisor);
    }

    public static long quotient(double dividend, double divisor) {
        if (divisor == 0.0d) {
            throw new ArithmeticException("/ by zero");
        }
        if (Double.isNaN(dividend) || Double.isNaN(divisor)
                || Double.isInfinite(dividend) || Double.isInfinite(divisor)) {
            throw new ArithmeticException("NaN or Infinity has no integer quotient");
        }
        return (long) (dividend / divisor);
    }

    public static Long quotient(Double dividend, Double divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return quotient((double) dividend, (double) divisor);
    }

    public static BigInteger quotient(BigInteger dividend, BigInteger divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return dividend.divide(divisor);
    }

    public static BigInteger quotient(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null) {
            return null;
        }
        return dividend.divideToIntegralValue(divisor).toBigInteger();
    }
}
