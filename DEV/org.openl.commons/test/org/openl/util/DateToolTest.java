package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class DateToolTest {

    private static Locale locale;

    private static Calendar createCalendar(int day, int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
    @BeforeAll
    static void setup() {
        locale = Locale.getDefault();
        Locale.setDefault(Locale.US); // Fix weekOfYear test due it Depends on Locale
    }

    @AfterAll
    static void done() {
        Locale.setDefault(locale);
    }

    @Test
    public void testYearDiff() {
        assertNull(DateTool.yearDiff(null, null));
        assertNull(DateTool.yearDiff(new Date(), null));
        assertNull(DateTool.yearDiff(null, new Date()));

        Date startDate = createCalendar(5, 11, 2013).getTime();
        Date endDate = createCalendar(6, 12, 2015).getTime();

        assertEquals(Integer.valueOf(2), DateTool.yearDiff(endDate, startDate));
    }

    @Test
    public void test_absMonth_shouldReturnNull() {
        Integer actual = DateTool.absMonth(null);
        assertNull(actual);
    }

    @Test
    public void test_absMonth() {
        Date date = createCalendar(24, 9, 2015).getTime();
        int actual = DateTool.absMonth(date);
        assertEquals(24188, actual);
    }

    @Test
    public void test_absQuarter_shouldReturnNull() {
        Integer actual = DateTool.absQuarter(null);
        assertNull(actual);
    }

    @Test
    public void test_dayDiff_shouldReturnNull_whenFirstParameterIsNull() {
        Integer actual = DateTool.dayDiff(null, new Date());
        assertNull(actual);
    }

    @Test
    public void test_dayDiff_shouldReturnNull_whenSecondParameterIsNull() {
        Integer actual = DateTool.dayDiff(new Date(), null);
        assertNull(actual);
    }

    @Test
    public void test_dayOfWeek_shouldReturnNull() {
        Integer actual = DateTool.dayOfWeek(null);
        assertNull(actual);
    }

    @Test
    public void test_dayOfMonth_shouldReturnNull() {
        Integer actual = DateTool.dayOfMonth(null);
        assertNull(actual);
    }

    @Test
    public void test_dayOfYear_shouldReturnNull() {
        Integer actual = DateTool.dayOfYear(null);
        assertNull(actual);
    }

    @Test
    public void test_weekOfMonth_shouldReturnNull() {
        Integer actual = DateTool.weekOfMonth(null);
        assertNull(actual);
    }

    @Test
    public void test_weekOfYear_shouldReturnNull() {
        Integer actual = DateTool.weekOfYear(null);
        assertNull(actual);
    }

    @Test
    public void test_lastDayOfMonth_shouldReturnNull() {
        Integer actual = DateTool.lastDayOfMonth(null);
        assertNull(actual);
    }

    @Test
    public void test_monthDiff_shouldReturnNull_whenFirstParameterIsNull() {
        Integer actual = DateTool.monthDiff(null, new Date());
        assertNull(actual);
    }

    @Test
    public void test_monthDiff_shouldReturnNull_whenSecondParameterIsNull() {
        Integer actual = DateTool.monthDiff(new Date(), null);
        assertNull(actual);
    }

    @Test
    public void test_yearDiff_shouldReturnNull_whenFirstParameterIsNull() {
        Integer actual = DateTool.yearDiff(null, new Date());
        assertNull(actual);
    }

    @Test
    public void test_yearDiff_shouldReturnNull_whenSecondParameterIsNull() {
        Integer actual = DateTool.yearDiff(new Date(), null);
        assertNull(actual);
    }

    @Test
    public void test_weekDiff_shouldReturnNull_whenFirstParameterIsNull() {
        Integer actual = DateTool.weekDiff(null, new Date());
        assertNull(actual);
    }

    @Test
    public void test_weekDiff_shouldReturnNull_whenSecondParameterIsNull() {
        Integer actual = DateTool.weekDiff(new Date(), null);
        assertNull(actual);
    }

    @Test
    public void test_quarter_shouldReturnNull() {
        Integer actual = DateTool.quarter(null);
        assertNull(actual);
    }

    @Test
    public void test_second_shouldReturnNull() {
        Integer actual = DateTool.second(null);
        assertNull(actual);
    }

    @Test
    public void test_second_shouldReturnSeconds() {
        Calendar calendar = createCalendar(15, 6, 2018);
        for (int seconds = 0; seconds < 60; seconds++) {
            calendar.set(Calendar.SECOND, seconds);
            int actual = DateTool.second(calendar.getTime());
            assertEquals(seconds, actual);
        }
    }

    @Test
    public void test_minute_shouldReturnNull() {
        Integer actual = DateTool.minute(null);
        assertNull(actual);
    }

    @Test
    public void test_minute_shouldReturnMinutes() {
        Calendar calendar = createCalendar(15, 6, 2018);
        for (int minutes = 0; minutes < 60; minutes++) {
            calendar.set(Calendar.MINUTE, minutes);
            int actual = DateTool.minute(calendar.getTime());
            assertEquals(minutes, actual);
        }
    }

    @Test
    public void test_hour_shouldReturnNull() {
        Integer actual = DateTool.hour(null);
        assertNull(actual);
    }

    @Test
    public void test_hour_shouldReturnHourBetween0and11_whenTimeBetween0and11() {
        Calendar calendar = createCalendar(15, 6, 2018);
        for (int hour = 0; hour < 12; hour++) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            int actual = DateTool.hour(calendar.getTime());
            assertEquals(hour, actual);
        }
    }

    @Test
    public void test_hour_shouldReturnHourBetween0and11_whenTimeBetween12and23() {
        Calendar calendar = createCalendar(15, 6, 2018);
        for (int hour = 12; hour < 23; hour++) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            int actual = DateTool.hour(calendar.getTime());
            int expected = hour - 12;
            assertEquals(expected, actual);
        }
    }

    @Test
    public void test_hourOfDay_shouldReturnNull() {
        Integer actual = DateTool.hourOfDay(null);
        assertNull(actual);
    }

    @Test
    public void test_hourOfDay_shouldReturnHourBetween0and11_whenTimeBetween0and11() {
        Calendar calendar = createCalendar(15, 6, 2018);
        for (int hour = 0; hour < 12; hour++) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            int actual = DateTool.hourOfDay(calendar.getTime());
            assertEquals(hour, actual);
        }
    }

    @Test
    public void test_hourOfDay_shouldReturnHourBetween0and11_whenTimeBetween12and23() {
        Calendar calendar = createCalendar(15, 6, 2018);
        for (int hour = 12; hour < 23; hour++) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            int actual = DateTool.hourOfDay(calendar.getTime());
            assertEquals(hour, actual);
        }
    }

    @Test
    public void test_month_shouldReturnNull() {
        Integer actual = DateTool.month(null);
        assertNull(actual);
    }

    @Test
    public void test_year_shouldReturnNull() {
        Integer actual = DateTool.year(null);
        assertNull(actual);
    }

    @Test
    public void test_amPm_shouldReturnNull() {
        String actual = DateTool.amPm(null);
        assertNull(actual);
    }

    @Test
    public void test_amPm_shouldReturnAM_whenTimeBetween0and11() {
        Calendar calendar = createCalendar(15, 6, 2018);
        for (int hour = 0; hour < 12; hour++) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            String actual = DateTool.amPm(calendar.getTime());
            assertEquals("AM", actual);
        }
    }

    @Test
    public void test_amPm_shouldReturnPM_whenTimeBetween12and23() {
        Calendar calendar = createCalendar(15, 6, 2018);
        for (int hour = 12; hour < 24; hour++) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            String actual = DateTool.amPm(calendar.getTime());
            assertEquals("PM", actual);
        }
    }

    @Test
    public void test_dateToString_shouldReturnNull_whenDateIsNull() {
        String actual = DateTool.dateToString(null, "dd/MM/yyyy");
        assertNull(actual);
    }

    @Test
    public void test_dateToString_shouldFormatUsingCustomDatePattern() {
        Date date = createCalendar(11, 12, 2013).getTime();
        String actual = DateTool.dateToString(date, "dd MMM yyyy");
        assertEquals("11 Dec 2013", actual);
    }

    @Test
    public void dateToString_shouldReturnNull_whenDateIsNull() {
        String actual = DateTool.dateToString(null);
        assertNull(actual);
    }

    @Test
    public void test_dateToString_shouldFormatUsingShortDatePattern() {
        Date date = createCalendar(11, 12, 2013).getTime();
        String actual = DateTool.dateToString(date);
        assertEquals("12/11/13", actual);
    }

    @CsvSource({ "2015-05-14,2015",
            "2015-12-31,2015",
            "1789-05-14,1789",
            "0001-01-01,1",
            "1900-01-01,1900",
            "1899-02-28,1899",
            "3892-05-14,3892" })
    @ParameterizedTest(name = "{index}: DateTool.year({0})={1}")
    public void test_year(String input, int expected) throws ParseException {
        var inputDate = new SimpleDateFormat("yyyy-MM-dd").parse(input);
        int actual = DateTool.year(inputDate);
        assertEquals(expected, actual);
    }

    @CsvSource({ "2015-05-14,5",
            "2015-12-31,12",
            "1789-06-14,6",
            "0001-01-01,1",
            "1900-03-01,3",
            "1899-02-28,2",
            "3892-07-14,7" })
    @ParameterizedTest(name = "{index}: DateTool.month({0})={1}")
    public void test_month(String input, int expected) throws ParseException {
        var inputDate = new SimpleDateFormat("yyyy-MM-dd").parse(input);
        int actual = DateTool.month(inputDate);
        assertEquals(expected, actual);
    }

    @CsvSource({ "2015-05-14,14", "2015-12-31,31", "1788-02-29,29", "0001-01-01,1", "1900-04-30,30" })
    @ParameterizedTest(name = "{index}: DateTool.dayOfMonth({0})={1}")
    public void test_dayOfMonth(String input, int expected) throws ParseException {
        var inputDate = new SimpleDateFormat("yyyy-MM-dd").parse(input);
        int actual = DateTool.dayOfMonth(inputDate);
        assertEquals(expected, actual);
    }

    @CsvSource({ "2015-05-14,134",
            "2015-12-31,365",
            "2015-12-30,364",
            "2015-02-28,59",
            "2015-03-01,60",
            "2000-12-30,365",
            "2000-12-31,366",
            "2000-02-28,59",
            "2000-02-29,60",
            "2000-03-01,61" })
    @ParameterizedTest(name = "{index}: DateTool.dayOfYear({0})={1}")
    public void test_dayOfYear(String input, int expected) throws ParseException {
        var inputDate = new SimpleDateFormat("yyyy-MM-dd").parse(input);
        int actual = DateTool.dayOfYear(inputDate);
        assertEquals(expected, actual);
    }

    @CsvSource({ "2018-01-01,0",
            "2018-03-31,0",
            "2018-04-01,1",
            "2018-06-30,1",
            "2018-07-01,2",
            "2018-09-30,2",
            "2018-10-01,3",
            "2018-12-31,3" })
    @ParameterizedTest(name = "{index}: DateTool.quarter({0})={1}")
    public void test_quarter(String input, int expected) throws ParseException {
        var inputDate = new SimpleDateFormat("yyyy-MM-dd").parse(input);
        int actual = DateTool.quarter(inputDate);
        assertEquals(expected, actual);
    }

    @CsvSource({ "2015-05-14,5",
            "2015-12-30,4",
            "2015-02-28,7",
            "2015-03-01,1",
            "2000-12-31,1",
            "2000-02-28,2",
            "2000-02-29,3",
            "2000-03-01,4" })
    @ParameterizedTest(name = "{index}: DateTool.dayOfWeek({0})={1}")
    public void test_dayOfWeek2(String input, int expected) throws ParseException {
        var inputDate = new SimpleDateFormat("yyyy-MM-dd").parse(input);
        int actual = DateTool.dayOfWeek(inputDate);
        assertEquals(expected, actual);
    }

    @CsvSource({ "2015-05-14,20",
            "2015-12-31,1",
            "1789-05-14,20",
            "0001-01-01,1",
            "1899-02-28,9",
            "3892-05-14,20",
            "2013-12-28,52" })
    @ParameterizedTest(name = "{index}: DateTool.weekOfYear({0})={1}")
    public void test_weekOfYear(String input, int expected) throws ParseException {
        var inputDate = new SimpleDateFormat("yyyy-MM-dd").parse(input);
        int actual = DateTool.weekOfYear(inputDate);
        assertEquals(expected, actual);
    }

    @CsvSource({ "2015-05-14, 3", "2015-12-31, 5", "1789-05-14, 3", "0001-01-01, 1", "1899-02-28, 5", "3892-05-14, 2" })
    @ParameterizedTest(name = "{index}: DateTool.weekOfYear({0})={1}")
    public void test_weekOfMonth(String input, int expected) throws ParseException {
        var inputDate = new SimpleDateFormat("yyyy-MM-dd").parse(input);
        int actual = DateTool.weekOfMonth(inputDate);
        assertEquals(expected, actual);
    }

    @CsvSource({ "8072,2018-01-01,2018-03-31",
            "8073,2018-04-01,2018-06-30",
            "8074,2018-07-01,2018-09-30",
            "8075,2018-10-01,2018-12-31" })
    @ParameterizedTest(name = "{index}: When Quarter={0} between [{1}, {2}]")
    public void test_absQuarter_whenLastDateOfQuarter(Integer quarter, String d1, String d2) throws ParseException {
        var firstDate = new SimpleDateFormat("yyyy-MM-dd").parse(d1);
        var lastDate = new SimpleDateFormat("yyyy-MM-dd").parse(d2);
        assertEquals(firstDate, DateTool.firstDateOfQuarter(quarter));
        assertEquals(quarter, DateTool.absQuarter(firstDate));
        assertEquals(lastDate, DateTool.lastDateOfQuarter(quarter));
        assertEquals(quarter, DateTool.absQuarter(lastDate));
    }

    @ParameterizedTest(name = "{index}: DateTool.lastDayOfMonth({0})={1}")
    @CsvSource({ "1900-01-02,31", "2015-05-14,31", "2015-12-30,31", "2015-02-28,28", "2000-12-4,31", "2015-04-30,30" })
    public void test_dayOfWeek(String input, int expected) throws ParseException {
        var inputDate = new SimpleDateFormat("yyyy-MM-dd").parse(input);
        int actual = DateTool.lastDayOfMonth(inputDate);
        assertEquals(expected, actual);
    }
}
