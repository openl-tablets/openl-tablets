package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

class DatesTest {

    @Test
    void testDate() {
        assertNull(Dates.toString(null));
        assertEquals(new Date(-1899, 0, 1), Dates.Date(1, 1, 1));
        assertEquals(new Date(118, 6, 12), Dates.Date(2018, 7, 12));
        assertEquals(new Date(80, 6, 12), Dates.Date(1980, 7, 12));
        assertEquals(new Date(-1820, 6, 12), Dates.Date(80, 7, 12));
        assertEquals(new Date(116, 1, 29), Dates.Date(2016, 2, 29));
        assertEquals(new Date(116, 1, 29, 11, 11, 11), Dates.Date(2016, 2, 29, 11, 11, 11));
    }

    @Test
    void testDateWrongMonth() {
        assertNull(Dates.Date(2018, 13, 1));
    }

    @Test
    void testDateWrongDay() {
        assertNull(Dates.Date(2018, 2, 29));
    }

    @Test
    void testDateWrongYear() {
        assertNull(Dates.Date(0, 1, 1));
    }

    @Test
    void testDateWrongHour() {
        assertNull(Dates.Date(2022, 1, 1, 24, 1, 1));
    }

    @Test
    void testDateWrongMinute() {
        assertNull(Dates.Date(2022, 1, 1, 1, 60, 1));
    }

    @Test
    void testDateWrongSecond() {
        assertNull(Dates.Date(2022, 1, 1, 1, 1, 60));
    }

    @Test
    void testToString() {
        assertNull(Dates.toString(null));
        assertEquals("07/12/1980", Dates.toString(new Date(80, 6, 12, 23, 59)));
        assertEquals("07/12/1980", Dates.toString(new Date(80, 6, 12)));
        assertEquals("12.07.1980", Dates.toString(new Date(80, 6, 12, 23, 59), "dd.MM.yyyy"));
        assertEquals("12-Jul-1980", Dates.toString(new Date(80, 6, 12), "dd-MMM-yyyy"));
    }

    @Test
    void testFormat() {
        assertNull(Dates.format(null));
        assertNull(Dates.format(null, "dd.MM.yyyy"));
        assertEquals("07/12/1980", Dates.format(new Date(80, 6, 12, 23, 59)));
        assertEquals("12-Jul-1980", Dates.format(new Date(80, 6, 12), "dd-MMM-yyyy"));
    }

    @Test
    void testToDate() {
        assertNull(Dates.toDate(null));
        assertNull(Dates.toDate(""));
        assertNull(Dates.toDate(" "));
        assertNull(Dates.toDate("  \t  "));

        assertEquals(new Date(50, 11, 31), Dates.toDate("12/31/50"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("7/12/80"));
        assertEquals(new Date(138, 6, 12), Dates.toDate("7/12/38"));
        assertEquals(new Date(-1820, 6, 12), Dates.toDate("7/12/080"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("1980-07-12"));
        assertEquals(date(1980, 7, 12), Dates.toDate(" 1980-07-12\t"));
        assertEquals(date(1980, 7, 12), Dates.toDate("\n07/12/1980 "));

        assertNull(Dates.toDate("13/13/2013"));
        assertNull(Dates.toDate("04/30/2015abc"));
        assertNull(Dates.toDate("2024-03-15T10:20"));
        assertNull(Dates.toDate("2024-03-15 x"));
        assertNull(Dates.toDate("x 2024-03-15"));
    }

    @Test
    void testToDateWithFormat() {
        assertNull(Dates.toDate(null, null));
        assertNull(Dates.toDate("", ""));
        assertNull(Dates.toDate(" ", " "));
        assertNull(Dates.toDate("  \t  ", "  \t"));

        assertEquals(new Date(80, 6, 12), Dates.toDate("7/12/80", "MM/dd/yy"));
        assertEquals(new Date(-1820, 6, 12), Dates.toDate("7/12/80", "M/d/yyyy"));
        assertEquals(new Date(-1820, 6, 12), Dates.toDate("07/12/0080", "M/d/yyyy"));
        assertNull(Dates.toDate("Date is: 12 Jul 1980 [+]", "'Date is: 'dd MMM yyyy"));
        assertEquals(date(1980, 7, 12), Dates.toDate("Date is: 12 Jul 1980", "'Date is: 'dd MMM yyyy"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("Date is: 12 Jul 1980 year", "'Date is: 'dd MMM yyyy 'year'"));
        assertEquals(new Date(80, 0, 1), Dates.toDate("Date: 1980 year", "'Date: 'yyyy 'year'"));
        assertNull(Dates.toDate("Date: 19 days from the 1th January 1970", "'Date: 'd 'days'"));
        assertEquals(date(1970, 1, 19), Dates.toDate("Date: 19 days", "'Date: 'd 'days'"));
        assertEquals(date(2024, 3, 15), Dates.toDate(" 15.03.2024 ", "dd.MM.yyyy"));
        assertEquals(date(2024, 3, 15, 10, 20), Dates.toDate("2024-03-15T10:20", "yyyy-MM-dd'T'HH:mm"));

        assertNull(Dates.toDate("12/12/2013", "a"));
        assertNull(Dates.toDate("15.03.2024x", "dd.MM.yyyy"));
        assertNull(Dates.toDate("2024-03-15 10:20", "yyyy-MM-dd"));
    }

    @Test
    void testToDateWithBlankFormat() {
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", null));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", ""));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", " "));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", "  \t"));
        assertEquals(date(1980, 7, 12), Dates.toDate("1980-07-12", null));
        assertEquals(date(1980, 7, 12), Dates.toDate("1980-07-12", ""));
        assertEquals(date(1980, 7, 12), Dates.toDate("1980-07-12", " "));
        assertEquals(date(2012, 1, 11), Dates.toDate("01/11/12", ""));
    }

    private static Date date(int year, int month, int day) {
        return date(year, month, day, 0, 0);
    }

    private static Date date(int year, int month, int day, int hours, int minutes) {
        return new GregorianCalendar(year, month - 1, day, hours, minutes).getTime();
    }

    private static final Date DEF_DATE = new Date();
    private static final Double ZERO_DOUBLE = 0.0d;

    @Test
    void test_dateDif_shouldThrowIllegalArgumentException_whenUnitNameInUnknown() {
        assertThrows(IllegalArgumentException.class, () -> Dates.dateDif(null, null, "SOME_NAME"));
    }

    @Test
    void test_dateDif_shouldReturnNull_whenUnitNameIsNull() {
        assertNull(Dates.dateDif(DEF_DATE, DEF_DATE, null));
        assertNull(Dates.dateDif(null, null, null));
    }

    @Test
    void test_dateDif_shouldReturnNull_whenOneDateParameterIsNull() {
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
    void test_dateDif_shouldReturnZero_whenDateParametersAreNull() {
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
    void test_dateDif_shouldReturnIntResult() {
        final Date start = getDate(28, 3, 2012);
        final Date end = getDate(29, 3, 2013);

        assertEquals(Double.valueOf(366), Dates.dateDif(start, end, "D"));
        assertEquals(Double.valueOf(52), Dates.dateDif(start, end, "W"));
        assertEquals(Double.valueOf(12), Dates.dateDif(start, end, "M"));
        assertEquals(Double.valueOf(1), Dates.dateDif(start, end, "Y"));
        assertEquals(Double.valueOf(1), Dates.dateDif(start, end, "MD"));
        assertEquals(Double.valueOf(1), Dates.dateDif(start, end, "YD"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(start, end, "YM"));

        assertEquals(Double.valueOf(10), Dates.dateDif(start, getDate(27, 2, 2013), "M"));
        assertEquals(Double.valueOf(2), Dates.dateDif(start, getDate(27, 1, 2015), "Y"));
        assertEquals(Double.valueOf(23), Dates.dateDif(start, getDate(20, 1, 2015), "MD"));
        assertEquals(Double.valueOf(298), Dates.dateDif(start, getDate(20, 1, 2015), "YD"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(getDate(31, 1, 2013), getDate(1, 3, 2013), "MD"));
    }

    @Test
    void test_dateDif_shouldReturnNegativeIntResult() {
        final Date start = getDate(28, 3, 2012);
        final Date end = getDate(29, 3, 2013);

        assertEquals(Double.valueOf(-366), Dates.dateDif(end, start, "D"));
        assertEquals(Double.valueOf(-52), Dates.dateDif(end, start, "W"));
        assertEquals(Double.valueOf(-12), Dates.dateDif(end, start, "M"));
        assertEquals(Double.valueOf(-1), Dates.dateDif(end, start, "Y"));
        assertEquals(Double.valueOf(-1), Dates.dateDif(end, start, "MD"));
        assertEquals(Double.valueOf(-1), Dates.dateDif(end, start, "YD"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(end, start, "YM"));
    }

    @Test
    void test_dateDif_shouldReturnFractionalResult() {
        final Date start = getDate(28, 3, 2012);
        final Date end = getDate(29, 3, 2013);

        assertEquals(Double.valueOf(52.285714285714285d), Dates.dateDif(start, end, "WF"));
        assertEquals(Double.valueOf(12.03225806451613d), Dates.dateDif(start, end, "MF"));
        assertEquals(Double.valueOf(1.0027397260273974d), Dates.dateDif(start, end, "YF"));
        assertEquals(Double.valueOf(0.03225806451612903d), Dates.dateDif(start, end, "YMF"));
        assertEquals(Double.valueOf(10.967741935483872), Dates.dateDif(start, getDate(27, 2, 2013), "MF"));
        assertEquals(Double.valueOf(2.8356164383561646), Dates.dateDif(start, getDate(27, 1, 2015), "YF"));
    }

    /** The days after the last anniversary that hold the 29th of February are a fraction of 366 days. */
    @Test
    void test_dateDif_yearFractionAcrossTheTwentyNinthOfFebruary() {
        final Date start = getDate(19, 7, 2022);

        assertEquals(Double.valueOf(2), Dates.dateDif(start, getDate(19, 7, 2024), "YF"));
        assertEquals(Double.valueOf(1 + 365d / 366), Dates.dateDif(start, getDate(18, 7, 2024), "YF"));
        assertEquals(Double.valueOf(364d / 365), Dates.dateDif(start, getDate(18, 7, 2023), "YF"));
        assertEquals(Double.valueOf(365d / 366), Dates.dateDif(getDate(1, 3, 2023), getDate(29, 2, 2024), "YF"));
        // the 29th of February comes back on the 1st of March of a year without it: no fifth year is complete yet
        assertEquals(Double.valueOf(4 + 365d / 366), Dates.dateDif(getDate(29, 2, 2020), getDate(28, 2, 2025), "YF"));
        assertEquals(Double.valueOf(5), Dates.dateDif(getDate(29, 2, 2020), getDate(1, 3, 2025), "YF"));
        // before 1582 the calendar is Julian: 1500 is a leap year
        assertEquals(Double.valueOf(365d / 366), Dates.dateDif(getDate(1, 1, 1500), getDate(31, 12, 1500), "YF"));
        assertEquals(Double.valueOf(-1 - 365d / 366), Dates.dateDif(getDate(18, 7, 2024), start, "YF"));
    }

    /** The days after the last anniversary in one calendar year are a fraction of its length by the calendar. */
    @Test
    void test_dateDif_yearFractionInOneHistoricalYear() {
        // 1582 has 355 days: the Gregorian calendar skips 10 days of October
        assertEquals(Double.valueOf(354d / 355), Dates.dateDif(getDate(1, 1, 1582), getDate(31, 12, 1582), "YF"));
        // 1 BC is a leap year of the Julian calendar
        assertEquals(Double.valueOf(365d / 366), Dates.dateDif(getDateBC(1, 1, 1), getDateBC(31, 12, 1), "YF"));
        assertEquals(Double.valueOf(364d / 365), Dates.dateDif(getDateBC(1, 1, 2), getDateBC(31, 12, 2), "YF"));
    }

    /**
     * For dates without a time of day, a period shorter than a year gives the value of Excel {@code YEARFRAC} with
     * basis 1, as computed in Microsoft Excel.
     *
     * <p>Days in one leap year or holding the 29th of February are a fraction of 366 days, other days of 365 days. A
     * 29th of February after the end date does not count.
     */
    @Test
    void test_dateDif_yearFractionAsExcelYearFracActualActual() {
        // no 29th of February, although the year up to the next anniversary holds one
        assertEquals(Double.valueOf(119d / 365), Dates.dateDif(getDate(2, 4, 2023), getDate(30, 7, 2023), "YF"));
        assertEquals(Double.valueOf(364d / 365), Dates.dateDif(getDate(1, 3, 2023), getDate(28, 2, 2024), "YF"));
        assertEquals(Double.valueOf(152d / 365), Dates.dateDif(getDate(1, 9, 2023), getDate(31, 1, 2024), "YF"));
        assertEquals(Double.valueOf(316d / 365), Dates.dateDif(getDate(10, 3, 2024), getDate(20, 1, 2025), "YF"));
        // inside one leap year
        assertEquals(Double.valueOf(142d / 366), Dates.dateDif(getDate(10, 3, 2024), getDate(30, 7, 2024), "YF"));
        assertEquals(Double.valueOf(41d / 366), Dates.dateDif(getDate(10, 1, 2024), getDate(20, 2, 2024), "YF"));
        // the 29th of February inside the period, as its end or as its start
        assertEquals(Double.valueOf(365d / 366), Dates.dateDif(getDate(19, 7, 2023), getDate(18, 7, 2024), "YF"));
        assertEquals(Double.valueOf(229d / 366), Dates.dateDif(getDate(30, 7, 2023), getDate(15, 3, 2024), "YF"));
        assertEquals(Double.valueOf(365d / 366), Dates.dateDif(getDate(1, 3, 2023), getDate(29, 2, 2024), "YF"));
        assertEquals(Double.valueOf(365d / 366), Dates.dateDif(getDate(29, 2, 2024), getDate(28, 2, 2025), "YF"));
        // the days after the last anniversary of a longer period follow the same rule
        assertEquals(Double.valueOf(1 + 119d / 365), Dates.dateDif(getDate(2, 4, 2022), getDate(30, 7, 2023), "YF"));
        assertEquals(Double.valueOf(1 + 31d / 366), Dates.dateDif(getDate(1, 3, 2023), getDate(1, 4, 2024), "YF"));
    }

    /** An end earlier in the day of an anniversary than the start counts that anniversary and no day after it. */
    @Test
    void test_dateDif_endEarlierInTheDayOfAnAnniversary() {
        final Date start = date(2023, 7, 19, 12, 0);
        final Date end = date(2024, 7, 19, 6, 0);

        assertEquals(Double.valueOf(1), Dates.dateDif(start, end, "Y"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(start, end, "YD"));
        assertEquals(Double.valueOf(1), Dates.dateDif(start, end, "YF"));
        assertEquals(Double.valueOf(-1), Dates.dateDif(end, start, "YF"));
        // the 29th of the month comes on the 1st of March of a year without the 29th of February
        final Date monthStart = date(2023, 1, 29, 12, 0);
        final Date monthEnd = date(2023, 3, 1, 6, 0);
        assertEquals(Double.valueOf(1), Dates.dateDif(monthStart, monthEnd, "M"));
        assertEquals(ZERO_DOUBLE, Dates.dateDif(monthStart, monthEnd, "MD"));
        assertEquals(Double.valueOf(1), Dates.dateDif(monthStart, monthEnd, "MF"));
        assertEquals(Double.valueOf(1), Dates.dateDif(monthStart, monthEnd, "YMF"));
    }

    @Test
    void test_dateDif_shouldReturnNegativeFractionalResult() {
        final Date start = getDate(28, 3, 2012);
        final Date end = getDate(29, 3, 2013);

        assertEquals(Double.valueOf(-52.285714285714285d), Dates.dateDif(end, start, "WF"));
        assertEquals(Double.valueOf(-12.03225806451613d), Dates.dateDif(end, start, "MF"));
        assertEquals(Double.valueOf(-1.0027397260273974d), Dates.dateDif(end, start, "YF"));
        assertEquals(Double.valueOf(-0.03225806451612903d), Dates.dateDif(end, start, "YMF"));
    }

    @Test
    void testSetters() {
        Date date = getDate(1, 1, 2022);
        assertNull(Dates.setDate(date, 2022, 22, 1));
        assertNull(Dates.setDate(date, -111, 1, 1));
        assertNull(Dates.setDate(date, 2022, 2, 222));
        assertNull(Dates.setTime(date, 2022, 2, 1));
        assertNull(Dates.setTime(date, 2, 222, 1));
        assertNull(Dates.setTime(date, 2, 2, 122));
        assertNull(Dates.setTime(date, 2, 2, -1));
        assertNull(Dates.setTime(date, 2, 2, 1, 1000));
        assertNull(Dates.setTime(date, 2, 2, 1, -1));
        assertEquals(new Date(122, 1, 3), Dates.setDate(date, 2022, 2, 3));
        assertEquals(new Date(122, 0, 1, 11, 11, 11), Dates.setTime(date, 11, 11, 11));
        var l = new Date(122, 0, 1, 11, 11, 11).getTime() + 11;
        assertEquals(l, Dates.setTime(date, 11, 11, 11, 11).getTime());
    }

    @Test
    void isLeap() {
        assertTrue(Dates.isLeap(Dates.Date(2016, 2, 29)));
        assertFalse(Dates.isLeap(Dates.Date(2017, 2, 28)));
        assertFalse(Dates.isLeap(Dates.Date(1900, 2, 28)));
        assertTrue(Dates.isLeap(Dates.Date(2000, 2, 28)));
    }

    private static Date getDate(int day, int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private static Date getDateBC(int day, int month, int year) {
        var calendar = new GregorianCalendar(year, month - 1, day);
        calendar.set(Calendar.ERA, GregorianCalendar.BC);
        return calendar.getTime();
    }

}
