package org.openl.util.formatters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Date formatter.
 *
 * @author Andrei Astrouski
 */
public class DateFormatter implements IFormatter {

    private final Logger log = LoggerFactory.getLogger(DateFormatter.class);

    private DateFormat format;

    public DateFormatter() {
        this(new SimpleDateFormat());
    }

    public DateFormatter(Locale locale) {
        this(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale == null ? Locale.getDefault()
                : locale));
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
            this.format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                    locale == null ? Locale.getDefault() : locale);
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

    @Override
    public Object parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return format.parse(value);
        } catch (ParseException e) {
            log.debug("Could not parse Date: {}", value, e);
            return null;
        }
    }

}
