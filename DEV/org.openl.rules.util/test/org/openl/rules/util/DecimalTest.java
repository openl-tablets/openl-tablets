package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DecimalTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.1, -0.1, 0.3, 1, 100, 1234.56, 0.30000000000000004, 0.3333333333333333, 1e23,
            9.999999999999999e22, 1e-270, 1e270, 9007199254740992.0, 1125899906842624.0, 123456789012345678.0})
    void isTheDecimalDoubleToStringShows(double x) {
        assertShortest(x);
    }

    @Test
    void everyBinadeAndRandomDoubles() {
        for (var e = -899; e <= 900; e++) {
            var power = Math.scalb(1.0, e);
            assertShortest(power);
            assertShortest(Math.nextUp(power));
            assertShortest(Math.nextDown(power));
        }
        var random = new Random(20261029);
        for (var i = 0; i < 200_000; i++) {
            assertShortest(Math.scalb(random.nextDouble() + 0.5, random.nextInt(-899, 900)));
            assertShortest(BigDecimal.valueOf(random.nextLong(), random.nextInt(-20, 40)).doubleValue());
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.0, Double.MIN_VALUE, 1e-300, 1e300, Double.MAX_VALUE, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void hasNoneOutOfRange(double x) {
        assertEquals(Decimal.NONE, Decimal.of(x));
    }

    @Test
    void exactValueHasAllTheBinaryDigits() {
        assertSameValue(new BigDecimal("0.1000000000000000055511151231257827021181583404541015625"),
                Decimal.exactValue(0.1));
        assertSameValue(new BigDecimal("-99999999999999991611392"), Decimal.exactValue(-1e23));
        assertSameValue(new BigDecimal(BigInteger.valueOf(5).pow(1074), 1074), Decimal.exactValue(Double.MIN_VALUE));
        assertSameValue(BigDecimal.ZERO, Decimal.exactValue(-0.0));
        assertSameValue(new BigDecimal("3"), Decimal.exactValue(3.0));
    }

    private static void assertSameValue(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), () -> "expected: " + expected + " but was: " + actual);
    }

    private static void assertShortest(double x) {
        var d = Decimal.of(x);
        assertNotEquals(Decimal.NONE, d, () -> "no decimal of " + x);
        var expected = BigDecimal.valueOf(x).stripTrailingZeros();
        var actual = Decimal.toBigDecimal(x, d);
        assertEquals(0, expected.compareTo(actual), () -> x + ": " + actual);
        assertNotEquals(0, Decimal.digits(x, d) % 10, () -> x + " has trailing zeros: " + actual);
        var error = expected.subtract(new BigDecimal(x)).doubleValue();
        assertTrue(Math.abs(Decimal.error(x, d) - error) <= Math.abs(x) * 0x1p-101, () -> x + " error");
    }
}
