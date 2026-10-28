package org.openl.rules.util;

/**
 * The exact rounding error of a product of doubles and the exact remainder of a quotient, as {@link Math#fma} gives
 * them, equally fast on every processor.
 * <p>
 * Without the fused multiply-add instruction of the processor, as on x86-64 processors before Haswell or in virtual
 * machines that hide it, the JVM computes {@link Math#fma} with {@link java.math.BigDecimal}, hundreds of times
 * slower. Here each factor is split into two halves, whose products a double holds exactly (Dekker), with no such
 * instruction.
 * <p>
 * The results are exact for products of at least {@code 2^-960} and factors below {@code 2^996} in magnitude, the
 * range of the decimal arithmetic.
 */
final class ExactProduct {

    // 2^27 + 1: a factor times it splits into two halves of 26 bits
    private static final double SPLITTER = 0x1p27 + 1;

    private ExactProduct() {
        // Utility class
    }

    /**
     * {@code a * b - product} for the rounded {@code product} of {@code a * b}.
     */
    static double error(double a, double b, double product) {
        var aHigh = upperHalf(a);
        var aLow = a - aHigh;
        var bHigh = upperHalf(b);
        var bLow = b - bHigh;
        return aLow * bLow - (product - aHigh * bHigh - aLow * bHigh - aHigh * bLow);
    }

    /**
     * {@code x - quotient * y} for the rounded {@code quotient} of {@code x / y}.
     */
    static double remainder(double x, double y, double quotient) {
        var product = quotient * y;
        // The product is within a rounding of x, so their difference is exact, and so is the remainder.
        return x - product - error(quotient, y, product);
    }

    private static double upperHalf(double a) {
        var t = SPLITTER * a;
        return t - (t - a);
    }
}
