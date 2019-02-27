package org.openl.rules.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A set of util methods to work with Date.
 *
 * Note: Days and months begin from 1 (not like in Java from 0). Years begin from 0000.
 *
 *
 * Note: For OpenL rules only! Don't use it in Java code.
 *
 * @author Yury Molchan
 */
public final class Dates {

    private Dates() {
        // Utility class
    }

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

    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
