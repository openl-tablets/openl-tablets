package org.openl.rules.convertor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * A utility for parsing and formatting numbers. This utility uses US locale to parse and to format numbers.
 */
class NumberFormatHelper {

    private final String defaultFormat;

    NumberFormatHelper() {
        defaultFormat = "";
    }

    NumberFormatHelper(String defaultFormat) {
        this.defaultFormat = defaultFormat;
    }

    /**
     * Format a number to String according to a format. If the input string is null then null will be returned.
     *
     * @param data   a number to format
     * @param format a format of a number. If it is null then a default format will be used.
     * @return a String or null
     */
    String format(Number data, String format) {
        if (data == null) return null;

        DecimalFormat df = getFormatter(format);

        return df.format(data);
    }

    /**
     * Parse an input string to a number. If the input string is null then null will be returned.
     *
     * @param data   an input string to parse
     * @param format a format of parsed string. If it is null then a default format will be used.
     * @return a number or null
     * @throws NumberFormatException if the specified string cannot be parsed
     */
    Number parse(String data, String format) {
        if (data == null) return null;
        DecimalFormat df = getFormatter(format);
        if (data.endsWith("%")) {
            // Configure to parse percents
            df.setMultiplier(100);
            data = data.substring(0, data.length() - 1);
        }
        ParsePosition position = new ParsePosition(0);
        Number number = df.parse(data, position);
        int index = position.getIndex();
        if (index < data.length() || index == 0) {
            throw new NumberFormatException("Cannot convert String \"" + data + "\" to numeric type");
        }
        return number;
    }

    private DecimalFormat getFormatter(String format) {
        // NOTE!!! Using new DecimalFormat(format), depends on the users locale.
        // E.g. if locale on the users machine is ru_RU, the ','(comma) delimiter will
        // be used. It is not appropriate for many cases, e.g. formatting the value for writing its
        // value to the Java class(Java expects '.' dot delimiter).
        //
        // NOTE2 DecimalFormat uses <code>RoundingMode.HALF_EVEN</code> by default. This is also known as banker's rounding.
        // It is not the same as math rounding (normal/usual rounding) which is <code>RoundingMode.HALF_UP</code>
        //
        DecimalFormat df;
        if (format == null) {
            df = new DecimalFormat(defaultFormat);
        } else {
            df = new DecimalFormat(format);
        }

        // Reset using a default locale and set force the US locale.
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        return df;
    }
}
