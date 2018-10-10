package org.openl.rules.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class DatesTest {


    @Test
    public void testDate() {
        assertNull(Dates.toString(null));
        assertEquals(new Date(-1899, 0, 1), Dates.Date(1,1,1));
        assertEquals(new Date(118, 6, 12), Dates.Date(2018,7,12));
        assertEquals(new Date(80, 6, 12), Dates.Date(1980,7,12));
        assertEquals(new Date(-1820, 6, 12), Dates.Date(80,7,12));
        assertEquals(new Date(116, 1, 29), Dates.Date(2016,2,29));
    }

    @Test(expected = Exception.class)
    public void testDateWrongMonth() {
        Dates.Date(2018,13,1);
    }

    @Test(expected = Exception.class)
    public void testDateWrongDay() {
        Dates.Date(2018,2,29);
    }

    @Test(expected = Exception.class)
    public void testDateWrongYear() {
        Dates.Date(0,1,1);
    }

    @Test
    public void testToString() {
        assertNull(Dates.toString(null));
        assertEquals("07/12/1980", Dates.toString(new Date(80, 6, 12, 23, 59)));
        assertEquals("07/12/1980", Dates.toString(new Date(80, 6, 12)));
        assertEquals("12.07.1980", Dates.toString(new Date(80, 6, 12, 23, 59), "dd.MM.yyyy"));
        assertEquals("12-Jul-1980", Dates.toString(new Date(80, 6, 12), "dd-MMM-yyyy"));
    }

    @Test
    public void testToDate() throws Exception {
        assertNull(Dates.toDate(null));
        assertNull(Dates.toDate(""));
        assertNull(Dates.toDate(" "));
        assertNull(Dates.toDate("  \t  "));

        assertEquals(new Date(50, 11, 31), Dates.toDate("12/31/50"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("7/12/80"));
        assertEquals(new Date(138, 6, 12), Dates.toDate("7/12/38"));
        assertEquals(new Date(-1820, 6, 12), Dates.toDate("7/12/080"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980"));

        assertNull(Dates.toDate(null, null));
        assertNull(Dates.toDate("", ""));
        assertNull(Dates.toDate(" ", " "));
        assertNull(Dates.toDate("  \t  ", "  \t"));

        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", null));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", ""));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", " "));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", "  \t"));

        assertEquals(new Date(80, 6, 12), Dates.toDate("7/12/80", "MM/dd/yy"));
        assertEquals(new Date(-1820, 6, 12), Dates.toDate("7/12/80", "M/d/yyyy"));
        assertEquals(new Date(-1820, 6, 12), Dates.toDate("07/12/0080", "M/d/yyyy"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("Date is: 12 Jul 1980 [+]", "'Date is: 'dd MMM yyyy"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("Date is: 12 Jul 1980 year", "'Date is: 'dd MMM yyyy 'year'"));
        assertEquals(new Date(80, 0, 1), Dates.toDate("Date: 1980 year", "'Date: 'yyyy 'year'"));
        assertEquals(new Date(70, 0, 19), Dates.toDate("Date: 19 days from the 1th January 1970", "'Date: 'd 'days'"));
    }

    @Test(expected = Exception.class)
    public void testToDateLetter() throws Exception {
        Dates.toDate("13/13/2013");
    }

    @Test(expected = Exception.class)
    public void testToDateWrongPattern() throws Exception {
        Dates.toDate("12/12/2013", "a");
    }

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
        assertNull(Dates.dateDif(null, null, "D"));
        assertNull(Dates.dateDif(null, null, "W"));
        assertNull(Dates.dateDif(null, null, "M"));
        assertNull(Dates.dateDif(null, null, "Y"));
        assertNull(Dates.dateDif(null, null, "MD"));
        assertNull(Dates.dateDif(null, null, "YD"));
        assertNull(Dates.dateDif(null, null, "YM"));
        assertNull(Dates.dateDif(null, null, "MF"));
        assertNull(Dates.dateDif(null, null, "WF"));
        assertNull(Dates.dateDif(null, null, "YF"));
        assertNull(Dates.dateDif(null, null, "YMF"));
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
