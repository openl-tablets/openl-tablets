package org.openl.rules.util.dates;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Wrapper for {@code Date} object to simplify calculation between two dates in days, months or years.
 *
 * @author Vladylav Pikus
 */
final class CalendarWrapper {

    private static final long MILLS_IN_HOUR = 1000L * 60 * 60;
    private static final long MILLS_IN_DAY = MILLS_IN_HOUR * 24;

    static final int DAYS_IN_WEEK = 7;
    static final int MONTH_IN_YEAR = 12;
    private static final int DAYS_IN_YEAR = 365;
    private static final int DAYS_IN_LEAP_YEAR = 366;

    private final GregorianCalendar calendar;
    private final int year;
    private final int month;
    @Getter(AccessLevel.PACKAGE)
    private final int day;

    CalendarWrapper(Date date) {
        this.calendar = new GregorianCalendar();
        this.calendar.setTime(date);

        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH) + 1;
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    CalendarWrapper(Calendar calendar) {
        this.calendar = (GregorianCalendar) calendar;
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH) + 1;
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    private long getTimeInMillis() {
        return calendar.getTimeInMillis() + calendar.get(Calendar.DST_OFFSET) + calendar.get(Calendar.ZONE_OFFSET);
    }

    /**
     * Make a full copy of the calendar
     *
     * @return full copy of the calendar
     */
    private GregorianCalendar getCalendar() {
        return (GregorianCalendar) calendar.clone();
    }

    /**
     * Calculate difference in full days between two dates
     *
     * @param start start date
     * @return difference in full days
     */
    int daysDiff(CalendarWrapper start) {
        var estDiff = (int) ((getTimeInMillis() - start.getTimeInMillis()) / MILLS_IN_DAY);
        var startCalendar = start.getCalendar();
        startCalendar.add(Calendar.DAY_OF_MONTH, estDiff - 2);
        for (var i = estDiff - 1; ; i++) {
            startCalendar.add(Calendar.DAY_OF_MONTH, 1);
            if (startCalendar.after(calendar)) {
                return i - 1;
            }
        }
    }

    /**
     * Calculate difference in full months between two dates
     *
     * @param start start date
     * @return difference in full months
     */
    int monthsDiff(CalendarWrapper start) {
        var monthsDiff = month - start.month + MONTH_IN_YEAR * (year - start.year);
        if (start.day > day) {
            monthsDiff--; // because the last month wasn't completed
        }
        return monthsDiff;
    }

    /**
     * Calculate difference in full years between two dates.
     *
     * @param start start date
     * @return difference in full years
     */
    int yearsDiff(CalendarWrapper start) {
        var yearsDiff = year - start.year; // diff in complete years
        if (!isCompleteLastYear(start)) {
            yearsDiff -= 1; // because the last year wasn't completed
        }
        return yearsDiff;
    }

    /**
     * Calculate difference in full days between two dates regarding last month. This method skips years and months.
     * <p>
     * Note: this is an analog for Excel native function DATEDIF(start, end, "MD")
     *
     * @param start start date
     * @return difference in full days
     */
    int daysDiffExcludeYearsAndMonths(CalendarWrapper start) {
        int daysWithoutMonthDiff;
        if (start.day <= day) {
            daysWithoutMonthDiff = day - start.day;
        } else {
            // decrement last month from start date
            // and set year as in end date
            var calendarStartDate = start.getCalendar();
            setPrevMonth(calendarStartDate);
            var startDate = new CalendarWrapper(calendarStartDate);

            if (month == startDate.month && day < startDate.day) {
                return 0; // return zero because may be produced negative result for some dates: 1/31/2017 - 3/1/2017
            }
            daysWithoutMonthDiff = daysDiff(startDate);
        }
        return daysWithoutMonthDiff;
    }

    /**
     * Checks if the last year was complete regarding day of month
     *
     * @param start start date
     * @return {@code true} if was complete, otherwise {@code false}
     */
    private boolean isCompleteLastYear(CalendarWrapper start) {
        return month > start.month || (month == start.month && day >= start.day);
    }

    /**
     * Calculate difference in full days between two dates regarding last year. This method skips years.
     * <p>
     * Note: this is an analog for Excel native function DATEDIF(start, end, "YD")
     *
     * @param start start date
     * @return difference in full days
     */
    int daysDiffExcludeYears(CalendarWrapper start) {
        return daysDiff(lastAnniversary(start));
    }

    /**
     * The last anniversary of the start date on or before this date.
     *
     * <p>A start on the 29th of February has its anniversary on the 1st of March of a year without that day.
     *
     * @param start start date
     * @return the start date moved to its last anniversary
     */
    private CalendarWrapper lastAnniversary(CalendarWrapper start) {
        var anniversary = start.getCalendar();
        anniversary.set(Calendar.YEAR, isCompleteLastYear(start) ? year : year - 1);
        return new CalendarWrapper(anniversary);
    }

    int getActualMonthLength() {
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    int getPrevMonthLength() {
        var prevMonthCalendar = getCalendar();
        prevMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
        setPrevMonth(prevMonthCalendar);
        return prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void setPrevMonth(Calendar calendar) {
        if (month == 1) {
            calendar.set(Calendar.YEAR, year - 1);
            calendar.set(Calendar.MONTH, 11);
        } else {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 2);
        }
    }

    /**
     * The length of the year that the days after the last anniversary of the start date are a fraction of. It is the
     * year length Excel {@code YEARFRAC} with basis 1 (actual/actual) takes for those days.
     *
     * <p>When those days lie in one calendar year, it is the length of that year: 366 days in a leap year. When they
     * run into the next year, it is 366 days if they hold the 29th of February, the last anniversary and this date
     * included, and 365 days if not.
     *
     * @param start start date
     * @return the length of the year in days
     */
    int lastYearLength(CalendarWrapper start) {
        if (isCompleteLastYear(start)) {
            return yearLength(); // the last anniversary is in the year of this date
        }
        var anniversary = lastAnniversary(start);
        var leapDayAfterAnniversary = anniversary.month <= 2 && anniversary.isLeapYear();
        var leapDayUpToThisDate = (month > 2 || (month == 2 && day == 29)) && isLeapYear();
        return leapDayAfterAnniversary || leapDayUpToThisDate ? DAYS_IN_LEAP_YEAR : DAYS_IN_YEAR;
    }

    /** The number of days in the calendar year of this date, by the calendar the days are counted with. */
    private int yearLength() {
        return calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    private boolean isLeapYear() {
        return yearLength() == DAYS_IN_LEAP_YEAR;
    }
}
