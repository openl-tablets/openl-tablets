package org.openl.rules.testmethod.result;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class NumberComparatorTest {

    private static final BigDecimal TWO_THIRDS = BigDecimal.TWO.divide(BigDecimal.valueOf(3), MathContext.DECIMAL128);

    @Test
    void test() {
        var comp = NumberComparator.INSTANCE;

        assertTrue(comp.isEqual(null, null));

        var value = (double) 10;

        assertFalse(comp.isEqual(value, null));

        assertFalse(comp.isEqual(null, value));

        assertTrue(comp.isEqual(value, value));

        // Tests with delta
        var value1 = 590.4563546464;
        var value2 = 590.456377867;
        var value3 = 550.46;

        assertFalse(new NumberComparator(null).isEqual(value2, value1));
        assertFalse(new NumberComparator(new BigDecimal("0.00001")).isEqual(value2, value1));
        assertTrue(new NumberComparator(new BigDecimal("0.0001")).isEqual(value2, value1));
        assertTrue(new NumberComparator(new BigDecimal("100")).isEqual(value3, value1));
    }

    @Test
    void bigDecimalWithAllDigits() {
        var comparator = TestResultComparatorFactory.getComparator(BigDecimal.class, null);

        assertFalse(comparator.isEqual(new BigDecimal("12345678901234567890.123456788"),
                new BigDecimal("12345678901234567890.123456789")));
        assertTrue(comparator.isEqual(new BigDecimal("12345678901234567890.123456789"),
                new BigDecimal("12345678901234567890.123456789")));
        assertTrue(comparator.isEqual(new BigDecimal("2.50"), new BigDecimal("2.5")));
        assertFalse(comparator.isEqual(new BigDecimal("0.6666666666666666"), TWO_THIRDS));
        assertTrue(comparator.isEqual(new BigDecimal("0.6666666666666666666666666666666667"), TWO_THIRDS));
    }

    @Test
    void bigDecimalWithinPrecision() {
        var five = TestResultComparatorFactory.getComparator(BigDecimal.class, new BigDecimal("0.00001"));
        assertTrue(five.isEqual(new BigDecimal("0.00001"), BigDecimal.ZERO));
        assertFalse(five.isEqual(new BigDecimal("0.000011"), BigDecimal.ZERO));

        var ten = TestResultComparatorFactory.getComparator(BigDecimal.class, new BigDecimal("1E-10"));
        assertTrue(ten.isEqual(new BigDecimal("0.6666666667"), TWO_THIRDS));
        assertFalse(ten.isEqual(new BigDecimal("0.666666667"), TWO_THIRDS));

        // From 1 on, the difference must be smaller than the delta
        var hundreds = TestResultComparatorFactory.getComparator(BigDecimal.class, new BigDecimal("100"));
        assertTrue(hundreds.isEqual(new BigDecimal("31400"), new BigDecimal("31415.93")));
        assertFalse(hundreds.isEqual(new BigDecimal("31300"), new BigDecimal("31400")));
    }

    @Test
    void bigIntegerWithinPrecision() {
        var whole = TestResultComparatorFactory.getComparator(BigInteger.class, BigDecimal.ONE);

        assertFalse(whole.isEqual(new BigInteger("123456789012345678901"), new BigInteger("123456789012345678999")));
        assertTrue(whole.isEqual(new BigInteger("123456789012345678901"), new BigInteger("123456789012345678901")));
    }

    @Test
    void precisionBeyondDouble() {
        // The precision -309 allows 1E+309, which a Double holds only as Infinity
        var huge = BigDecimal.ONE.scaleByPowerOfTen(309);
        assertTrue(TestResultComparatorFactory.getComparator(BigDecimal.class, huge)
                .isEqual(new BigDecimal("9E+308"), BigDecimal.ZERO));
        assertFalse(TestResultComparatorFactory.getComparator(BigDecimal.class, huge)
                .isEqual(new BigDecimal("1E+309"), BigDecimal.ZERO));
        assertTrue(TestResultComparatorFactory.getComparator(Double.class, huge).isEqual(Double.MAX_VALUE, 0.0));

        // The precision 324 allows 1E-324, which a Double holds only as 0
        var tiny = BigDecimal.ONE.scaleByPowerOfTen(-324);
        assertTrue(TestResultComparatorFactory.getComparator(BigDecimal.class, tiny)
                .isEqual(new BigDecimal("1E-324"), BigDecimal.ZERO));
        assertFalse(TestResultComparatorFactory.getComparator(BigDecimal.class, tiny)
                .isEqual(new BigDecimal("2E-324"), BigDecimal.ZERO));
    }

    @Test
    void floatingPointWithinPrecision() {
        // A double difference is computed in binary: 0.4 - 0.3 is 0.10000000000000003
        assertFalse(TestResultComparatorFactory.getComparator(Double.class, new BigDecimal("0.1")).isEqual(0.4, 0.3));
        // The precision 5 allows the double nearest to 0.00001, not 9.999999999999999E-6
        assertTrue(TestResultComparatorFactory.getComparator(Double.class, new BigDecimal("0.00001"))
                .isEqual(0.00001, 0.0));
    }

    @Test
    void bigNumbersWithOtherNumbers() {
        var comparator = NumberComparator.INSTANCE;

        assertTrue(comparator.isEqual(2, new BigDecimal("2.0")));
        assertTrue(comparator.isEqual(new BigDecimal("2"), 2L));
        assertTrue(comparator.isEqual((short) 2, BigInteger.TWO));
        assertTrue(comparator.isEqual((byte) 2, BigInteger.TWO));
        assertTrue(comparator.isEqual(0.1, new BigDecimal("0.1")));
        assertTrue(comparator.isEqual(0.1f, new BigDecimal("0.1")));
        assertFalse(comparator.isEqual(0.1, new BigDecimal("0.10000000000000001")));
        assertFalse(comparator.isEqual(Double.NaN, BigDecimal.ZERO));
        assertFalse(comparator.isEqual(BigDecimal.ZERO, Float.POSITIVE_INFINITY));
        assertFalse(comparator.isEqual(new AtomicInteger(2), BigDecimal.TWO));
        assertFalse(comparator.isEqual(BigDecimal.ONE, null));
        assertTrue(comparator.isEqual(null, null));
    }
}
