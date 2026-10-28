package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DecimalArithmeticTest {

    private static final double NAN = Double.NaN;
    private static final double INF = Double.POSITIVE_INFINITY;
    private static final double MAX = Double.MAX_VALUE;
    // Exact enough to round a quotient of any two doubles compared below.
    private static final MathContext EXACT = new MathContext(1200, RoundingMode.HALF_EVEN);

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({
            "0.1, 0.2, 0.3",
            "0.7, 0.1, 0.8",
            "1.1, 2.2, 3.3",
            "1234567.89, 0.01, 1234567.9",
            "-0.1, 0.1, 0.0",
            // 17 digits: a computed number, which counts as its double, and the result moves to the shorter decimal
            "0.30000000000000004, 0.1, 0.4",
            "1e23, 1, 1.0000000000000001e23",
            "1e-300, 1e-300, 2e-300",
            "0.1, 0.3333333333333333, 0.4333333333333333",
            "0.3333333333333333, 0.6666666666666666, 1.0",
            "9007199254740992, 1, 9007199254740992",
            "9007199254740992, 3, 9007199254740996"
    })
    void add(double x, double y, double expected) {
        assertSame(expected, DecimalArithmetic.add(x, y));
        assertSame(expected, DecimalArithmetic.add(y, x));
    }

    @ParameterizedTest(name = "{0} - {1} = {2}")
    @CsvSource({"0.3, 0.1, 0.2", "1.0, 0.9, 0.1", "0.1, 0.3, -0.2", "100.0, 99.99, 0.01", "0.1, 0.1, 0.0"})
    void subtract(double x, double y, double expected) {
        assertSame(expected, DecimalArithmetic.subtract(x, y));
    }

    @ParameterizedTest(name = "{0} * {1} = {2}")
    @CsvSource({
            "1.1, 1.1, 1.21",
            "1.15, 100, 115",
            "4.35, 100, 435",
            "0.1, 3, 0.3",
            "19.99, 0.07, 1.3993",
            "-0.5, 0.2, -0.1",
            "1e-15, 1e-15, 1e-30",
            "1e200, 1e200, Infinity",
            "0.3333333333333333, 3, 1.0",
            "142.85714285714286, 0.07, 10.0",
            "1e-300, 1e-10, 1e-310"
    })
    void multiply(double x, double y, double expected) {
        assertSame(expected, DecimalArithmetic.multiply(x, y));
        assertSame(expected, DecimalArithmetic.multiply(y, x));
    }

    @ParameterizedTest(name = "{0} / {1} = {2}")
    @CsvSource({
            "0.3, 0.1, 3",
            "0.69, 0.1, 6.9",
            "1, 3, 0.3333333333333333",
            "2, 3, 0.6666666666666666",
            "1.21, 1.1, 1.1",
            "-4.35, 0.01, -435",
            "0.30000000000000004, 0.1, 3.0",
            "1e-320, 3, 3.335e-321",
            "1e300, 1e-10, Infinity",
            "1, 0.3333333333333333, 3.0",
            "0.9999999999999999, 3, 0.3333333333333333",
            "1e-300, 3, 3.3333333333333334e-301",
            // The quotient lies just below the smallest normal double, by less than half the spacing there
            "2.8925960160593614E-307, 13, 2.225073858507201E-308",
            "-1.9091133705991785E-305, 858, -2.225073858507201E-308",
            "4.9E-324, 1.7976931348623157E308, 0.0"
    })
    void divide(double x, double y, double expected) {
        assertSame(expected, DecimalArithmetic.divide(x, y));
    }

    @ParameterizedTest(name = "{0} % {1} = {2}")
    @CsvSource({
            "1.1, 0.1, 0.0",
            "-1.1, 0.1, -0.0",
            "0.7, 0.2, 0.1",
            "-0.7, 0.2, -0.1",
            "0.7, -0.2, 0.1",
            "3.22, 1.75, 1.47",
            "1e23, 0.3, 0.1",
            "0.30000000000000004, 0.1, 4.4408920985006264E-17",
            "0.3, 0.7, 0.3",
            "1e-30, 3e-31, 1e-31",
            "142.85714285714286, 0.3333333333333333, 0.19047619047620246",
            "1e300, 0.3, 0.1",
            "0.9000000000000001, 0.30000000000000004, 0.0",
            "-0.9000000000000001, 0.30000000000000004, -0.0"
    })
    void remainder(double x, double y, double expected) {
        assertSame(expected, DecimalArithmetic.remainder(x, y));
    }

    @Test
    void equalDecimalsCancel() {
        // 1 / 7.0 shows 17 digits, too many for the whole-digits path
        assertSame(0.0, DecimalArithmetic.subtract(1 / 7.0, 1 / 7.0));
        assertSame(0.0, DecimalArithmetic.add(-1 / 7.0, 1 / 7.0));
        assertSame(0.0, DecimalArithmetic.subtract(-1e300, -1e300));
    }

    @Test
    void floorModRoundsOnce() {
        // The remainder plus the divisor of the values, rounded once: 2 / 3, 1 / 7 and 10 / 3 count as their doubles
        assertSame(0.11333333333333326, DecimalArithmetic.floorMod(-1.22, 2.0 / 3));
        assertSame(0.05714285714285709, DecimalArithmetic.floorMod(-0.8, 1.0 / 7));
        assertSame(-4.121147867408581E-13, DecimalArithmetic.floorMod(18560.0, -1.4285714285714286));
        assertSame(4.440892098500626E-14, DecimalArithmetic.floorMod(-1000.0, 10.0 / 3));
        assertSame(0.4, DecimalArithmetic.floorMod(-0.6, 1.0));
        assertSame(-0.0, DecimalArithmetic.floorMod(-1.1, 0.1));
        assertSame(-0.3, DecimalArithmetic.floorMod(0.7, -1.0));
        assertSame(2.0, DecimalArithmetic.floorMod(-7.0, 3.0));
    }

    @Test
    void floorModIsNearestToExactResult() {
        var random = new Random(20261030);
        for (var i = 0; i < 100_000; i++) {
            var x = randomDouble(random);
            var y = randomDouble(random);
            assertSame(remainder(x, y, true), DecimalArithmetic.floorMod(x, y));
        }
    }

    @Test
    void computedNumbersCountAsTheirDoubles() {
        // 1 / 3 shows 0.3333333333333333, 16 digits: it counts as its double, nearer to one third
        assertSame(1.0, DecimalArithmetic.multiply(1.0 / 3, 3));
        assertSame(1.0, DecimalArithmetic.add(1.0 / 3, 2.0 / 3));
        // Within the rounding the computed operand carries, the result is the shorter decimal
        assertSame(1.8, DecimalArithmetic.multiply(9.0 / 35, 7));
        assertSame(100.0, DecimalArithmetic.multiply(100.0 / 12, 12));
        assertSame(0.3, DecimalArithmetic.multiply(0.30000000000000004, 1));
        assertSame(-1.0, DecimalArithmetic.remainder(-1.0000000000000002, 3));
        assertSame(2.0, DecimalArithmetic.floorMod(-1.0000000000000002, 3));
        // Digits beyond that rounding stay
        assertSame(6910158952078997.0, DecimalArithmetic.add(6910158952078997.0, 1.0 / 30));
        assertSame(1.000000000000002E13, DecimalArithmetic.add(10000000000000.01, 0.01));
        // A whole number counts as the decimal it shows, also with 16 digits
        assertSame(9007199254740996.0, DecimalArithmetic.add(9007199254740992.0, 3));
        // Below the range of the shorter decimals, the double of the result
        assertSame(9.999999999999999E-301, DecimalArithmetic.multiply(3.333333333333333E-301, 3));
        assertSame(1.0, DecimalArithmetic.divide(3.333333333333333E-301, 3.333333333333333E-301));
    }

    @Test
    void snapMovesAResultWithinTheRoundingAndHalfAUnitOfTheFifteenthDigit() {
        // A half unit of the 15th digit rounds the decimal shown to even
        assertSame(1.23456789012, DecimalArithmetic.snap(1.234567890120005, 1e-10));
        assertSame(1.234567890121, DecimalArithmetic.snap(1.234567890120995, 1e-10));
        assertSame(1.234567890120015, DecimalArithmetic.snap(1.234567890120015, 1e-10));
        assertSame(-1.8, DecimalArithmetic.snap(-1.7999999999999998, 1e-15));
        assertSame(1e200, DecimalArithmetic.snap(1.0000000000000002e200, 1e190));
        // No rounding carried, a rounding smaller than the distance, or out of the range of the shorter decimals
        assertSame(1.0000000000000002, DecimalArithmetic.snap(1.0000000000000002, 0));
        assertSame(1.0000000000000002, DecimalArithmetic.snap(1.0000000000000002, 1e-16));
        assertSame(1.0000000000000002E-300, DecimalArithmetic.snap(1.0000000000000002E-300, 1));
        assertSame(1.0000000000000002E300, DecimalArithmetic.snap(1.0000000000000002E300, 1e290));
    }

    @Test
    void specialValuesFollowIeee754() {
        double[] values = {NAN, INF, -INF, 0.0, -0.0, 1.5, -1.5, MAX, -MAX, Double.MIN_VALUE, 1e23};
        for (var x : values) {
            for (var y : values) {
                if (!Double.isFinite(x) || !Double.isFinite(y) || x == 0 || y == 0) {
                    assertSame(x + y, DecimalArithmetic.add(x, y));
                    assertSame(x - y, DecimalArithmetic.subtract(x, y));
                    assertSame(x * y, DecimalArithmetic.multiply(x, y));
                    assertSame(x / y, DecimalArithmetic.divide(x, y));
                    assertSame(x % y, DecimalArithmetic.remainder(x, y));
                }
            }
        }
        assertSame(INF, DecimalArithmetic.add(MAX, MAX));
        assertSame(-INF, DecimalArithmetic.subtract(-MAX, MAX));
        assertSame(-0.0, DecimalArithmetic.multiply(-1e23, 0.0));
    }

    @Test
    void resultIsNearestToExactResult() {
        var random = new Random(20261028);
        for (var i = 0; i < 100_000; i++) {
            var x = randomDouble(random);
            var y = randomDouble(random);
            var vx = DecimalModel.value(x);
            var vy = DecimalModel.value(y);
            var rx = DecimalModel.rounding(x);
            var ry = DecimalModel.rounding(y);
            assertSame(DecimalModel.snap(nearest(vx.add(vy), x + y), rx + ry), DecimalArithmetic.add(x, y));
            assertSame(DecimalModel.snap(nearest(vx.subtract(vy), x - y), rx + ry), DecimalArithmetic.subtract(x, y));
            var product = nearest(vx.multiply(vy), x * y);
            assertSame(DecimalModel.snap(product, rx * Math.abs(y) + ry * Math.abs(x)), DecimalArithmetic.multiply(x, y));
            var quotient = nearest(vx.divide(vy, EXACT), x / y);
            var quotientRounding = Math.abs(quotient) * (rx / Math.abs(x) + ry / Math.abs(y));
            assertSame(DecimalModel.snap(quotient, quotientRounding), DecimalArithmetic.divide(x, y));
            assertSame(remainder(x, y, false), DecimalArithmetic.remainder(x, y));
        }
    }

    /**
     * The remainder of the values {@code x} and {@code y} count as, moved within the rounding they carry. A remainder
     * that rounds to the divisor is zero. With {@code floor}, the floor modulo.
     */
    private static double remainder(double x, double y, boolean floor) {
        var divisor = DecimalModel.value(y);
        var exact = DecimalModel.value(x).remainder(divisor);
        var rounding = DecimalModel.rounding(x) + DecimalModel.rounding(y) * Math.floor(Math.abs(x / y));
        var r = DecimalModel.snap(exact.doubleValue(), rounding);
        if (floor && (x < 0) != (y < 0) && r != 0 && Math.abs(r) < Math.abs(y)) {
            return DecimalModel.snap(exact.add(divisor).doubleValue(), rounding + DecimalModel.rounding(y));
        }
        return Math.copySign(Math.abs(r) < Math.abs(y) ? Math.abs(r) : 0.0, x);
    }

    private static double randomDouble(Random random) {
        if (random.nextInt(4) == 0) {
            // Any double from 2^-950 to 2^950: also the ones with no decimal in Decimal
            return Math.scalb(random.nextDouble() + 0.5, random.nextInt(-950, 950)) * (random.nextBoolean() ? 1 : -1);
        }
        var digits = switch (random.nextInt(3)) {
            case 0 -> random.nextLong(10_000_000_000L); // a money amount
            case 1 -> random.nextLong(1_000_000_000_000_000_000L); // up to 18 digits
            default -> random.nextLong(1_000_000); // a rate or a factor
        };
        var value = BigDecimal.valueOf(random.nextBoolean() ? digits : -digits, random.nextInt(-5, 25));
        return digits == 0 ? 1 : value.doubleValue();
    }

    private static double nearest(BigDecimal exact, double ieee) {
        return exact.signum() == 0 ? ieee : exact.doubleValue();
    }

    private static void assertSame(double expected, double actual) {
        assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(actual),
                () -> "expected: " + expected + " but was: " + actual);
    }
}
