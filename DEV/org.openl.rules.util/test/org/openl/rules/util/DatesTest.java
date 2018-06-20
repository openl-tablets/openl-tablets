package org.openl.rules.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class DatesTest {

    private static final Date DEF_DATE = new Date();
    private static final Double ZERO_DOUBLE = 0.0d;

    @Test(expected = IllegalArgumentException.class)
    public void test_dateDif_shouldThrowIllegalArgumentException_whenUnitNameInUnknown() {
        Dates.dateDif(null, null, "SOME_NAME");
    }

    @Test
    public void test_dateDif_shouldReturnNull_whenOneDateParameterIsNull() {
        assertNull(Dates.dateDif(null, DEF_DATE, "D"));
        assertNull(Dates.dateDif(DEF_DATE, null, "D"));
        assertNull(Dates.dateDif(null, DEF_DATE, "W"));
        assertNull(Dates.dateDif(DEF_DATE, null, "W"));
        assertNull(Dates.dateDif(null, DEF_DATE, "M"));
        assertNull(Dates.dateDif(DEF_DATE, null, "M"));
        assertNull(Dates.dateDif(null, DEF_DATE, "Y"));
        assertNull(Dates.dateDif(DEF_DATE, null, "Y"));
        assertNull(Dates.dateDif(null, DEF_DATE, "MD"));
        assertNull(Dates.dateDif(DEF_DATE, null, "MD"));
        assertNull(Dates.dateDif(null, DEF_DATE, "YD"));
        assertNull(Dates.dateDif(DEF_DATE, null, "YD"));
        assertNull(Dates.dateDif(null, DEF_DATE, "YM"));
        assertNull(Dates.dateDif(DEF_DATE, null, "YM"));
        assertNull(Dates.dateDif(null, DEF_DATE, "MF"));
        assertNull(Dates.dateDif(DEF_DATE, null, "MF"));
        assertNull(Dates.dateDif(null, DEF_DATE, "WF"));
        assertNull(Dates.dateDif(DEF_DATE, null, "WF"));
        assertNull(Dates.dateDif(null, DEF_DATE, "YF"));
        assertNull(Dates.dateDif(DEF_DATE, null, "YF"));
        assertNull(Dates.dateDif(null, DEF_DATE, "YMF"));
        assertNull(Dates.dateDif(DEF_DATE, null, "YMF"));
    }

    @Test
    public void test_dateDif_shouldReturnZero_whenDateParametersAreNull() {
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "D"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "W"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "M"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "Y"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "MD"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "YD"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "YM"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "MF"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "WF"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "YF"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(null, null, "YMF"));
    }

    @Test
    public void test_dateDif_shouldReturnIntResult() {
        final Date start = getDate(28, 3, 2012);
        final Date end = getDate(29, 3, 2013);

        assertEquals(new Double(366), Dates.dateDif(start, end, "D"));
        assertEquals(new Double(52), Dates.dateDif(start, end, "W"));
        assertEquals(new Double(12), Dates.dateDif(start, end, "M"));
        assertEquals(new Double(1), Dates.dateDif(start, end, "Y"));
        assertEquals(new Double(1), Dates.dateDif(start, end, "MD"));
        assertEquals(new Double(1), Dates.dateDif(start, end, "YD"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(start, end, "YM"));

        assertEquals(new Double(10), Dates.dateDif(start, getDate(27, 2, 2013), "M"));
        assertEquals(new Double(2), Dates.dateDif(start, getDate(27, 1, 2015), "Y"));
        assertEquals(new Double(23), Dates.dateDif(start, getDate(20, 1, 2015), "MD"));
        assertEquals(new Double(298), Dates.dateDif(start, getDate(20, 1, 2015), "YD"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(getDate(31, 1, 2013), getDate(1, 3, 2013), "MD"));
    }

    @Test
    public void test_dateDif_shouldReturnNegativeIntResult() {
        final Date start = getDate(28, 3, 2012);
        final Date end = getDate(29, 3, 2013);

        assertEquals(new Double(-366), Dates.dateDif(end, start, "D"));
        assertEquals(new Double(-52), Dates.dateDif(end, start, "W"));
        assertEquals(new Double(-12), Dates.dateDif(end, start, "M"));
        assertEquals(new Double(-1), Dates.dateDif(end, start, "Y"));
        assertEquals(new Double(-1), Dates.dateDif(end, start, "MD"));
        assertEquals(new Double(-1), Dates.dateDif(end, start, "YD"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(end, start, "YM"));
    }

    @Test
    public void test_dateDif_shouldReturnFractionalResult() {
        final Date start = getDate(28, 3, 2012);
        final Date end = getDate(29, 3, 2013);

        assertEquals(new Double(52.285714285714285d), Dates.dateDif(start, end, "WF"));
        assertEquals(new Double(12.03225806451613d), Dates.dateDif(start, end, "MF"));
        assertEquals(new Double(1.0027397260273974d), Dates.dateDif(start, end, "YF"));
        assertEquals(new Double(0.03225806451612903d), Dates.dateDif(start, end, "YMF"));
        assertEquals(new Double(10.967741935483872), Dates.dateDif(start, getDate(27, 2, 2013), "MF"));
        assertEquals(new Double(2.8356164383561646), Dates.dateDif(start, getDate(27, 1, 2015), "YF"));
    }

    @Test
    public void test_dateDif_shouldReturnNegativeFractionalResult() {
        final Date start = getDate(28, 3, 2012);
        final Date end = getDate(29, 3, 2013);

        assertEquals(new Double(-52.285714285714285d), Dates.dateDif(end, start, "WF"));
        assertEquals(new Double(-12.03225806451613d), Dates.dateDif(end, start, "MF"));
        assertEquals(new Double(-1.0027397260273974d), Dates.dateDif(end, start, "YF"));
        assertEquals(new Double(-0.03225806451612903d), Dates.dateDif(end, start, "YMF"));
    }

    private static Date getDate(int day, int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

}
