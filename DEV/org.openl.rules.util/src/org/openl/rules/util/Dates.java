package org.openl.rules.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.openl.rules.util.dates.DateInterval;
import org.openl.rules.util.dates.DateInterval.Scale;
import org.openl.rules.util.dates.DateInterval.Unit;

/**
 * A set of util methods to work with dates.
 *
 * Note: Days and months begin from 1 (not like in Java from 0). Years begin from 0000.
 * Note: For OpenL rules only! Don't use it in Java code.
 *
 * @author  Yury Molchan, Vladyslav Pikus
 */
public final class Dates {

    /**
     * Converts a string to a date using a pattern.
     *
     * @see SimpleDateFormat
     */
    public static Date date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setLenient(false); // Strict matching
        return calendar.getTime();
    }

    /**
     * Converts a date to a string using a default pattern. The default pattern is system and setting dependent.
     */
    public static String toString(Date date) {
        return toString(date, "MM/dd/yyyy");
    }

    /**
     * Converts a date to a string using a pattern.
     *
     * @see SimpleDateFormat
     */
    public static String toString(Date date, String pattern) {
        return date == null ? null : getDateFormat(pattern).format(date);
    }

    /**
     * Converts a string to a date using a default pattern. The default pattern is system and setting dependent.
     */
    public static Date toDate(String str) throws ParseException {
        return toDate(str, "MM/dd/yy");
    }

    /**
     * Converts a string to a date using a pattern.
     * 
     * @see SimpleDateFormat
     */
    public static Date toDate(String str, String pattern) throws ParseException {
        return (str == null || str.trim().isEmpty()) ? null : getDateFormat(pattern).parse(str);
    }

    private static DateFormat getDateFormat(String pattern) {
        DateFormat df = new SimpleDateFormat(pattern == null || pattern.trim().isEmpty() ? "MM/dd/yyyy" : pattern, Locale.US);
        df.setLenient(false); // Strict matching
        df.getCalendar().set(0, 0, 0, 0, 0, 0); // at
        df.getCalendar().set(Calendar.MILLISECOND, 0);
        return df;
    }

    /**
     * Calculate difference between two dates
     *
     * @param startDate start date
     * @param endDate end date
     * @param unitName method type
     * @return difference between two dates
     */
    public static Double dateDif(Date startDate, Date endDate, String unitName) {
        Unit unit = Unit.getUnit(unitName);
        if (unit == null) {
            throw new IllegalArgumentException("Unsupported unit '" + unitName + "'");
        }
        DateInterval interval = DateInterval.between(startDate, endDate);
        switch (unit) {
            case DAYS:
                return interval.toDays();
            case WEEKS:
                return interval.toWeeks(Scale.INT);
            case MONTHS:
                return interval.toMonths(Scale.INT);
            case YEARS:
                return interval.toYears(Scale.INT);
            case DAYS_EXCLUDE_MONTHS_AND_YEARS:
                return interval.toDaysExcludeYearsAndMonths();
            case MONTHS_EXCLUDE_YEARS:
                return interval.toMonthsExcludeYears(Scale.INT);
            case DAYS_EXCLUDE_YEARS:
                return interval.toDaysExcludeYears();
            // calculate fractional difference between dates
            case WEEKS_FRACTIONAL:
                return interval.toWeeks(Scale.FRAC);
            case MONTHS_FRACTIONAL:
                return interval.toMonths(Scale.FRAC);
            case YEARS_FRACTIONAL:
                return interval.toYears(Scale.FRAC);
            case MONTHS_FRACTIONAL_EXCLUDE_YEARS:
                return interval.toMonthsExcludeYears(Scale.FRAC);
            default:
                // should never be happened
                throw new IllegalArgumentException("Unsupported unit '" + unitName + "'");
        }
    }
}
