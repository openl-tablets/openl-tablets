package org.openl.util.formatters;

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

    private SimpleDateFormat format;

    public DateFormatter(SimpleDateFormat fmt) {
        this.format = fmt;
    }

    public DateFormatter(String fmt) {
        format = new SimpleDateFormat(fmt);
    }

    public DateFormatter(String fmt, Locale locale) {
        format = new SimpleDateFormat(fmt, locale);
    }

    public String format(Object value) {
        if (!(value instanceof Date)) {
            LOG.error("Should be date" + value);
            return null;
        }
        Date date = (Date) value;
        return format.format(date);
    }

    public Object parse(String value) {
        try {
            return format.parse(value);
        } catch (ParseException e) {
            LOG.warn("Could not parse Date: " + value, e);
            return value;
        }
    }

}
