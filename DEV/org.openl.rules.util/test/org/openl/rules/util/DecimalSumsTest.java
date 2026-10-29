package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

class DecimalSumsTest {

    private static final double NAN = Double.NaN;
    private static final double INF = Double.POSITIVE_INFINITY;
    private static final MathContext EXACT = new MathContext(2000);

    @Test
    void computeWithDecimals() {
        // In binary: 0.30000000000000004, 0.15000000000000002 and 1.2100000000000002
        assertSame(0.3, Sum.sum(0.1, 0.2));
        assertSame(0.15, Avg.avg(0.1, 0.2));
        assertSame(1.21, Product.product(1.1, 1.1));
        // In binary: 0.9999999999999999 after ten additions
        assertSame(1.0, Sum.sum(0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1));
        assertSame(0.0, Sum.sum(0.1, 0.2, -0.3));
        assertSame(0.0, Avg.avg(0.1, 0.2, -0.3));
        assertSame(12.1, Product.product((Number) 1.1, 11, null));
        assertSame(3.0, Sum.sum((Number) 1, 2L, null));
    }

    @Test
    void roundOnceForAllValues() {
        // Each addition rounds 1e16 + 1 back to 1e16; the exact sum is 1e16 + 4
        assertSame(1.0000000000000004e16, Sum.sum(1e16, 1.0, 1.0, 1.0, 1.0));
        assertSame(1.0000000000000004e15, Avg.avg(1e16, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0));
        // A computed number counts as its double: 1 / 3 times 3 is 1
        assertSame(1.0, Product.product(0.3333333333333333, 3.0));
    }

    @Test
    void floatValuesComputeInBinary() {
        // One by one, as the Java operators of the widened values
        assertSame((double) 1.1f * 1.1f, Product.product(1.1f, 1.1f));
        assertSame(1.210000052452088, Product.product(1.1f, 1.1f));
        assertSame(979.1910247802734, Sum.sum(new Number[]{661.632f, 194.559f, 123f}));
        assertSame(((double) 539.41f + 185.92f + 305f) / 3, Avg.avg(new Number[]{539.41f, 185.92f, 305f}));
        // A Double among them computes with the decimals
        assertSame(0.3, Sum.sum(new Number[]{0.5f, -0.2}));
    }

    @Test
    void wholeNumbersAndBigDecimalsCountWithAllTheirDigits() {
        // 2^60 and -(2^60 + 256) are exact doubles, which show 1152921504606847000 and -1152921504606847200
        assertSame(-128.0, Avg.avg(new Long[]{1L << 60, -((1L << 60) + 256)}));
        assertSame(-256.0, Sum.sum(new Number[]{new BigDecimal(BigInteger.ONE.shiftLeft(60)), -((1L << 60) + 256)}));
        assertSame(0.1, Sum.sum(new Number[]{new BigDecimal("0.30000000000000000001"), -0.2}));
        assertSame(1.0E-20, Sum.sum(new Number[]{new BigDecimal("0.30000000000000000001"), -0.3}));
        assertSame(6.0, Product.product(new Number[]{2, 3L, BigInteger.ONE, BigDecimal.ONE}));
    }

    @Test
    void longProductsLeaveTheRangeOfDoublesOnTheWay() {
        assertSame(0.0, Product.product(filled(100_000, 0.9)));
        assertSame(-0.0, Product.product(filled(100_001, -0.12345678901234567)));
        assertSame(Double.POSITIVE_INFINITY, Product.product(filled(100_000, 1.05)));
        // Far below the doubles on the way, where they give 0.0, and back
        var values = filled(1_000, 0.1);
        values = Arrays.copyOf(values, 2_000);
        Arrays.fill(values, 1_000, 2_000, 10.0);
        assertSame(1.0, Product.product(values));
        // Values out of the range the decimals are found in
        assertSame(1e-310, Product.product(1e-300, 1e-10));
        assertSame(1.2e-305, Product.product(1e-300, 1e-10, 1.2e5));
        assertSame(0.0, Product.product(filled(100_000, 0.12345678901234567, 1e-300)));
        // A product below the normal doubles is rounded once, to a multiple of the smallest double
        assertSame(Double.MIN_VALUE * 3, Product.product(Double.MIN_VALUE * 3, 1.0000000000000002));
        assertSame(Double.MIN_VALUE * 4, Product.product(Double.MIN_VALUE * 7, 0.5));
        assertSame(3.0E-310, Product.product(1e-310, 3.0));
        assertSame(2.225073858507201E-308, Product.product(2.225073858507201E-308, 0.9999999999999999));
        // Computed numbers, which count as their doubles
        assertSame(2.225073858507201E-308,
                Product.product(2.225073858507201E-308, 0.9999999999999999, 1.0000000000000002));
    }

    @Test
    void computedNumbersCountAsTheirDoubles() {
        assertSame(1.0, Sum.sum(1.0 / 3, 1.0 / 3, 1.0 / 3));
        assertSame(1.0 / 3, Avg.avg(1.0 / 3, 1.0 / 3, 1.0 / 3));
        // Within the rounding the computed values carry, the result is the shorter decimal
        assertSame(1.8, Product.product(9.0 / 35, 7.0));
        assertSame(100.0, Product.product(100.0 / 12, 12.0));
        assertSame(0.3, Sum.sum(0.30000000000000004, 0.0));
        assertSame(0.15, Avg.avg(0.30000000000000004, 0.0));
        // Digits beyond that rounding stay
        assertSame(6910158952078997.0, Sum.sum(6910158952078997.0, 1.0 / 30));
        assertSame(1.000000000000002E13, Sum.sum(10000000000000.01, 0.01));
        // Out of the range of the shorter decimals
        assertSame(9.999999999999999E-302,
                Sum.sum(new Number[]{3.333333333333333E-302, 3.333333333333333E-302, 3.3333333333333334E-302}));
        assertSame(-1.6666666666666665E-301, Product.product(-3.333333333333333E-301, 0.5));
    }

    @Test
    void averageBelowTheNormalDoubles() {
        assertSame(2.225073858507201E-308,
                Avg.avg(2.225073858507201E-308, 2.2250738585072004E-308, 2.225073858507202E-308));
    }

    private static Double[] filled(int count, double value, double... more) {
        var values = new Double[count + more.length];
        Arrays.fill(values, value);
        for (var i = 0; i < more.length; i++) {
            values[count + i] = more[i];
        }
        return values;
    }

    @Test
    void emptyValues() {
        assertNull(Sum.sum((Double[]) null));
        assertNull(Sum.sum(new Double[]{null}));
        assertNull(Avg.avg(new Double[]{}));
        assertNull(Avg.avg((Double[]) null));
        assertNull(Product.product((Double[]) null));
        assertNull(Product.product(new Double[]{null, null}));
    }

    @Test
    void specialValuesFollowIeee754() {
        assertSame(NAN, Sum.sum(1.0, NAN));
        assertSame(NAN, Sum.sum(INF, -INF));
        assertSame(INF, Sum.sum(INF, 1.0));
        assertSame(INF, Avg.avg(INF, 1.0));
        assertSame(NAN, Product.product(0.0, INF));
        assertSame(-0.0, Product.product(-1.5, 0.0));
        // The doubles overflow to Infinity before the zero, which gives NaN in binary
        assertSame(0.0, Product.product(1e300, 1e300, 0.0));
        assertSame(-0.0, Product.product(-1e300, 1e300, -0.0, -1.0));
        assertSame(-0.0, Sum.sum(-0.0, -0.0));
        assertSame(0.0, Sum.sum(-0.0, 0.0));
        assertSame(-0.0, Avg.avg(-0.0, -0.0));
        assertSame(INF, Sum.sum(Double.MAX_VALUE, Double.MAX_VALUE));
        assertSame(INF, Product.product(1e200, 1e200));
        assertSame(1e-310, Product.product(1e-300, 1e-10));
        assertSame(2e-300, Sum.sum(1e-300, 1e-300));
    }

    @Test
    void resultIsNearestToExactResult() {
        var random = new Random(20261029);
        for (var i = 0; i < 20_000; i++) {
            var values = new Double[random.nextInt(1, 60)];
            for (var j = 0; j < values.length; j++) {
                values[j] = random.nextInt(20) == 0 ? null : randomDouble(random);
            }
            var sum = BigDecimal.ZERO;
            var product = BigDecimal.ONE;
            var count = 0;
            var rounding = 0.0;
            var relativeRounding = 0.0;
            for (var x : values) {
                if (x != null) {
                    var value = DecimalModel.value(x);
                    sum = sum.add(value);
                    product = product.multiply(value);
                    count++;
                    rounding += DecimalModel.rounding(x);
                    relativeRounding += DecimalModel.rounding(x) / Math.abs(x);
                }
            }
            if (count > 0 && sum.signum() != 0) {
                assertSame(DecimalModel.snap(sum.doubleValue(), rounding), Sum.sum(values));
                var average = sum.divide(BigDecimal.valueOf(count), EXACT).doubleValue();
                assertSame(DecimalModel.snap(average, rounding / count), Avg.avg(values));
            }
            if (count > 0 && product.signum() != 0) {
                var p = product.doubleValue();
                assertSame(DecimalModel.snap(p, Math.abs(p) * relativeRounding), Product.product(values));
            }
        }
    }

    private static double randomDouble(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> BigDecimal.valueOf(random.nextLong(-10_000_000_000L, 10_000_000_000L), 2).doubleValue();
            case 1 -> (random.nextInt(2000) - 1000) / 7.0;
            case 2 -> BigDecimal.valueOf(random.nextLong(), random.nextInt(25)).doubleValue();
            default -> Math.scalb(random.nextDouble() + 0.5, random.nextInt(-950, 950));
        };
    }

    private static void assertSame(double expected, Double actual) {
        assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(actual),
                () -> "expected: " + expected + " but was: " + actual);
    }
}
