package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The shortest decimal of a double, {@code digits * 10^exponent}: the one {@link Double#toString(double)} shows.
 * <p>
 * The decimal is packed in a {@code long}, so it costs no object: the digits without their sign and trailing zeros,
 * and the exponent as an offset from a power of ten the double itself tells. The double is passed along with it to
 * read it back. The decimal is found without building a string: a decimal with few digits by scaling the double by
 * powers of ten, any other one by the algorithm {@link Double#toString(double)} uses (Schubfach, by Raffaello
 * Giulietti).
 * <p>
 * Only doubles from {@code 2^-900} to {@code 2^901} in magnitude have one here: zero, the doubles nearest to zero,
 * the largest doubles, infinities and {@code NaN} get {@link #NONE}.
 */
final class Decimal {

    /**
     * The decimal of a double out of the range of this class.
     */
    static final long NONE = -1;

    // Powers of ten a double holds exactly.
    static final double[] POW10 = {1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14,
            1e15, 1e16, 1e17, 1e18, 1e19, 1e20, 1e21, 1e22};

    // The exponent offset takes the low bits, the digits, below 10^17, the 57 bits above.
    private static final int OFFSET_BITS = 5;
    private static final int OFFSET_MASK = (1 << OFFSET_BITS) - 1;
    // Biased exponents of the doubles that have a decimal here.
    private static final int MIN_BIASED = 1023 - 900;
    private static final int MAX_BIASED = 1023 + 900;
    private static final long C_MIN = 1L << 52;
    private static final long MASK_63 = (1L << 63) - 1;
    // Fraction digits a decimal is searched with by scaling: most numbers of business have fewer.
    private static final int SCALED_DIGITS = 8;
    // Up to it, rounding the scaled double gives the digits of its decimal: the error is below a quarter.
    private static final double MAX_SCALED = 0x1p50;
    // The digits have at most 17 digits, so at most 16 trailing zeros.
    private static final int MAX_ZEROS = 16;
    // A packed decimal of at least 16 digits: more than every decimal a double keeps.
    private static final long MIN_LONG = 1_000_000_000_000_000L << OFFSET_BITS;
    // 5^-z modulo 2^64: multiplying by it divides by 5^z whatever divides exactly.
    private static final long[] INVERSE_POW5 = new long[MAX_ZEROS + 1];
    // (2^64 - 1) / 5^z, unsigned: a product above it tells the division by 5^z is inexact.
    private static final long[] MAX_QUOTIENT_POW5 = new long[MAX_ZEROS + 1];

    static {
        var modulus = BigInteger.ONE.shiftLeft(64);
        for (var z = 0; z <= MAX_ZEROS; z++) {
            var pow5 = BigInteger.valueOf(5).pow(z);
            INVERSE_POW5[z] = pow5.modInverse(modulus).longValue();
            MAX_QUOTIENT_POW5[z] = Long.divideUnsigned(-1L, pow5.longValue());
        }
    }

    private Decimal() {
        // Utility class
    }

    /**
     * The shortest decimal of {@code x}, or {@link #NONE} when {@code x} is out of the range of this class.
     */
    static long of(double x) {
        var bits = Double.doubleToRawLongBits(x);
        var biased = (int) (bits >>> 52) & 0x7ff;
        if (biased < MIN_BIASED || biased > MAX_BIASED) {
            return NONE;
        }
        var k = base(bits);
        var scale = scale(x);
        if (scale > 0) {
            // The last digit is not zero: with it, a smaller scale would have been found.
            return pack((long) Math.rint(Math.abs(x) * POW10[scale]), -scale - k);
        }
        if (scale == 0) {
            return withoutZeros((long) Math.abs(x), -k);
        }
        // |x| = c * 2^q
        var q = biased - 1075;
        var c = C_MIN | bits & (C_MIN - 1);
        return withoutZeros(shortest(c, q, k), 0);
    }

    /**
     * Whether the decimal {@code d} has 16 or 17 digits. A double keeps every decimal of at most 15 digits, so such a
     * decimal is the rounding of a result, not a number written as it is.
     */
    static boolean isLong(long d) {
        return d >= MIN_LONG;
    }

    /**
     * The digits of the decimal {@code d} of {@code x}, with the sign of {@code x}.
     */
    static long digits(double x, long d) {
        var digits = d >>> OFFSET_BITS;
        return x < 0 ? -digits : digits;
    }

    /**
     * The power of ten the digits of the decimal {@code d} of {@code x} are scaled by.
     */
    static int exponent(double x, long d) {
        return base(Double.doubleToRawLongBits(x)) + (int) (d & OFFSET_MASK);
    }

    /**
     * The decimal {@code d} minus {@code x}, the double it rounds to, within {@code 2^-101 * |x|}.
     */
    static double error(double x, long d) {
        var exponent = exponent(x, d);
        var high = PowersOfTen.high(exponent);
        var low = PowersOfTen.low(exponent);
        var digits = digits(x, d);
        var c = (double) digits;
        // The digits beyond a double, exact: the digits are below 2^57
        var cLow = digits - (long) c;
        var product = c * high;
        var productLow = ExactProduct.error(c, high, product) + (c * low + (double) cLow * high);
        // The product is within a factor of 2 of x, so their difference is exact.
        return product - x + productLow;
    }

    /**
     * The exact value of a finite {@code x}, with all the binary digits of the double.
     */
    static BigDecimal exactValue(double x) {
        if (x == 0) {
            return BigDecimal.ZERO;
        }
        // x = m * 2^k for an integer m below 2^53
        var k = Math.max(Math.getExponent(x), Double.MIN_EXPONENT) - (Double.PRECISION - 1);
        var m = BigInteger.valueOf((long) Math.scalb(x, -k));
        return k >= 0 ? new BigDecimal(m.shiftLeft(k)) : new BigDecimal(m.multiply(BigInteger.valueOf(5).pow(-k)), -k);
    }

    /**
     * The decimal {@code d} of {@code x}.
     */
    static BigDecimal toBigDecimal(double x, long d) {
        return BigDecimal.valueOf(digits(x, d), -exponent(x, d));
    }

    /**
     * A power of ten from the binary exponent of a double: the one Schubfach finds the digits at. The exponent of
     * the decimal is at most 17 above it.
     */
    private static int base(long bits) {
        var q = ((int) (bits >>> 52) & 0x7ff) - 1075;
        return (bits & C_MIN - 1) == 0 ? flog10threeQuartersPow2(q) : flog10pow2(q);
    }

    /**
     * The number of fraction digits of the shortest decimal of {@code x}, when it has at most
     * {@link #SCALED_DIGITS} of them and its digits are below {@link #MAX_SCALED}; {@code -1} otherwise.
     */
    private static int scale(double x) {
        for (var s = 0; s <= SCALED_DIGITS; s++) {
            var m = Math.rint(x * POW10[s]);
            if (!(Math.abs(m) < MAX_SCALED)) {
                return -1;
            }
            if (m / POW10[s] == x) {
                return s;
            }
        }
        return -1;
    }

    /**
     * The decimal of positive {@code digits} with the exponent offset {@code offset}, with the trailing zeros of
     * the digits moved to the offset.
     */
    private static long withoutZeros(long digits, int offset) {
        // 10^z divides the digits when 2^z does and 5^z divides what is left: the most zeros first.
        var z = Math.min(Long.numberOfTrailingZeros(digits), MAX_ZEROS);
        var quotient = (digits >>> z) * INVERSE_POW5[z];
        while (Long.compareUnsigned(quotient, MAX_QUOTIENT_POW5[z]) > 0) {
            z--;
            quotient = (digits >>> z) * INVERSE_POW5[z];
        }
        return pack(quotient, offset + z);
    }

    private static long pack(long digits, int offset) {
        return offset < 0 || offset > OFFSET_MASK ? NONE : digits << OFFSET_BITS | offset;
    }

    /**
     * The digits of the shortest decimal of {@code c * 2^q} at the power of ten {@code k}, maybe with trailing zeros.
     */
    private static long shortest(long c, int q, int k) {
        var out = (int) c & 1;
        var cb = c << 2;
        var cbr = cb + 2;
        var cbl = c == C_MIN ? cb - 1 : cb - 2;
        var h = q + PowersOfTen.flog2pow10(-k) + 2;
        var g1 = PowersOfTen.g1(-k);
        var g0 = PowersOfTen.g0(-k);
        var vb = roundToOdd(g1, g0, cb << h);
        var vbl = roundToOdd(g1, g0, cbl << h);
        var vbr = roundToOdd(g1, g0, cbr << h);
        var s = vb >> 2;
        if (s >= 100) {
            // One digit less, when exactly one of the two decimals around lies in the rounding interval.
            var sp10 = 10 * (s / 10);
            var tp10 = sp10 + 10;
            var upin = vbl + out <= sp10 << 2;
            var wpin = (tp10 << 2) + out <= vbr;
            if (upin != wpin) {
                return upin ? sp10 : tp10;
            }
        }
        var t = s + 1;
        var uin = vbl + out <= s << 2;
        var win = (t << 2) + out <= vbr;
        if (uin != win) {
            return uin ? s : t;
        }
        // Both lie in the rounding interval: the closer one, the even one on a tie.
        var cmp = vb - (s + t << 1);
        return cmp < 0 || cmp == 0 && (s & 1) == 0 ? s : t;
    }

    /**
     * {@code floor(g * cp / 2^127)}, made odd when inexact.
     */
    private static long roundToOdd(long g1, long g0, long cp) {
        var x1 = Math.multiplyHigh(g0, cp);
        var y0 = g1 * cp;
        var y1 = Math.multiplyHigh(g1, cp);
        var z = (y0 >>> 1) + x1;
        var vbp = y1 + (z >>> 63);
        return vbp | (z & MASK_63) + MASK_63 >>> 63;
    }

    // floor(log10(2^e))
    static int flog10pow2(int e) {
        return (int) (e * 661_971_961_083L >> 41);
    }

    // floor(log10(3/4 * 2^e))
    private static int flog10threeQuartersPow2(int e) {
        return (int) (e * 661_971_961_083L + -274_743_187_321L >> 41);
    }
}
