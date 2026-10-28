package org.openl.rules.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

class OperatorsTest {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Test
    void testAddToDate() throws Exception {
        Date date = Operators.add(dateFormat.parse("2012-10-01"), 120);
        assertEquals("2013-01-29", dateFormat.format(date)); // date without time
        assertEquals(dateFormat.parse("2013-01-29"), date); // date with time
    }

    @Test
    void testSubstractDays() throws Exception {
        Date date = Operators.subtract(dateFormat.parse("2012-04-30"), 120);
        assertEquals("2012-01-01", dateFormat.format(date)); // date without time
        assertEquals(dateFormat.parse("2012-01-01"), date); // date with time
    }

    @Test
    void testSubtractDates() throws Exception {
        Integer diff = Operators.subtract(dateFormat.parse("2013-01-29"), dateFormat.parse("2012-10-01"));
        assertEquals(Integer.valueOf(120), diff);
    }

    @Test
    void doublesComputeWithDecimals() {
        assertEquals(0.3, Operators.add(0.1, 0.2));
        assertEquals(0.2, Operators.subtract(0.3, 0.1));
        assertEquals(115.0, Operators.multiply(1.15, 100.0));
        assertEquals(6.9, Operators.divide(0.69, 0.1));
        assertEquals(0.0, Operators.rem(1.1, 0.1));
    }

    @Test
    void boxedDoublesComputeWithDecimals() {
        assertEquals(0.3, Operators.add(Double.valueOf(0.1), Double.valueOf(0.2)));
        assertEquals(0.2, Operators.subtract(Double.valueOf(0.3), Double.valueOf(0.1)));
        assertEquals(115.0, Operators.multiply(Double.valueOf(1.15), Double.valueOf(100.0)));
        assertEquals(6.9, Operators.divide(Double.valueOf(0.69), Double.valueOf(0.1)));
        assertNull(Operators.add((Double) null, (Double) null));
        assertEquals(-0.1, Operators.subtract(null, Double.valueOf(0.1)));
        assertNull(Operators.multiply(Double.valueOf(0.1), null));
        assertNull(Operators.divide(null, Double.valueOf(0.1)));
    }

    @Test
    @SuppressWarnings("deprecation")
    void incrementAndDecrementComputeWithDecimals() {
        assertEquals(-3.35, Operators.inc(-4.35));
        assertEquals(3.35, Operators.dec(4.35));
        assertEquals(-3.35, Operators.inc(Double.valueOf(-4.35)));
        assertEquals(3.35, Operators.dec(Double.valueOf(4.35)));
    }
}
