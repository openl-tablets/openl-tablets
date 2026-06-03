package org.openl.rules.serialization;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Extended implementation of {@link StdDateFormat}. Added an ability to use additional default date format.
 *
 * @author Vladyslav Pikus
 */
public class ExtendedStdDateFormat extends StdDateFormat {

    private static final String PARSE_ERROR_MSG =
            "Cannot parse date \"%s\": not compatible with any of standard forms (%s)";

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
     * Parse a string representation of date to {@link Date} object.
     * <p>
     * The configured pattern is tried first, but it is accepted only when it consumes the whole input. A
     * leftover tail — typically a {@code Z} or a numeric zone offset after the milliseconds — means the
     * time zone would be silently dropped and the value misread as local time. In that case, and whenever
     * the pattern does not match at all, parsing falls back to the standard ISO-8601 and RFC-1123 forms
     * supported by {@link StdDateFormat}, which honour the time zone.
     *
     * @param dateStr date to parse
     * @return parsed date object
     * @throws ParseException if the target string doesn't match any defined date format
     */
    @Override
    public Date parse(String dateStr) throws ParseException {
        var df = cloneDateFormat();
        var pos = new ParsePosition(0);
        var result = df.parse(dateStr, pos);
        if (result != null && pos.getIndex() == dateStr.length()) {
            return result;
        }
        try {
            return super.parse(dateStr);
        } catch (ParseException e) {
            var ex = new ParseException(PARSE_ERROR_MSG.formatted(dateStr, getAllAllowedFormats()),
                    e.getErrorOffset());
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * Format {@link Date} using defined pattern.
     * {@see {@link SimpleDateFormat#format(Date, StringBuffer, FieldPosition)}}
     *
     * @param date          a Date to be formatted into a date/time string.
     * @param toAppendTo    the string buffer for the returning date/time string.
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

    /**
     * Returns a copy bound to {@code timeZone}, keeping the configured pattern. {@link StdDateFormat} returns
     * a plain instance from this method, which would drop the custom pattern; Jackson calls it for
     * {@code @JsonFormat(timezone = ...)} fields, so the override preserves this type.
     * <p>
     * Returns {@code this} unchanged when the time zone already matches, mirroring {@link StdDateFormat}.
     */
    @Override
    public ExtendedStdDateFormat withTimeZone(TimeZone timeZone) {
        var tz = timeZone == null ? TimeZone.getDefault() : timeZone;
        if (tz == _timezone || tz.equals(_timezone)) {
            return this;
        }
        return new ExtendedStdDateFormat(pattern, tz, _locale, _lenient, isColonIncludedInTimeZone());
    }

    /**
     * Returns a copy bound to {@code locale}, keeping the configured pattern. Overridden for the same reason
     * as {@link #withTimeZone(TimeZone)} — Jackson calls it for {@code @JsonFormat(locale = ...)} fields.
     * <p>
     * Returns {@code this} unchanged when the locale already matches, mirroring {@link StdDateFormat}.
     */
    @Override
    public ExtendedStdDateFormat withLocale(Locale locale) {
        if (Objects.equals(locale, _locale)) {
            return this;
        }
        return new ExtendedStdDateFormat(pattern, _timezone, locale, _lenient, isColonIncludedInTimeZone());
    }

    /**
     * Returns a copy with the given leniency, keeping the configured pattern. Overridden for the same reason
     * as {@link #withTimeZone(TimeZone)} — Jackson calls it for {@code @JsonFormat(lenient = ...)} fields.
     * <p>
     * Returns {@code this} unchanged when the leniency already matches, mirroring {@link StdDateFormat}.
     */
    @Override
    public ExtendedStdDateFormat withLenient(Boolean lenient) {
        if (Objects.equals(lenient, _lenient)) {
            return this;
        }
        return new ExtendedStdDateFormat(pattern, _timezone, _locale, lenient, isColonIncludedInTimeZone());
    }

    /**
     * Returns a copy with the given zone-offset colon setting, keeping the configured pattern. Overridden for
     * the same reason as {@link #withTimeZone(TimeZone)}.
     * <p>
     * Returns {@code this} unchanged when the setting already matches, mirroring {@link StdDateFormat}.
     */
    @Override
    public ExtendedStdDateFormat withColonInTimeZone(boolean withColonInTimeZone) {
        if (isColonIncludedInTimeZone() == withColonInTimeZone) {
            return this;
        }
        return new ExtendedStdDateFormat(pattern, _timezone, _locale, _lenient, withColonInTimeZone);
    }

    private String getAllAllowedFormats() {
        StringBuilder sb = new StringBuilder();
        for (String f : ALL_FORMATS) {
            if (!sb.isEmpty()) {
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
