package org.openl.rules.ruleservice.databinding.util;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Extended implementation of {@link StdDateFormat}. Added an ability to use additional default date format.
 *
 * @author Vladyslav Pikus
 */
public class ExtendedStdDateFormat extends StdDateFormat {

    private static final String PARSE_ERROR_MSG = "Cannot parse date \"%s\": not compatible with any of standard forms (%s)";

    private final String pattern;
    private final SimpleDateFormat dateFormat;

    public ExtendedStdDateFormat(String pattern) {
        this.pattern = pattern;
        this._timezone = TimeZone.getDefault();
        this.dateFormat = new SimpleDateFormat(pattern);
    }

    private ExtendedStdDateFormat(String pattern,
            TimeZone tz,
            Locale loc,
            Boolean lenient,
            boolean formatTzOffsetWithColon) {

        super(tz, loc, lenient, formatTzOffsetWithColon);
        this.pattern = pattern;
        this.dateFormat = new SimpleDateFormat(pattern);
    }

    /**
     * Parse a string representation of date to {@link Date} object. <br>
     * <p>
     * 1) Use default implementation from {@link StdDateFormat#parse(String)} <br>
     * 2) Use defined date pattern <br>
     * </p>
     * 
     * @param dateStr date to parse
     * @return parsed date object
     * @throws ParseException if the target string doesn't match any defined date format
     */
    @Override
    public Date parse(String dateStr) throws ParseException {
        SimpleDateFormat df = cloneDateFormat();
        try {
            return df.parse(dateStr);
        } catch (ParseException e1) {
            try {
                return super.parse(dateStr);
            } catch (ParseException e2) {
                ParseException e = new ParseException(String.format(PARSE_ERROR_MSG, dateStr, getAllAllowedFormats()),
                    e2.getErrorOffset());
                e.initCause(e2);
                e.addSuppressed(e1);
                throw e;
            }
        }
    }

    /**
     * Format {@link Date} using defined pattern.
     * {@see {@link SimpleDateFormat#format(Date, StringBuffer, FieldPosition)}}
     * 
     * @param date a Date to be formatted into a date/time string.
     * @param toAppendTo the string buffer for the returning date/time string.
     * @param fieldPosition keeps track of the position of the field
     * @return the string buffer passed in as toAppendTo, with formatted text appended.
     */
    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        SimpleDateFormat df = cloneDateFormat();
        return df.format(date, toAppendTo, fieldPosition);
    }

    @Override
    public StdDateFormat clone() {
        return new ExtendedStdDateFormat(pattern, _timezone, _locale, _lenient, isColonIncludedInTimeZone());
    }

    private String getAllAllowedFormats() {
        StringBuilder sb = new StringBuilder();
        for (String f : ALL_FORMATS) {
            if (sb.length() > 0) {
                sb.append("\", \"");
            } else {
                sb.append('"');
            }
            sb.append(f);
        }
        sb.append("\", \"").append(pattern).append('"');
        return sb.toString();

    }

    /**
     * Create a new instance of {@link SimpleDateFormat}. <br>
     * NOTE: {@link SimpleDateFormat} is not thread save, so a new instance must be used every call
     * 
     * @return new instance of {@link SimpleDateFormat}
     */
    private SimpleDateFormat cloneDateFormat() {
        SimpleDateFormat df = (SimpleDateFormat) dateFormat.clone();
        if (_timezone != null) {
            df.setTimeZone(_timezone);
        }
        if (_lenient != null) {
            df.setLenient(_lenient);
        }
        return df;
    }
}
