package org.openl.rules.util.dates;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Wrapper for {@code Date} object to simplify calculation between two dates in days, months or years.
 *
 * @author Vladylav Pikus
 */
final class CalendarWrapper {

    private static final long MILLS_IN_HOUR = 1000 * 60 * 60;
    private static final long MILLS_IN_DAY = MILLS_IN_HOUR * 24;

    static final int DAYS_IN_WEEK = 7;
    static final int MONTH_IN_YEAR = 12;

    private final GregorianCalendar calendar;
    private final int year;
    private final int month;
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
        int estDiff = (int) ((getTimeInMillis() - start.getTimeInMillis()) / MILLS_IN_DAY);
        Calendar startCalendar = start.getCalendar();
        startCalendar.add(Calendar.DAY_OF_MONTH, estDiff - 2);
        for (int i = estDiff - 1;; i++) {
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
        int monthsDiff = month - start.month + MONTH_IN_YEAR * (year - start.year);
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
        int yearsDiff = year - start.year; // diff in complete years
        if (!isCompleteLastYear(start)) {
            yearsDiff -= 1; // because the last year wasn't completed
        }
        return yearsDiff;
    }

    /**
     * Calculate difference in full days between two dates regarding last month. This method skips years and months.
     *
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
            GregorianCalendar calendarStartDate = start.getCalendar();
            setPrevMonth(calendarStartDate);
            CalendarWrapper startDate = new CalendarWrapper(calendarStartDate);

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
     *
     * Note: this is an analog for Excel native function DATEDIF(start, end, "YD")
     *
     * @param start start date
     * @return difference in full days
     */
    int daysDiffExcludeYears(CalendarWrapper start) {
        GregorianCalendar calendarStartDate = start.getCalendar();
        if (isCompleteLastYear(start)) {
            calendarStartDate.set(Calendar.YEAR, year);
        } else {
            calendarStartDate.set(Calendar.YEAR, year - 1);
        }
        CalendarWrapper startDate = new CalendarWrapper(calendarStartDate);
        return daysDiff(startDate);
    }

    int getDay() {
        return day;
    }

    int getActualMonthLength() {
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    int getPrevMonthLength() {
        GregorianCalendar calendar = getCalendar();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        setPrevMonth(calendar);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
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

    int getUncompleteYearLength(CalendarWrapper start) {
        if (isCompleteLastYear(start)) {
            return calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
        }
        return getPrevYearLength();
    }

    private int getPrevYearLength() {
        GregorianCalendar calendar = getCalendar();
        calendar.set(Calendar.YEAR, year - 1);
        return calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
    }
}
