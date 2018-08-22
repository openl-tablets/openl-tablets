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
public class Dates {

    /**
     * Converts a date to a string using a default pattern. The default pattern is system and setting dependent.
     */
    public static String toString(Date date) {
        return toString(date, null);
    }

    /**
     * Converts a date to a string using a pattern.
     *
     * @see SimpleDateFormat
     */
    public static String toString(Date date, String pattern) {
        return date == null ? null
                            : new SimpleDateFormat(pattern == null ? "MM/dd/yyyy" : pattern, Locale.US).format(date);
    }

    /**
     * Converts a string to a date using a default pattern. The default pattern is system and setting dependent.
     */
    public static Date toDate(String str) throws ParseException {
        return (str == null || str.trim().isEmpty()) ? null : getDateFormat().parse(str);
    }

    private static DateFormat getDateFormat() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
        df.setLenient(false); // Strict matching
        df.getCalendar().set(0, 0, 0, 0, 0, 0); // at
        df.getCalendar().set(Calendar.MILLISECOND, 0);
        return df;
    }
}
