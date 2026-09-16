package org.openl.util.formatters;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Number formatter.
 *
 * @author Andrei Astrouski
 */
@RequiredArgsConstructor
@Slf4j
public class NumberFormatter implements IFormatter {


    private final NumberFormat format;

    public NumberFormatter() {
        this(new DecimalFormat());
    }

    public NumberFormatter(Locale locale) {
        this(NumberFormat.getInstance(locale == null ? Locale.getDefault() : locale));
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

    /**
     * The number the whole text stands for, or {@code null} where it stands for none.
     *
     * <p>Text the parser stops inside is not a number written differently: {@code 1abc} is not one, and
     * answering with the {@code 1} the parser managed would quietly put a value in place of what the author
     * wrote. Space around the number is not part of it and is ignored.
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
            log.debug("Could not parse Number: {}", value);
            return null;
        }
        return parsed;
    }

    private static DecimalFormatSymbols createDecimalFormatSymbols(Locale locale) {
        var symbols = new DecimalFormatSymbols(locale);
        symbols.setNaN("NaN");
        return symbols;
    }

}
