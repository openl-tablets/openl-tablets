package org.openl.binding.impl;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

public class OperatorsTest {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Test
    public void testAddToDate() throws Exception {
        Date date = Operators.add(dateFormat.parse("2012-10-01"), 120);
        assertEquals("2013-01-29", dateFormat.format(date)); // date without time
        assertEquals(dateFormat.parse("2013-01-29"), date); // date with time
    }

    @Test
    public void testSubstractDays() throws Exception {
        Date date = Operators.subtract(dateFormat.parse("2012-04-30"), 120);
        assertEquals("2012-01-01", dateFormat.format(date)); // date without time
        assertEquals(dateFormat.parse("2012-01-01"), date); // date with time
    }

    @Test
    public void testSubtractDates() throws Exception {
        Integer diff = Operators.subtract(dateFormat.parse("2013-01-29"), dateFormat.parse("2012-10-01"));
        assertEquals(new Integer(120), diff);
    }
}
