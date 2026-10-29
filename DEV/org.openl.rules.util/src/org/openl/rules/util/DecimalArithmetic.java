package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Arithmetic of doubles as of the decimal numbers they show: {@code 0.1 + 0.2} gives {@code 0.3}, not
 * {@code 0.30000000000000004}, and {@code 1.1 % 0.1} gives {@code 0.0}.
 * <p>
 * An operand of at most 15 significant digits counts as its shortest decimal, the one {@link Double#toString(double)}
 * shows and {@link BigDecimal#valueOf(double)} gives. A double keeps every such decimal as it is written. An operand
 * of 16 or 17 digits with a fraction is a computed one, the rounding of a result such as {@code 1 / 3}. It counts as
 * its double, the nearer value to that result. The result is the double nearest to the exact result of the operation
 * on these values, ties to even. A quotient that has no finite decimal is rounded the same way, once.
 * <p>
 * A computed operand carries a rounding, which reaches the result. When the result lies within that rounding and
 * within half a unit of its 15th significant digit of a decimal of at most 13 digits, it is that decimal. So
 * {@code 1 / 3 * 3} is {@code 1.0} and {@code 9 / 35 * 7} is {@code 1.8}, while {@code 6910158952078997 + 1 / 30}
 * keeps all its digits.
 * <p>
 * A remainder is smaller than the divisor: one that would round to the divisor is zero, so
 * {@code 0.9000000000000001 % 0.30000000000000004} is {@code 0.0}, as the quotient {@code 3.0} tells.
 * <p>
 * Special cases follow IEEE 754, as the Java operators do: {@code NaN} and infinite operands, division by zero,
 * overflow and the sign of a zero result. This class is a helper of the operators and functions of rules, not a
 * library of rules itself.
 */
public final class DecimalArithmetic {

    private static final double[] POW10 = Decimal.POW10;
    private static final long[] LONG_POW10 = {1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L,
            100_000_000L, 1_000_000_000L, 10_000_000_000L, 100_000_000_000L, 1_000_000_000_000L,
            10_000_000_000_000L, 100_000_000_000_000L, 1_000_000_000_000_000L, 10_000_000_000_000_000L,
            100_000_000_000_000_000L, 1_000_000_000_000_000_000L};
    // Long.MAX_VALUE / 10^k: the largest long that a long holds multiplied by 10^k.
    private static final long[] MAX_TIMES_POW10 = new long[LONG_POW10.length];

    static {
        for (var k = 0; k < LONG_POW10.length; k++) {
            MAX_TIMES_POW10[k] = Long.MAX_VALUE / LONG_POW10[k];
        }
    }

    // Up to it, a double holds every integer, so integer operations on doubles are exact.
    private static final double MAX_EXACT = 0x1p53;
    // Below it, a rounded quotient lies within a quarter of the exact one.
    private static final double MAX_WHOLE = 0x1p52;
    // Results of the arithmetic with twice the digits of a double are kept in this range, away from the limits.
    private static final double MIN_RESULT = 0x1p-900;
    private static final double MAX_RESULT = 0x1p900;
    // Bits of a quotient: two more than a double holds, so rounding it once more is exact.
    private static final int QUOTIENT_BITS = 55;
    // The largest shift that keeps a quotient of QUOTIENT_BITS bits a normal double: 2^54 * 2^-1076 is 2^-1022.
    private static final int MAX_NORMAL_SHIFT = QUOTIENT_BITS - 1 - Double.MIN_EXPONENT;
    // The doubles below 2^-1022 are the multiples of 2^-1074.
    private static final int SUBNORMAL_SHIFT = Double.PRECISION - 1 - Double.MIN_EXPONENT;
    // A result of a computed operand within half a unit of its 15th digit of a decimal of 13 digits is that decimal.
    private static final int SNAP_DIGITS = 13;
    private static final MathContext SNAP_ROUNDING = new MathContext(SNAP_DIGITS + 2, RoundingMode.HALF_EVEN);
    // The distance of |r| scaled to 13 whole digits from a whole number: half a unit of the 15th digit and the
    // rounding of the scaling.
    private static final double SNAP_FILTER = 0.008;
    // The rounding a computed operand carries, in units of its last place: its own and those of earlier operations.
    private static final double CARRIED_ULPS = 2;

    private DecimalArithmetic() {
        // Utility class
    }

    public static double add(double x, double y) {
        // Equal decimals cancel, and an integer is its own decimal
        if (x == 0 || y == 0 || !Double.isFinite(x) || !Double.isFinite(y) || x == -y || areIntegers(x, y)) {
            return x + y;
        }
        var dx = Decimal.of(x);
        var dy = Decimal.of(y);
        var cx = isComputed(x, dx);
        var cy = isComputed(y, dy);
        if (cx || cy) {
            // The operator rounds the exact sum of doubles once
            var r = areDoubles(x, cx, y, cy) ? x + y : sum(x, dx, cx, y, dy, cy);
            return snap(r, rounding(x, cx) + rounding(y, cy));
        }
        if (dx != Decimal.NONE && dy != Decimal.NONE) {
            // Whole digits at the smaller exponent, while a double holds them exactly.
            var ex = Decimal.exponent(x, dx);
            var ey = Decimal.exponent(y, dy);
            var s = Math.min(ex, ey);
            var a = digitsAt(Decimal.digits(x, dx), ex - s);
            var b = digitsAt(Decimal.digits(y, dy), ey - s);
            var c = a + b;
            if (isExact(a) && isExact(b) && isExact(c) && isExactPower(s)) {
                return scale(c, s);
            }
        }
        return sum(x, dx, false, y, dy, false);
    }

    public static double subtract(double x, double y) {
        return add(x, -y);
    }

    public static double multiply(double x, double y) {
        if (x == 0 || y == 0 || !Double.isFinite(x) || !Double.isFinite(y) || areIntegers(x, y)) {
            return x * y;
        }
        var dx = Decimal.of(x);
        var dy = Decimal.of(y);
        var cx = isComputed(x, dx);
        var cy = isComputed(y, dy);
        if (cx || cy) {
            var r = areDoubles(x, cx, y, cy) ? x * y : product(x, dx, cx, y, dy, cy);
            return snap(r, rounding(x, cx) * Math.abs(y) + rounding(y, cy) * Math.abs(x));
        }
        if (dx != Decimal.NONE && dy != Decimal.NONE) {
            var s = Decimal.exponent(x, dx) + Decimal.exponent(y, dy);
            var c = (double) Decimal.digits(x, dx) * Decimal.digits(y, dy);
            if (isExact(c) && isExactPower(s)) {
                return scale(c, s);
            }
        }
        return product(x, dx, false, y, dy, false);
    }

    public static double divide(double x, double y) {
        if (x == 0 || y == 0 || !Double.isFinite(x) || !Double.isFinite(y) || areIntegers(x, y)) {
            return x / y;
        }
        var dx = Decimal.of(x);
        var dy = Decimal.of(y);
        var cx = isComputed(x, dx);
        var cy = isComputed(y, dy);
        if (cx || cy) {
            var r = areDoubles(x, cx, y, cy) ? x / y : quotient(x, dx, cx, y, dy, cy);
            return snap(r, Math.abs(r) * (rounding(x, cx) / Math.abs(x) + rounding(y, cy) / Math.abs(y)));
        }
        if (dx != Decimal.NONE && dy != Decimal.NONE) {
            // x / y = (a / b) * 10^(ex - ey)
            var k = Decimal.exponent(x, dx) - Decimal.exponent(y, dy);
            if (isExactPower(k)) {
                var a = digitsAt(Decimal.digits(x, dx), Math.max(k, 0));
                var b = digitsAt(Decimal.digits(y, dy), Math.max(-k, 0));
                if (isExact(a) && isExact(b)) {
                    return a / b;
                }
            }
        }
        return quotient(x, dx, false, y, dy, false);
    }

    /**
     * The sum of the values {@code x} and {@code y} count as, rounded once: {@code cx} and {@code cy} tell computed
     * operands, which count as their doubles.
     */
    private static double sum(double x, long dx, boolean cx, double y, long dy, boolean cy) {
        if (dx != Decimal.NONE && dy != Decimal.NONE) {
            // The values as x and y with their errors, added with twice the digits of a double.
            var sum = x + y;
            var yPart = sum - x;
            var sumError = x - (sum - yPart) + (y - yPart);
            var error = sumError + (error(x, dx, cx) + error(y, dy, cy));
            var r = nearest(sum, error, (Math.abs(x) + Math.abs(y)) * 0x1p-98);
            if (!Double.isNaN(r)) {
                return r;
            }
        }
        return value(x, dx, cx).add(value(y, dy, cy)).doubleValue();
    }

    /**
     * The product of the values {@code x} and {@code y} count as, rounded once.
     */
    private static double product(double x, long dx, boolean cx, double y, long dy, boolean cy) {
        var product = x * y;
        if (dx != Decimal.NONE && dy != Decimal.NONE && isInRange(product)) {
            var productError = ExactProduct.error(x, y, product);
            var error = productError + (x * error(y, dy, cy) + y * error(x, dx, cx));
            var r = nearest(product, error, Math.abs(product) * 0x1p-97);
            if (!Double.isNaN(r)) {
                return r;
            }
        }
        return value(x, dx, cx).multiply(value(y, dy, cy)).doubleValue();
    }

    /**
     * The quotient of the values {@code x} and {@code y} count as, rounded once.
     */
    private static double quotient(double x, long dx, boolean cx, double y, long dy, boolean cy) {
        var quotient = x / y;
        if (dx != Decimal.NONE && dy != Decimal.NONE && isInRange(quotient)) {
            var remainder = ExactProduct.remainder(x, y, quotient);
            var error = (remainder + error(x, dx, cx) - quotient * error(y, dy, cy)) / y;
            var r = nearest(quotient, error, Math.abs(quotient) * 0x1p-96);
            if (!Double.isNaN(r)) {
                return r;
            }
        }
        return quotient(value(x, dx, cx), value(y, dy, cy));
    }

    /**
     * The double nearest to {@code a / b} for {@code b != 0}, ties to even.
     */
    static double quotient(BigDecimal a, BigDecimal b) {
        var k = b.scale() - a.scale();
        var n = k > 0 ? a.unscaledValue().multiply(BigInteger.TEN.pow(k)) : a.unscaledValue();
        var d = k < 0 ? b.unscaledValue().multiply(BigInteger.TEN.pow(-k)) : b.unscaledValue();
        var q = nearest(n.abs(), d.abs());
        return n.signum() == d.signum() ? q : -q;
    }

    public static double remainder(double x, double y) {
        return remainder(x, y, false);
    }

    /**
     * The remainder of {@code x / y} with the sign of {@code y}, the floor modulo. A remainder of the other sign is
     * replaced with its sum with the divisor, rounded once from the exact sum.
     * <p>
     * A remainder smaller than a rounding of the divisor leaves a sum nearest to the divisor itself:
     * {@code floorMod(-1e-20, 1.0)} is {@code 1.0}.
     */
    static double floorMod(double x, double y) {
        return remainder(x, y, true);
    }

    /**
     * The whole number of times {@code y} goes into {@code x}: the quotient rounded toward zero, or toward negative
     * infinity when {@code floor} is set. It leaves the rest {@link #remainder(double, double)} or
     * {@link #floorMod(double, double)} gives, so a remainder that rounds to the divisor counts as one more divisor.
     * <p>
     * A quotient of {@code 2^52} or more, an infinite divisor and a divisor below {@link Double#MIN_NORMAL} are
     * rounded from the rounded quotient.
     */
    static double wholeQuotient(double x, double y, boolean floor) {
        var q = divide(x, y);
        if (!(Math.abs(q) < MAX_WHOLE) || !(Math.abs(y) >= Double.MIN_NORMAL) || Double.isInfinite(y)) {
            return floor || q >= 0 ? Math.floor(q) : Math.ceil(q);
        }
        var r = remainder(x, y);
        if (r == 0) {
            // A multiple of the divisor, within a rounding
            return Math.rint(q);
        }
        var whole = q >= 0 ? Math.floor(q) : Math.ceil(q);
        if (whole == q && Math.abs(r) > Math.abs(y) * 0.5) {
            // The quotient rounds to a whole number that the exact one stays below
            whole = Math.copySign(Math.abs(whole) - 1, q);
        }
        return floor && (r < 0) != (y < 0) ? whole - 1 : whole;
    }

    private static double remainder(double x, double y, boolean floor) {
        if (x == 0 || y == 0 || !Double.isFinite(x) || !Double.isFinite(y)) {
            return x % y;
        }
        // The floor modulo of operands of different signs is the remainder plus the divisor
        var complement = floor && (x < 0) != (y < 0);
        if (Math.abs(x) < Math.abs(y)) {
            // A smaller dividend is the remainder itself: its value is smaller too.
            return complement ? add(x, y) : snapComputed(x);
        }
        if (areIntegers(x, y)) {
            var r = x % y;
            return complement && r != 0 ? r + y : r;
        }
        var dx = Decimal.of(x);
        var dy = Decimal.of(y);
        var cx = isComputed(x, dx);
        var cy = isComputed(y, dy);
        if (cx || cy) {
            return remainder(x, dx, cx, y, dy, cy, complement);
        }
        if (dx != Decimal.NONE && dy != Decimal.NONE) {
            var ex = Decimal.exponent(x, dx);
            var ey = Decimal.exponent(y, dy);
            var s = Math.min(ex, ey);
            var a = Math.abs(Decimal.digits(x, dx));
            var b = Math.abs(Decimal.digits(y, dy));
            var fa = digitsAt(a, ex - s);
            var fb = digitsAt(b, ey - s);
            if (isExact(fa) && isExact(fb) && isExactPower(s)) {
                var c = fa % fb;
                var r = scale(c, s);
                return complement && isRemainder(r, y) ? Math.copySign(scale(fb - c, s), y) : remainder(r, x, y);
            }
            // At the smaller exponent, the digits of y stay below those of x, so a long holds them.
            var m = ex > ey ? b : b * LONG_POW10[ey - ex];
            var c = ex > ey ? remainderTimesPow10(a % b, ex - ey, b) : a % m;
            var r = c == 0 ? 0.0 : nearest(c, s);
            if (complement && isRemainder(r, y)) {
                var sum = nearest(m - c, s);
                if (!Double.isNaN(sum)) {
                    return Math.copySign(sum, y);
                }
            } else if (!Double.isNaN(r)) {
                return remainder(r, x, y);
            }
        }
        var exact = decimal(x, dx).remainder(decimal(y, dy));
        var r = exact.doubleValue();
        if (complement && exact.signum() != 0 && Math.abs(r) < Math.abs(y)) {
            return exact.add(decimal(y, dy)).doubleValue();
        }
        return remainder(r, x, y);
    }

    /**
     * The remainder of operands one of which is computed: of the values they count as, rounded once and moved to a
     * shorter decimal as {@link #snap(double, double)} tells. With {@code complement}, a remainder that is neither
     * zero nor the divisor is replaced with its sum with the divisor.
     */
    private static double remainder(double x, long dx, boolean cx, double y, long dy, boolean cy, boolean complement) {
        // The rounding of x, and of y once per divisor taken away; the sum takes one more
        var noise = rounding(x, cx) + rounding(y, cy) * Math.floor(Math.abs(x / y));
        var sumNoise = noise + rounding(y, cy);
        if (areDoubles(x, cx, y, cy)) {
            // The remainder of doubles is exact, and its sum with the divisor is rounded once
            var rest = x % y;
            var r = snap(rest, noise);
            return complement && isRemainder(r, y) ? snap(rest + y, sumNoise) : remainder(r, x, y);
        }
        var divisor = value(y, dy, cy);
        var exact = value(x, dx, cx).remainder(divisor);
        var r = snap(exact.doubleValue(), noise);
        if (complement && isRemainder(r, y)) {
            return snap(exact.add(divisor).doubleValue(), sumNoise);
        }
        return remainder(r, x, y);
    }

    /**
     * Whether the rounded remainder {@code r} is neither zero nor rounds to the divisor {@code y}.
     */
    private static boolean isRemainder(double r, double y) {
        return r != 0 && Math.abs(r) < Math.abs(y);
    }

    /**
     * The rounded remainder {@code r} with the sign of {@code x}. A remainder that rounds to the divisor is zero:
     * the dividend is then within a rounding of a multiple of the divisor, which the whole quotient counts.
     */
    private static double remainder(double r, double x, double y) {
        var abs = Math.abs(r);
        return Math.copySign(abs < Math.abs(y) ? abs : 0.0, x);
    }

    /**
     * Whether both values are integers a double holds exactly: they are their own decimals, so the Java operators
     * round their exact result once.
     */
    private static boolean areIntegers(double x, double y) {
        return isInteger(x) && isInteger(y);
    }

    private static boolean isInteger(double x) {
        return Math.abs(x) < MAX_EXACT && Math.rint(x) == x;
    }

    /**
     * Whether a finite {@code x} with the decimal {@code d} is a computed number: it has a fraction, and its shortest
     * decimal has 16 or 17 digits, more than a double keeps of a number written as it is.
     */
    static boolean isComputed(double x, long d) {
        if (d != Decimal.NONE) {
            return Decimal.isLong(d) && Math.rint(x) != x;
        }
        // Out of the range of Decimal: the large doubles are whole
        return Math.abs(x) < 1 && x != 0 && BigDecimal.valueOf(x).stripTrailingZeros().precision() > 15;
    }

    /**
     * Whether both operands count as their doubles: each is computed or a whole number.
     */
    private static boolean areDoubles(double x, boolean cx, double y, boolean cy) {
        return (cx || isInteger(x)) && (cy || isInteger(y));
    }

    /**
     * The value {@code x} with the decimal {@code d} counts as minus {@code x}: zero for a computed number, which
     * counts as its double.
     */
    static double error(double x, long d, boolean computed) {
        return computed ? 0.0 : Decimal.error(x, d);
    }

    /**
     * {@code digits * 10^k} for {@code k >= 0}: exact when below {@code 2^53}.
     */
    private static double digitsAt(long digits, int k) {
        return k < POW10.length ? digits * POW10[k] : Double.POSITIVE_INFINITY;
    }

    /**
     * {@code r * 10^k mod m} for {@code 0 <= r < m}: multiplied by as many digits at once as a long holds.
     */
    private static long remainderTimesPow10(long r, int k, long m) {
        var step = 1;
        while (step + 1 < LONG_POW10.length && m <= MAX_TIMES_POW10[step + 1]) {
            step++;
        }
        var result = r;
        for (var left = k; left > 0; left -= step) {
            result = result * LONG_POW10[Math.min(left, step)] % m;
        }
        return result;
    }

    private static boolean isExact(double integer) {
        return Math.abs(integer) < MAX_EXACT;
    }

    private static boolean isExactPower(int e) {
        return -POW10.length < e && e < POW10.length;
    }

    private static boolean isInRange(double value) {
        var abs = Math.abs(value);
        return MIN_RESULT <= abs && abs <= MAX_RESULT;
    }

    /**
     * {@code c * 10^s} for an exact integer {@code c} and an exact power of ten, rounded once.
     */
    private static double scale(double c, int s) {
        return s >= 0 ? c * POW10[s] : c / POW10[-s];
    }

    /**
     * The double nearest to {@code digits * 10^exponent} of a decimal in the range of {@link Decimal}, or {@code NaN}
     * when it is too close to tell.
     */
    private static double nearest(long digits, int exponent) {
        if (Math.abs(digits) < MAX_EXACT && isExactPower(exponent)) {
            return scale(digits, exponent);
        }
        var c = (double) digits;
        // The digits beyond a double, exact: the digits are below 2^57
        var cLow = digits - (long) c;
        var high = PowersOfTen.high(exponent);
        var product = c * high;
        var error = ExactProduct.error(c, high, product) + (c * PowersOfTen.low(exponent) + (double) cLow * high);
        return isInRange(product) ? nearest(product, error, Math.abs(product) * 0x1p-100) : Double.NaN;
    }

    /**
     * The double nearest to a number known as {@code value + error} within {@code bound}, or {@code NaN} when a
     * number within the bound can round to another double.
     */
    static double nearest(double value, double error, double bound) {
        var high = value + error;
        var errorPart = high - value;
        var low = value - (high - errorPart) + (error - errorPart);
        // The rounding boundaries: half way to the neighbors of the double.
        var up = (Math.nextUp(high) - high) * 0.5;
        var down = (high - Math.nextDown(high)) * 0.5;
        return low + bound < up && low - bound > -down ? high : Double.NaN;
    }

    /**
     * The double nearest to {@code a / b} of positive integers, ties to even.
     */
    private static double nearest(BigInteger a, BigInteger b) {
        var shift = QUOTIENT_BITS - a.bitLength() + b.bitLength();
        var qr = shift > 0 ? a.shiftLeft(shift).divideAndRemainder(b) : a.divideAndRemainder(b.shiftLeft(-shift));
        // The last bit tells an inexact quotient: then it is odd and lies between the same doubles as the exact one.
        var q = qr[0].longValue() | qr[1].signum();
        if (shift <= MAX_NORMAL_SHIFT) {
            // A normal double: the conversion rounds once, and the scaling is exact or overflows as rounding does.
            return Math.scalb((double) q, -shift);
        }
        // Below the normal doubles a double holds fewer digits: round once to a multiple of 2^-1074, by the bits below.
        var dropped = shift - SUBNORMAL_SHIFT;
        if (dropped >= Long.SIZE) {
            return 0.0;
        }
        var kept = q >>> dropped;
        var rest = q & ((1L << dropped) - 1);
        var half = 1L << (dropped - 1);
        if (rest > half || rest == half && (kept & 1) != 0) {
            kept++;
        }
        return Math.scalb((double) kept, -SUBNORMAL_SHIFT);
    }

    /**
     * The exact value a finite {@code x} counts as: its double when it is computed, its shortest decimal otherwise.
     */
    static BigDecimal value(double x) {
        var d = Decimal.of(x);
        return value(x, d, isComputed(x, d));
    }

    private static BigDecimal value(double x, long d, boolean computed) {
        return computed ? Decimal.exactValue(x) : decimal(x, d);
    }

    /**
     * The shortest decimal of a finite {@code x}.
     */
    private static BigDecimal decimal(double x, long d) {
        return d != Decimal.NONE ? Decimal.toBigDecimal(x, d) : BigDecimal.valueOf(x);
    }

    /**
     * {@code x} moved to a shorter decimal as {@link #snap(double, double)} tells, when it is a computed number.
     */
    private static double snapComputed(double x) {
        return isComputed(x, Decimal.of(x)) ? snap(x, rounding(x, true)) : x;
    }

    /**
     * The rounding a computed operand {@code x} carries into a result, before the operation: twice its last place.
     */
    static double rounding(double x, boolean computed) {
        return computed ? CARRIED_ULPS * Math.ulp(x) : 0.0;
    }

    /**
     * A result {@code r} of a computed operand, carrying the rounding {@code noise} of it: the decimal of at most 13
     * significant digits that lies within half a unit of the 15th digit of {@code r} and within {@code noise} of it,
     * or {@code r} when there is none. The digits of {@code r} after the 13th then are that rounding, not a part of the
     * result.
     * <p>
     * The rounding to 15 digits is the one of the decimal {@code r} shows, ties to even. A result of at most 15 digits
     * stays as it is: such a decimal is either its own or not within half a unit of another.
     */
    static double snap(double r, double noise) {
        var abs = Math.abs(r);
        if (!isInRange(abs) || !(noise > 0)) {
            return r;
        }
        // |r| as m * 10^p for a whole m of 13 digits
        var e = Decimal.flog10pow2(Math.getExponent(abs));
        if (abs >= PowersOfTen.high(e + 1)) {
            e++;
        }
        var p = e + 1 - SNAP_DIGITS;
        var t = abs * PowersOfTen.high(-p);
        var m = Math.rint(t);
        if (Math.abs(t - m) > SNAP_FILTER) {
            return r;
        }
        // The distance of |r| from m * 10^p, exact enough to compare with half a unit of the 15th digit
        var high = PowersOfTen.high(p);
        var product = m * high;
        var distance = Math.abs(abs - product - (ExactProduct.error(m, high, product) + m * PowersOfTen.low(p)));
        var half = PowersOfTen.high(p - 2) * 0.5;
        // The decimal r shows lies within half a unit of its last place of r
        var ulp = Math.ulp(abs);
        if (distance > Math.min(half + ulp, noise)) {
            return r;
        }
        if (distance >= half - ulp) {
            return snapShown(r);
        }
        var snapped = nearest((long) m, p);
        return Math.copySign(Double.isNaN(snapped) ? BigDecimal.valueOf((long) m, -p).doubleValue() : snapped, r);
    }

    /**
     * {@code r} at a half unit of its 15th digit: the decimal it shows, rounded to 15 digits.
     */
    private static double snapShown(double r) {
        var shown = decimal(r, Decimal.of(r)).round(SNAP_ROUNDING).stripTrailingZeros();
        return shown.precision() <= SNAP_DIGITS ? shown.doubleValue() : r;
    }
}
