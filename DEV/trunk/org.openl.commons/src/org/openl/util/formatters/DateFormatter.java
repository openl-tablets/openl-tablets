package org.openl.util.formatters;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Date formatter.
 * 
 * @author Andrei Astrouski
 */
public class DateFormatter implements IFormatter {

    private static final Log LOG = LogFactory.getLog(DateFormatter.class);

    private DateFormat format;

    public DateFormatter() {
        this(new SimpleDateFormat());
    }

    public DateFormatter(Locale locale) {
        this(DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, locale == null ? Locale.getDefault() : locale));
    }

    public DateFormatter(DateFormat format) {
        this.format = format;
    }

    public DateFormatter(String format) {
        this(new SimpleDateFormat(format));
    }

    public DateFormatter(String format, Locale locale) {
        this(new SimpleDateFormat(format, locale));
    }

    public String format(Object value) {
        if (!(value instanceof Date)) {
            LOG.debug("Should be Date: " + value);
            return null;
        }

        return format.format(value);
    }

    public Object parse(String value) {
        try {
            return format.parse(value);
        } catch (ParseException e) {
            LOG.debug("Could not parse Date: " + value, e);
            return null;
        }
    }

}
