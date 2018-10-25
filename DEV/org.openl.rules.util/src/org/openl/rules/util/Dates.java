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
     * Creates Date object using human numbers for the year and the month. Also validates correctness of the date.
     *
     * @see SimpleDateFormat
     */
    public static Date Date(int year, int month, int day) {
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
    public static Date toDate(String str) {
        return toDate(str, "MM/dd/yy");
    }

    /**
     * Converts a string to a date using a pattern.
     * 
     * @see SimpleDateFormat
     */
    public static Date toDate(String str, String pattern) {
        return isEmpty(str) ? null : parse(str, pattern);
    }

    /**
     * Checks if the Object is valid date. <br/>
     * <br/>
     * Strings will be tested using following pattern {@code "MM/dd/yyyy"}.<br/>
     * <br/>
     * <code>
     *     isDate(null)         = false<br/>
     *     isDate("")           = false<br/>
     *     isDate("  ")         = false<br/>
     *     isDate(new Date())   = true<br/>
     *     isDate("10/24/2018") = true<br/>
     *     isDate("10.24.2018") = false<br/>
     * </code>
     *
     * @param source any object
     * @return {@code true} if the Object is correctly formatted date
     */
    public static boolean isDate(Object source) {
        return source instanceof Date || source instanceof String && isDate((String) source, null);
    }

    /**
     * Checks if the Object is valid date by pattern. <br/>
     * <br/>
     * If the Pattern is not passed, strings will be tested using following pattern {@code "MM/dd/yyyy"}.<br/>
     * <br/>
     * <code>
     *     isDate(null, null)                 = false<br/>
     *     isDate(null, "dd/MM/yyyy")         = false<br/>
     *     isDate("", "dd/MM/yyyy")           = false<br/>
     *     isDate("  ", "dd/MM/yyyy")         = false<br/>
     *     isDate("10.24.2018", "dd.MM.yyyy") = true<br/>
     *     isDate("10.24.2018", "dd/MM./yyy") = false<br/>
     * </code>
     *
     * @param str any String
     * @param pattern any valid date patten like {@code "MM/dd/yyyy"}
     * @return {@code true} if the Object is correctly formatted date
     */
    public static boolean isDate(String str, String pattern) {
        if (isEmpty(str)) {
            return false;
        }
        Date parse = parse(str, pattern);
        return parse != null;
    }

    private static Date parse(String str, String pattern) {
        try {
            return getDateFormat(pattern).parse(str);
        } catch (ParseException e) {
            // Return null for non-parsable strings
            return null;
        }
    }

    private static DateFormat getDateFormat(String pattern) {
        DateFormat df = new SimpleDateFormat(isEmpty(pattern) ? "MM/dd/yyyy" : pattern, Locale.US);
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

    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
