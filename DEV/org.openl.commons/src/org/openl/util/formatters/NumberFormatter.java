package org.openl.util.formatters;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Number formatter.
 *
 * @author Andrei Astrouski
 */
public class NumberFormatter implements IFormatter {

    private final Logger log = LoggerFactory.getLogger(NumberFormatter.class);

    private NumberFormat format;

    public NumberFormatter() {
        this(new DecimalFormat());
    }

    public NumberFormatter(Locale locale) {
        this(NumberFormat.getInstance(locale == null ? Locale.getDefault() : locale));
    }

    public NumberFormatter(NumberFormat format) {
        this.format = format;
    }

    public NumberFormatter(String format) {
        this(new DecimalFormat(format));
    }

    public NumberFormatter(String format, Locale locale) {
        this(new DecimalFormat(format, createDecimalFormatSymbols(locale)));
    }

    @Override
    public String format(Object value) {
        if (!(value instanceof Number)) {
            log.debug("Should be Number: {}", value);
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
            log.debug("Could not parse Number: {}", value);
            return null;
        }
    }

    private static DecimalFormatSymbols createDecimalFormatSymbols(Locale locale) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        symbols.setNaN("NaN");
        return symbols;
    }

}
