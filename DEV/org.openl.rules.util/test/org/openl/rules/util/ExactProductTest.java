package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

class ExactProductTest {

    // Significands with the most bits set, the fewest, and those around the halves of the split
    private static final double[] SIGNIFICANDS = {1.0, Math.nextUp(1.0), Math.nextDown(2.0), 1.5, 1 + 0x1p-26,
            1 + 0x1p-27, 1 + 0x1p-26 + 0x1p-52, 2 - 0x1p-26, 2 - 0x1p-27 - 0x1p-52};

    @Test
    void errorIsTheExactRestOfTheProduct() {
        var random = new Random(20261029);
        for (var i = 0; i < 200_000; i++) {
            var ea = random.nextInt(-960, 996);
            var a = randomDouble(random, ea);
            var b = randomDouble(random, random.nextInt(Math.max(-960 - ea, -1022), Math.min(996, 1022 - ea)));
            assertError(a, b);
        }
        for (var a : SIGNIFICANDS) {
            for (var b : SIGNIFICANDS) {
                assertError(a, b);
                assertError(-Math.scalb(a, 995), Math.scalb(b, -960));
                assertError(Math.scalb(a, -480), -Math.scalb(b, -480));
            }
        }
    }

    @Test
    void remainderIsTheExactRestOfTheQuotient() {
        var random = new Random(20261029);
        for (var i = 0; i < 200_000; i++) {
            var ex = random.nextInt(-950, 996);
            var x = randomDouble(random, ex);
            var y = randomDouble(random, random.nextInt(Math.max(ex - 994, -1022), Math.min(ex + 950, 996)));
            assertRemainder(x, y);
        }
        for (var x : SIGNIFICANDS) {
            for (var y : SIGNIFICANDS) {
                assertRemainder(x, y);
                assertRemainder(-Math.scalb(x, 995), Math.scalb(y, 990));
                assertRemainder(Math.scalb(x, -950), -Math.scalb(y, 5));
            }
        }
        // Exact quotients leave no rest
        assertSameNumber(0.0, ExactProduct.remainder(1.5, 0.5, 3.0));
        assertSameNumber(0.0, ExactProduct.remainder(-0x1p990, 0x1p-5, -0x1p995));
    }

    private static void assertError(double a, double b) {
        var product = a * b;
        var expected = Math.fma(a, b, -product);
        assertSameNumber(expected, ExactProduct.error(a, b, product));
        var exact = Decimal.exactValue(a).multiply(Decimal.exactValue(b)).subtract(Decimal.exactValue(product));
        assertEquals(0, exact.compareTo(Decimal.exactValue(expected)), () -> a + " * " + b + " is inexact");
    }

    private static void assertRemainder(double x, double y) {
        var quotient = x / y;
        var expected = Math.fma(-quotient, y, x);
        assertSameNumber(expected, ExactProduct.remainder(x, y, quotient));
        var exact = Decimal.exactValue(x).subtract(Decimal.exactValue(quotient).multiply(Decimal.exactValue(y)));
        assertEquals(0, exact.compareTo(Decimal.exactValue(expected)), () -> x + " / " + y + " is inexact");
    }

    /**
     * A double of random sign and significand in {@code [2^e, 2^(e+1))}.
     */
    private static double randomDouble(Random random, int e) {
        var significand = 1 + random.nextLong(1L << 52) * 0x1p-52;
        return Math.scalb(random.nextBoolean() ? significand : -significand, e);
    }

    // The sign of a zero rest does not count
    private static void assertSameNumber(double expected, double actual) {
        assertEquals(expected, actual, 0.0);
    }
}
