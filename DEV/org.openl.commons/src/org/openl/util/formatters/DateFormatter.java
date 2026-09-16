package org.openl.util.formatters;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

/**
 * Date formatter.
 *
 * @author Andrei Astrouski
 */
@Slf4j
public class DateFormatter implements IFormatter {


    private DateFormat format;

    public DateFormatter() {
        this(new SimpleDateFormat());
    }

    public DateFormatter(Locale locale) {
        this(DateFormat
                .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale == null ? Locale.getDefault() : locale));
    }

    public DateFormatter(DateFormat format) {
        this.format = format;
    }

    public DateFormatter(String format) {
        try {
            this.format = new SimpleDateFormat(format);
        } catch (Exception e) {
            log.error("Could not create format: {}", format);
            this.format = new SimpleDateFormat();
        }
    }

    public DateFormatter(String format, Locale locale) {
        try {
            this.format = new SimpleDateFormat(format, locale);
        } catch (Exception e) {
            log.error("Could not create format: {}", format);
            this.format = DateFormat
                    .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale == null ? Locale.getDefault() : locale);
        }
    }

    @Override
    public String format(Object value) {
        if (!(value instanceof Date)) {
            log.debug("Should be Date: {}", value);
            return null;
        }

        return format.format(value);
    }

    /**
     * The date the whole text stands for, or {@code null} where it stands for none.
     *
     * <p>Text the parser stops inside is not a date written differently: a pattern reading two digits of a
     * year takes {@code 12/31/2024} for the year 20 and leaves {@code 24} unread, and answering with that
     * date would put a value in place of what the author wrote. Space around the date is not part of it.
     */
    @Override
    public Object parse(String value) {
        if (value == null) {
            return null;
        }
        var text = value.trim();
        var readTo = new ParsePosition(0);
        var parsed = format.parse(text, readTo);
        if (parsed == null || readTo.getIndex() != text.length()) {
            log.debug("Could not parse Date: {}", value);
            return null;
        }
        return parsed;
    }

}
