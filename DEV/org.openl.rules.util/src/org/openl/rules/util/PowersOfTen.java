package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Powers of ten from {@code 10^MIN} to {@code 10^MAX}, with more digits than a double holds.
 * <p>
 * Each power is held twice. As a pair of doubles whose sum is within {@code 2^-106} of it, for arithmetic with
 * twice the digits of a double. As an integer {@code g} of 126 bits with {@code 10^e = g * 2^r} for the shortest
 * decimal of a double, where {@code g} is rounded up.
 * <p>
 * The tables are built once, when the first number needs them. Both ends keep every part of a pair a normal double.
 */
final class PowersOfTen {

    static final int MIN = -290;
    static final int MAX = 290;

    private static final long MASK_63 = (1L << 63) - 1;
    private static final double[] HIGH = new double[MAX - MIN + 1];
    private static final double[] LOW = new double[MAX - MIN + 1];
    private static final long[] G1 = new long[MAX - MIN + 1];
    private static final long[] G0 = new long[MAX - MIN + 1];

    static {
        for (var e = MIN; e <= MAX; e++) {
            var exact = BigDecimal.ONE.scaleByPowerOfTen(e);
            var high = exact.doubleValue();
            HIGH[e - MIN] = high;
            LOW[e - MIN] = exact.subtract(Decimal.exactValue(high)).doubleValue();

            var power = BigInteger.TEN.pow(Math.abs(e));
            var r = flog2pow10(e) - 125;
            BigInteger g;
            if (e < 0) {
                g = BigInteger.ONE.shiftLeft(-r).divide(power);
            } else {
                g = r < 0 ? power.shiftLeft(-r) : power.shiftRight(r);
            }
            g = g.add(BigInteger.ONE);
            G1[e - MIN] = g.shiftRight(63).longValue();
            G0[e - MIN] = g.longValue() & MASK_63;
        }
    }

    private PowersOfTen() {
        // Utility class
    }

    /**
     * The double nearest to {@code 10^e}.
     */
    static double high(int e) {
        return HIGH[e - MIN];
    }

    /**
     * The double nearest to {@code 10^e - high(e)}.
     */
    static double low(int e) {
        return LOW[e - MIN];
    }

    /**
     * The upper 63 bits of {@code g} for {@code 10^e}.
     */
    static long g1(int e) {
        return G1[e - MIN];
    }

    /**
     * The lower 63 bits of {@code g} for {@code 10^e}.
     */
    static long g0(int e) {
        return G0[e - MIN];
    }

    /**
     * {@code floor(log2(10^e))}, exact for {@code |e| <= 400}.
     */
    static int flog2pow10(int e) {
        return (int) (e * 913_124_641_741L >> 38);
    }
}
