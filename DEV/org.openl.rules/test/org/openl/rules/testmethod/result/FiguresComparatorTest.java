package org.openl.rules.testmethod.result;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * @author Yury Molchan
 */
class FiguresComparatorTest {

    private static final BigDecimal TWO_THIRDS = BigDecimal.TWO.divide(BigDecimal.valueOf(3), MathContext.DECIMAL128);

    @Test
    void significantFigures() {
        var four = figures(4);
        // 1.1 ** 3 is 1.3310000000000004, and 0.011 ** 3 is 1.3309999999999998E-6
        assertTrue(four.isEqual(1.331, 1.3310000000000004));
        assertTrue(four.isEqual(1.331E-6, 1.3309999999999998E-6));
        assertTrue(four.isEqual(1331000.0, 1331000.4));
                assertFalse(four.isEqual(1.33, 1.3310000000000004));
        assertFalse(four.isEqual(-1.331, 1.331));
        assertTrue(four.isEqual(-1.331, -1.3310000000000004));
    }

    @Test
    void roundedAtTheLastFigure() {
        // 10.004 rounds to 10.00 in 4 figures, and to 10.004 in 5 figures
        assertTrue(figures(4).isEqual(10.0, 10.004));
        assertFalse(figures(5).isEqual(10.0, 10.004));
        assertTrue(figures(1).isEqual(0.0001, 0.00006));
        assertFalse(figures(1).isEqual(0.0001, 0.00004));
        // halves round up: 99.005 is 99.01, and 98.995 is 99.00
        assertFalse(figures(4).isEqual(99.0, 99.005));
        assertTrue(figures(4).isEqual(99.0, 98.995));
        assertTrue(figures(4).isEqual(99.0, 99.0049));
        assertFalse(figures(4).isEqual(-99.0, -99.005));
        assertTrue(figures(4).isEqual(-99.0, -98.995));
        // the expected value is rounded too
        assertTrue(figures(4).isEqual(1.3314, 1.3306));
        assertTrue(figures(2).isEqual(11.0, 10.648));
        assertTrue(figures(2).isEqual(11000, 10648));
        assertFalse(figures(2).isEqual(10.0, 10.648));
    }

    @Test
    void significantFiguresOfZero() {
        var four = figures(4);
        assertTrue(four.isEqual(0.0, 0.0));
        assertTrue(four.isEqual(0.0, -0.0));
        assertTrue(four.isEqual(0, BigDecimal.ZERO));
        assertFalse(four.isEqual(1E-300, 0.0));
        assertFalse(four.isEqual(0.0, 1E-300));
    }

    @Test
    void significantFiguresOfSpecialValues() {
        var four = figures(4);
        assertTrue(four.isEqual(Double.NaN, Double.NaN));
        assertTrue(four.isEqual(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertFalse(four.isEqual(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertFalse(four.isEqual(Double.POSITIVE_INFINITY, Double.MAX_VALUE));
        assertFalse(four.isEqual(Double.NaN, 1.0));
        assertTrue(four.isEqual(Float.NaN, Float.NaN));
        assertFalse(four.isEqual(Float.POSITIVE_INFINITY, BigDecimal.ONE));
        assertFalse(four.isEqual(new BigDecimal("1E400"), Double.POSITIVE_INFINITY));
        assertFalse(four.isEqual(Double.NEGATIVE_INFINITY, new BigDecimal("-1E400")));
        assertFalse(written().isEqual(new BigDecimal("1E400"), Double.POSITIVE_INFINITY));
        assertFalse(four.isEqual(new AtomicInteger(2), 2));
        assertFalse(four.isEqual(1.0, "1.0"));
        assertTrue(four.isEqual(null, null));
        assertFalse(four.isEqual(1.0, null));
        assertTrue(written().isEqual(Double.NaN, Double.NaN));
        assertFalse(written().isEqual(Double.NaN, 1.0));
    }

    @Test
    void floatSignificantFigures() {
        // a Float is compared with the digits it shows: 1.331f is 1.331
        assertTrue(figures(4).isEqual(1.331f, 1.3314f));
        assertFalse(figures(5).isEqual(1.331f, 1.3314f));
        assertTrue(figures(20).isEqual(0.1f, 0.1));
    }

    @Test
    void bigNumbersInSignificantFigures() {
        var ten = TestResultComparatorFactory.getComparator(BigDecimal.class, new Tolerance.SignificantFigures(10));
        assertTrue(ten.isEqual(new BigDecimal("0.6666666667"), TWO_THIRDS));
        assertFalse(ten.isEqual(new BigDecimal("0.666666668"), TWO_THIRDS));
        // all the digits count, beyond the 17 a double holds
        var twenty = TestResultComparatorFactory.getComparator(BigInteger.class, new Tolerance.SignificantFigures(20));
        assertTrue(twenty.isEqual(new BigInteger("123456789012345678900"), new BigInteger("123456789012345678901")));
        assertFalse(twenty.isEqual(new BigInteger("123456789012345679000"), new BigInteger("123456789012345678901")));
        assertTrue(ten.isEqual(new BigDecimal("1.331"), 1.3310000000000004));
    }

    @Test
    void wholeNumbersInSignificantFigures() {
        var two = TestResultComparatorFactory.getComparator(Integer.class, new Tolerance.SignificantFigures(2));
        assertTrue(two.isEqual(1000, 1040));
        assertFalse(two.isEqual(1000, 1060));
    }

    @Test
    void arrayInSignificantFigures() {
        var comparator = TestResultComparatorFactory.getComparator(double[].class, new Tolerance.SignificantFigures(4));
        assertTrue(comparator.isEqual(new double[]{1.331, 1.0}, new double[]{1.3310000000000004, 1.0}));
        assertFalse(comparator.isEqual(new double[]{1.331, 1.0}, new double[]{1.3310000000000004, 1.1}));
    }

    @Test
    void objectInSignificantFigures() {
        var comparator = TestResultComparatorFactory.getComparator(Object.class, new Tolerance.SignificantFigures(4));
        assertTrue(comparator.isEqual(1.331, 1.3310000000000004));
        assertFalse(comparator.isEqual(1.33, 1.3310000000000004));
    }

    @Test
    void figuresOfTheExpectedValue() {
        var written = written();
        // a BigDecimal keeps the figures it is written with
        assertTrue(written.isEqual(new BigDecimal("10.00"), 10.004));
        assertFalse(written.isEqual(new BigDecimal("10.000"), 10.004));
        assertTrue(written.isEqual(new BigDecimal("0.0001"), 0.00006));
        assertFalse(written.isEqual(new BigDecimal("0.0001"), 0.00004));
    }

    @Test
    void wholeBigDecimalWithoutTrailingZeros() {
        // a BigDecimal read from a cell drops its trailing zeros: 1000 is 1E+3, and every digit still counts
        var written = written();
        assertTrue(written.isEqual(new BigDecimal("1E+3"), 1000.4));
        assertFalse(written.isEqual(new BigDecimal("1E+3"), 1000.5));
        assertFalse(written.isEqual(new BigDecimal("1E+3"), 1400));
    }

    @Test
    void decimalPlacesFarFromTheNumbers() {
        // a place below both numbers compares them exactly, without rounding
        var fine = new FiguresComparator(new Tolerance.ExpectedFigures(999_999_999));
        assertTrue(fine.isEqual(1.5, 1.5));
        assertFalse(fine.isEqual(1.5, 1.5000001));
        assertTrue(figures(Integer.MAX_VALUE).isEqual(1.5E-300, 1.5E-300));
        assertFalse(figures(Integer.MAX_VALUE).isEqual(1.5E-300, 1.6E-300));
        // a place far above both numbers rounds both to zero
        var coarse = new FiguresComparator(new Tolerance.ExpectedFigures(-999_999_999));
        assertTrue(coarse.isEqual(5, 7));
        assertTrue(coarse.isEqual(new BigDecimal("1E+300"), 0));
    }

    @Test
    void figuresOfTheExpectedValueAsItReads() {
        var written = written();
        // a Double reads in its shortest form, and every digit of a whole number counts
        assertTrue(written.isEqual(10.0, 10.4));
        assertFalse(written.isEqual(10.0, 10.6));
        assertTrue(written.isEqual(1.25, 1.254));
        assertFalse(written.isEqual(1.25, 1.256));
        assertTrue(written.isEqual(1.0E20, 1.0E20));
        assertTrue(written.isEqual(0.5f, 0.54));
        assertFalse(written.isEqual(0.5f, 0.56));
        assertTrue(written.isEqual(1000, 1000.4));
        assertFalse(written.isEqual(1000, 1000.5));
    }

    @Test
    void figuresOfTheWrittenCell() {
        // 10 written as 10.00
        var hundredths = new FiguresComparator(new Tolerance.ExpectedFigures(2));
        assertTrue(hundredths.isEqual(10.0, 10.004));
        assertTrue(hundredths.isEqual(10.0, 9.995));
        assertFalse(hundredths.isEqual(10.0, 10.005));
        // 1000 written as 1.0E+3
        var hundreds = new FiguresComparator(new Tolerance.ExpectedFigures(-2));
        assertTrue(hundreds.isEqual(1000, 1049));
        assertFalse(hundreds.isEqual(1000, 1050));
    }

    private static TestResultComparator figures(int count) {
        return new FiguresComparator(new Tolerance.SignificantFigures(count));
    }

    private static TestResultComparator written() {
        return new FiguresComparator(new Tolerance.ExpectedFigures(null));
    }
}
