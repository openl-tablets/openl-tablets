package org.openl.util.formatters;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Number formatter.
 * 
 * @author Andrei Astrouski
 */
public class NumberFormatter implements IFormatter {

    private static final Log LOG = LogFactory.getLog(NumberFormatter.class);

    private NumberFormat format;

    public NumberFormatter() {
        this(new DecimalFormat());
    }

    public NumberFormatter(Locale locale) {
        this(NumberFormat.getInstance(
                locale == null ? Locale.getDefault() : locale));
    }

    public NumberFormatter(NumberFormat format) {
        this.format = format;        
    }

    public NumberFormatter(String format) {
        this(new DecimalFormat(format));
    }

    public NumberFormatter(String format, Locale locale) {
        this(new DecimalFormat(format, new DecimalFormatSymbols(locale)));
    }

    public String format(Object value) {
        if (!(value instanceof Number)) {
            LOG.debug("Should be Number: " + value);
            return null;
        }

        return format.format(value);
    }

    public Object parse(String value) {
        try {
            return format.parse(value);
        } catch (ParseException e) {
            LOG.debug("Could not parse Number: " + value);
            return null;
        }
    }

}
