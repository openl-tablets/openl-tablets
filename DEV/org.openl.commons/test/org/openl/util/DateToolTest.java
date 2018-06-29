package org.openl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Created by dl on 7/8/14.
 */
@RunWith(Enclosed.class)
public class DateToolTest {

    private static Calendar createCalendar(int day, int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static class DateToolSimpleTest {

        @Test
        public void testYearDiff() {
            assertEquals(new Integer(0), DateTool.yearDiff(null, null));
            assertNull(DateTool.yearDiff(new Date(), null));
            assertNull(DateTool.yearDiff(null, new Date()));

            Date startDate = createCalendar(5, 11, 2013).getTime();
            Date endDate = createCalendar(6, 12, 2015).getTime();

            assertEquals(new Integer(2), DateTool.yearDiff(endDate, startDate));
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
    }

    @RunWith(Parameterized.class)
    public static class DateToolYearMethodTest {

        @Parameters(name = "{index}: DateTool.year({0})={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] { { "5/14/2015", 2015 },
                    { "12/31/2015", 2015 },
                    { "5/14/1789", 1789 },
                    { "1/1/0001", 1 },
                    { "1/1/1900", 1900 },
                    { "2/28/1899", 1899 },
                    { "5/14/3892", 3892 } });
        }

        private final Date inputDate;
        private final int expected;

        public DateToolYearMethodTest(String input, int expected) throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            this.inputDate = format.parse(input);
            this.expected = expected;
        }

        @Test
        public void test_year() {
            int actual = DateTool.year(inputDate);
            assertEquals(expected, actual);
        }

    }

    @RunWith(Parameterized.class)
    public static class DateToolMonthMethodTest {

        @Parameters(name = "{index}: DateTool.month({0})={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] { { "5/14/2015", 5 },
                    { "12/31/2015", 12 },
                    { "6/14/1789", 6 },
                    { "1/1/0001", 1 },
                    { "3/1/1900", 3 },
                    { "2/28/1899", 2 },
                    { "7/14/3892", 7 } });
        }

        private final Date inputDate;
        private final int expected;

        public DateToolMonthMethodTest(String input, int expected) throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            this.inputDate = format.parse(input);
            this.expected = expected;
        }

        @Test
        public void test_month() {
            int actual = DateTool.month(inputDate);
            assertEquals(expected, actual);
        }

    }

    @RunWith(Parameterized.class)
    public static class DateToolDayOfMonthMethodTest {

        @Parameters(name = "{index}: DateTool.dayOfMonth({0})={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] { { "5/14/2015",
                    14 }, { "12/31/2015", 31 }, { "2/29/1788", 29 }, { "1/1/0001", 1 }, { "4/30/1900", 30 }, });
        }

        private final Date inputDate;
        private final int expected;

        public DateToolDayOfMonthMethodTest(String input, int expected) throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            this.inputDate = format.parse(input);
            this.expected = expected;
        }

        @Test
        public void test_dayOfMonth() {
            int actual = DateTool.dayOfMonth(inputDate);
            assertEquals(expected, actual);
        }

    }

    @RunWith(Parameterized.class)
    public static class DateToolDayOfYearMethodTest {

        @Parameters(name = "{index}: DateTool.dayOfYear({0})={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] { { "5/14/2015", 134 },
                    { "12/31/2015", 365 },
                    { "12/30/2015", 364 },
                    { "2/28/2015", 59 },
                    { "3/1/2015", 60 },
                    { "12/30/2000", 365 },
                    { "12/31/2000", 366 },
                    { "2/28/2000", 59 },
                    { "2/29/2000", 60 },
                    { "3/1/2000", 61 }, });
        }

        private final Date inputDate;
        private final int expected;

        public DateToolDayOfYearMethodTest(String input, int expected) throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            this.inputDate = format.parse(input);
            this.expected = expected;
        }

        @Test
        public void test_dayOfYear() {
            int actual = DateTool.dayOfYear(inputDate);
            assertEquals(expected, actual);
        }

    }

    @RunWith(Parameterized.class)
    public static class DateToolQuarterMethodTest {

        @Parameters(name = "{index}: DateTool.quarter({0})={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] { { "1/1/2018", 0 },
                    { "3/31/2018", 0 },
                    { "4/1/2018", 1 },
                    { "6/30/2018", 1 },
                    { "7/1/2018", 2 },
                    { "9/30/2018", 2 },
                    { "10/1/2018", 3 },
                    { "12/31/2018", 3 }, });
        }

        private final Date inputDate;
        private final int expected;

        public DateToolQuarterMethodTest(String input, int expected) throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            this.inputDate = format.parse(input);
            this.expected = expected;
        }

        @Test
        public void test_quarter() {
            int actual = DateTool.quarter(inputDate);
            assertEquals(expected, actual);
        }

    }

    @RunWith(Parameterized.class)
    public static class DateToolDayOfWeekMethodTest {

        @Parameters(name = "{index}: DateTool.dayOfWeek({0})={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] { { "5/14/2015", 5 },
                    { "12/30/2015", 4 },
                    { "2/28/2015", 7 },
                    { "3/1/2015", 1 },
                    { "12/31/2000", 1 },
                    { "2/28/2000", 2 },
                    { "2/29/2000", 3 },
                    { "3/1/2000", 4 } });
        }

        private final Date inputDate;
        private final int expected;

        public DateToolDayOfWeekMethodTest(String input, int expected) throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            this.inputDate = format.parse(input);
            this.expected = expected;
        }

        @Test
        public void test_dayOfWeek() {
            int actual = DateTool.dayOfWeek(inputDate);
            assertEquals(expected, actual);
        }

    }

    @RunWith(Parameterized.class)
    public static class DateToolWeekOfYearMethodTest {

        @Parameters(name = "{index}: DateTool.weekOfYear({0})={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] { { "5/14/2015", 20 },
                    { "12/31/2015", 1 },
                    { "5/14/1789", 20 },
                    { "1/1/0001", 1 },
                    { "2/28/1899", 9 },
                    { "5/14/3892", 20 },
                    { "12/28/2013", 52}});
        }

        private final Date inputDate;
        private final int expected;

        public DateToolWeekOfYearMethodTest(String input, int expected) throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            this.inputDate = format.parse(input);
            this.expected = expected;
        }

        @Test
        public void test_weekOfYear() {
            int actual = DateTool.weekOfYear(inputDate);
            assertEquals(expected, actual);
        }

    }

    @RunWith(Parameterized.class)
    public static class DateToolWeekOfMonthMethodTest {

        @Parameters(name = "{index}: DateTool.weekOfYear({0})={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] { { "5/14/2015", 3 },
                    { "12/31/2015", 5 },
                    { "5/14/1789", 3 },
                    { "1/1/0001", 1 },
                    { "2/28/1899", 5 },
                    { "5/14/3892", 2 } });
        }

        private final Date inputDate;
        private final int expected;

        public DateToolWeekOfMonthMethodTest(String input, int expected) throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            this.inputDate = format.parse(input);
            this.expected = expected;
        }

        @Before
        public void before() {
            Locale.setDefault(Locale.US);
        }
        
        @Test
        public void test_weekOfMonth() {
            int actual = DateTool.weekOfMonth(inputDate);
            assertEquals(expected, actual);
        }

    }

    @RunWith(Parameterized.class)
    public static class DateToolFirstAndLastDaysOfQuarterMethodTest {

        @Parameters(name = "{index}: When Quarter={0} between [{1}, {2}]")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] { { 8072, "1/1/2018", "3/31/2018" },
                    { 8073, "4/1/2018", "6/30/2018" },
                    { 8074, "7/1/2018", "9/30/2018" },
                    { 8075, "10/1/2018", "12/31/2018" }, });
        }

        private final Date firstDate;
        private final Date lastDate;
        private final int quarter;

        public DateToolFirstAndLastDaysOfQuarterMethodTest(int quarter,
                String firstDate,
                String lastDate) throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            this.firstDate = format.parse(firstDate);
            this.lastDate = format.parse(lastDate);
            this.quarter = quarter;
        }

        @Test
        public void test_firstDateOfQuarter() {
            Date actual = DateTool.firstDateOfQuarter(quarter);
            assertEquals(firstDate, actual);
        }

        @Test
        public void test_absQuarter_whenFirstDateOfQuarter() {
            int actual = DateTool.absQuarter(firstDate);
            assertEquals(quarter, actual);
        }

        @Test
        public void test_lastDateOfQuarter() {
            Date actual = DateTool.lastDateOfQuarter(quarter);
            assertEquals(lastDate, actual);
        }

        @Test
        public void test_absQuarter_whenLastDateOfQuarter() {
            int actual = DateTool.absQuarter(lastDate);
            assertEquals(quarter, actual);
        }

    }

    @RunWith(Parameterized.class)
    public static class DateToolLastDayOfMonthMethodTest {

        @Parameters(name = "{index}: DateTool.lastDayOfMonth({0})={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] { { "01/02/1900", 31 },
                    { "5/14/2015", 31 },
                    { "12/30/2015", 31 },
                    { "2/28/2015", 28 },
                    { "12/4/2000", 31 },
                    { "4/30/2015", 30 } });
        }

        private final Date inputDate;
        private final int expected;

        public DateToolLastDayOfMonthMethodTest(String input, int expected) throws ParseException {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            this.inputDate = format.parse(input);
            this.expected = expected;
        }

        @Test
        public void test_dayOfWeek() {
            int actual = DateTool.lastDayOfMonth(inputDate);
            assertEquals(expected, actual);
        }

    }
}
