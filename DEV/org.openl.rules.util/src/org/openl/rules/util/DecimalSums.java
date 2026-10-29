package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jspecify.annotations.Nullable;

/**
 * Sums, averages and products of doubles as of the values they count as in {@link DecimalArithmetic}:
 * {@code sum(0.1, 0.2)} is {@code 0.3}. A double counts as the decimal it shows, or as itself when it is a computed
 * number of 16 or 17 digits. A whole number counts with all its digits and a {@link BigDecimal} as it is, also when
 * its double rounds them.
 * <p>
 * The result is the double nearest to the exact result of all the values, rounded once, not once per value. Each
 * value is added or multiplied in with the rounding error of the step carried along, as Kahan summation does, and
 * with the difference of its value from the double. When that is not enough to tell the nearest double, the values
 * are computed exactly. A result of computed values moves to a shorter decimal within the rounding they carry, as
 * {@link DecimalArithmetic#snap(double, double)} tells: {@code sum(1 / 3, 1 / 3, 1 / 3)} is {@code 1.0}.
 * <p>
 * Empty values are skipped, and no value gives {@code null}. {@code NaN}, infinities and the sign of a zero result
 * follow IEEE 754, as the Java operators do. A product of finite values with a zero is a zero, also when the other
 * values multiplied one by one overflow, which gives {@code NaN} in binary.
 * <p>
 * {@link Float} values compute in binary, as the Java operators do: one by one, each step rounded.
 */
final class DecimalSums {

    // The smallest double is 2^-1074
    private static final int SUBNORMAL_EXPONENT = Double.PRECISION - 1 - Double.MIN_EXPONENT;
    // Up to it, a double holds every whole number
    private static final double MAX_EXACT = 0x1p53;
    // The range of Decimal
    private static final double MIN_DECIMAL = 0x1p-900;
    private static final double MAX_DECIMAL = 0x1p901;

    private DecimalSums() {
        // Utility class
    }

    /**
     * The exact value a number counts as: a whole number with all its digits, a {@link BigDecimal} as it is, and any
     * other number as {@link DecimalArithmetic} counts its double.
     */
    private static BigDecimal valueOf(Number value) {
        return switch (value) {
            case BigDecimal decimal -> decimal;
            case BigInteger integer -> new BigDecimal(integer);
            case Long l -> BigDecimal.valueOf(l);
            case Integer i -> BigDecimal.valueOf(i);
            case Short s -> BigDecimal.valueOf(s);
            case Byte b -> BigDecimal.valueOf(b);
            default -> DecimalArithmetic.value(value.doubleValue());
        };
    }

    /**
     * Whether a value counts as {@link DecimalArithmetic} counts a double: it is neither a whole number nor a
     * {@link BigDecimal}.
     */
    private static boolean isDouble(Number value) {
        return !(isWhole(value) || value instanceof BigDecimal);
    }

    private static boolean isWhole(Number value) {
        return value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte
                || value instanceof BigInteger;
    }

    /**
     * The value a number counts as minus {@code x}, its non-zero finite double, or {@code NaN} when {@code x} is out of
     * the range of {@link Decimal}. {@code d} is the decimal of a double and {@code computed} tells a computed one,
     * which counts as itself.
     */
    private static double error(Number value, double x, long d, boolean computed) {
        var abs = Math.abs(x);
        if (isDouble(value)) {
            return d == Decimal.NONE ? Double.NaN : DecimalArithmetic.error(x, d, computed);
        }
        if (isWhole(value) && abs < MAX_EXACT) {
            return 0.0;
        }
        var inRange = abs >= MIN_DECIMAL && abs < MAX_DECIMAL;
        return inRange ? valueOf(value).subtract(Decimal.exactValue(x)).doubleValue() : Double.NaN;
    }

    static @Nullable Double sum(@Nullable Number @Nullable [] values) {
        var sum = Sum.of(values);
        return sum == null ? null : sum.total();
    }

    static @Nullable Double avg(@Nullable Number @Nullable [] values) {
        var sum = Sum.of(values);
        return sum == null ? null : sum.average();
    }

    static @Nullable Double product(@Nullable Number @Nullable [] values) {
        var product = Product.of(values);
        return product == null ? null : product.total();
    }

    /**
     * The sum of the decimals of the values, as {@code high + low} within the bound of the summation.
     */
    private static final class Sum {

        private final @Nullable Number[] values;
        private int count;
        // The sum of the doubles: its special values, the sign of a zero, and the sum of Float values
        private double ieee = -0.0;
        private boolean finite = true;
        private boolean floats = true;
        private double high;
        private double low;
        private double magnitude;
        private boolean inRange = true;
        // The rounding the computed values carry
        private double noise;

        private Sum(@Nullable Number[] values) {
            this.values = values;
        }

        static @Nullable Sum of(@Nullable Number @Nullable [] values) {
            if (values == null) {
                return null;
            }
            var sum = new Sum(values);
            for (var value : values) {
                if (value != null) {
                    sum.add(value);
                }
            }
            return sum.count == 0 ? null : sum;
        }

        private void add(Number value) {
            var x = value.doubleValue();
            count++;
            ieee += x;
            finite &= Double.isFinite(x);
            floats &= value instanceof Float;
            if (x == 0 || !finite) {
                return;
            }
            var d = isDouble(value) ? Decimal.of(x) : Decimal.NONE;
            var computed = isDouble(value) && DecimalArithmetic.isComputed(x, d);
            noise += DecimalArithmetic.rounding(x, computed);
            if (!inRange) {
                return;
            }
            var error = error(value, x, d, computed);
            inRange = !Double.isNaN(error);
            if (inRange) {
                // Kahan-Babuska: the rounding error of the step goes to low, whichever term is larger
                var t = high + x;
                low += Math.abs(high) >= Math.abs(x) ? high - t + x : x - t + high;
                high = t;
                low += error;
                magnitude += Math.abs(x);
            }
        }

        // The error of high + low: of the summation of twice as many terms, and of the decimals
        private double bound() {
            return ((double) count * count * 0x1p-101 + 0x1p-98) * magnitude;
        }

        private boolean isFast() {
            return inRange && Double.isFinite(high);
        }

        Double total() {
            if (!finite || floats) {
                return ieee;
            }
            return DecimalArithmetic.snap(rounded(), noise);
        }

        private double rounded() {
            if (isFast()) {
                // Never zero: a zero sum is too close to tell
                var r = DecimalArithmetic.nearest(high, low, bound());
                if (!Double.isNaN(r)) {
                    return r;
                }
            }
            var exact = exact();
            return exact.signum() == 0 ? zero() : exact.doubleValue();
        }

        Double average() {
            if (!finite || floats) {
                return ieee / count;
            }
            return DecimalArithmetic.snap(roundedAverage(), noise / count);
        }

        private double roundedAverage() {
            var n = (double) count;
            var q = high / n;
            if (isFast() && Math.abs(q) >= 0x1p-900) {
                var t = (ExactProduct.remainder(high, n, q) + low) / n;
                var r = DecimalArithmetic.nearest(q, t, bound() / n + Math.abs(q) * 0x1p-100);
                if (!Double.isNaN(r)) {
                    return r;
                }
            }
            var exact = exact();
            return exact.signum() == 0 ? zero() : DecimalArithmetic.quotient(exact, BigDecimal.valueOf(count));
        }

        // A zero sum is negative only when all the values are
        private double zero() {
            return ieee == 0 ? ieee : 0.0;
        }

        private BigDecimal exact() {
            var sum = BigDecimal.ZERO;
            for (var value : values) {
                if (value != null) {
                    sum = sum.add(valueOf(value));
                }
            }
            return sum;
        }
    }

    /**
     * The product of the decimals of the values, as {@code (high + low) * 2^exponent} within the bound of the
     * multiplication. The power of two keeps {@code high} from 1 to 2, so that no number of values overflows or
     * underflows before the end.
     */
    private static final class Product {

        private final @Nullable Number[] values;
        private int count;
        // The product of the doubles: the result when a value is not finite, and the product of Float values
        private double ieee = 1.0;
        private boolean finite = true;
        private boolean floats = true;
        private boolean negative;
        private boolean zero;
        private double high = 1.0;
        private double low;
        private long exponent;
        // The rounding the computed values carry, relative to the product
        private double noise;

        private Product(@Nullable Number[] values) {
            this.values = values;
        }

        static @Nullable Product of(@Nullable Number @Nullable [] values) {
            if (values == null) {
                return null;
            }
            var product = new Product(values);
            for (var value : values) {
                if (value != null) {
                    product.multiply(value);
                }
            }
            return product.count == 0 ? null : product;
        }

        private void multiply(Number value) {
            var x = value.doubleValue();
            count++;
            ieee *= x;
            finite &= Double.isFinite(x);
            floats &= value instanceof Float;
            negative ^= Math.copySign(1.0, x) < 0;
            zero |= x == 0;
            if (x == 0 || !finite) {
                return;
            }
            var d = isDouble(value) ? Decimal.of(x) : Decimal.NONE;
            var computed = isDouble(value) && DecimalArithmetic.isComputed(x, d);
            noise += DecimalArithmetic.rounding(x, computed) / Math.abs(x);
            // The value as (xHigh + xLow) * 2^xExponent
            double xHigh;
            double xLow;
            var xExponent = 0;
            var error = error(value, x, d, computed);
            if (!Double.isNaN(error)) {
                xHigh = x;
                xLow = error;
            } else {
                // Out of the range of Decimal: the value is found with BigDecimal, its power of two moved out
                xExponent = binaryExponent(x);
                var decimal = valueOf(value);
                if (xExponent >= 0) {
                    // A large double: its decimal lies within half a unit of its last digit
                    xHigh = Math.scalb(x, -xExponent);
                    xLow = Math.scalb(decimal.subtract(Decimal.exactValue(x)).doubleValue(), -xExponent);
                } else {
                    // A small double, maybe with fewer digits than its decimal
                    var scaled = decimal.multiply(new BigDecimal(BigInteger.ONE.shiftLeft(-xExponent)));
                    xHigh = scaled.doubleValue();
                    xLow = scaled.subtract(Decimal.exactValue(xHigh)).doubleValue();
                }
            }
            // The rounding error of the step and the errors of the decimals go to low
            var p = high * xHigh;
            low = ExactProduct.error(high, xHigh, p) + (low * xHigh + high * xLow);
            var e = Math.getExponent(p);
            high = Math.scalb(p, -e);
            low = Math.scalb(low, -e);
            exponent += e + xExponent;
        }

        // The power of two of x, subnormal or not
        private static int binaryExponent(double x) {
            var e = Math.getExponent(x);
            if (e >= Double.MIN_EXPONENT) {
                return e;
            }
            var bits = Double.doubleToRawLongBits(x) & ((1L << (Double.PRECISION - 1)) - 1);
            return -SUBNORMAL_EXPONENT + Long.SIZE - 1 - Long.numberOfLeadingZeros(bits);
        }

        Double total() {
            if (!finite || floats) {
                return ieee;
            }
            var r = rounded();
            return DecimalArithmetic.snap(r, Math.abs(r) * noise);
        }

        private double rounded() {
            if (zero) {
                // Exactly zero, though the product of the doubles may overflow before it reaches the zero
                return signed(0.0);
            }
            if (exponent > Double.MAX_EXPONENT + 1) {
                // At least 2^1025, within the bound
                return signed(Double.POSITIVE_INFINITY);
            }
            if (exponent < Double.MIN_EXPONENT - Double.PRECISION - 1) {
                // Below half the smallest double, within the bound
                return signed(0.0);
            }
            var bound = ((double) count * count * 0x1p-100 + count * 0x1p-98) * Math.abs(high);
            if (exponent <= Double.MIN_EXPONENT) {
                // Maybe below the normal doubles: round to a multiple of the smallest double, in its units
                var shift = (int) exponent + SUBNORMAL_EXPONENT;
                var units = Math.scalb(high, shift);
                var lowUnits = Math.scalb(low, shift);
                var whole = Math.rint(units + lowUnits);
                var rest = units - whole + lowUnits;
                if (Math.abs(rest) + Math.abs(rest) * 0x1p-50 + Math.scalb(bound, shift) < 0.5) {
                    return whole == 0 ? signed(0.0) : Math.scalb(whole, -SUBNORMAL_EXPONENT);
                }
                return exact();
            }
            var r = DecimalArithmetic.nearest(high, low, bound);
            // Moved by the exponent, a normal double stays rounded once, or overflows as rounding does
            return Double.isNaN(r) ? exact() : Math.scalb(r, (int) exponent);
        }

        private double signed(double magnitude) {
            return negative ? -magnitude : magnitude;
        }

        private double exact() {
            var product = BigDecimal.ONE;
            for (var value : values) {
                if (value != null) {
                    product = product.multiply(valueOf(value));
                }
            }
            return product.doubleValue();
        }
    }
}
