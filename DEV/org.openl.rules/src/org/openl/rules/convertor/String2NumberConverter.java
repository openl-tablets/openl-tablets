package org.openl.rules.convertor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * A base converter class which implements logic for parsing Java's numbers. This converter uses US locale for parsing
 * numbers.
 *
 * @param <T> type of a number
 * @author Yury Molchan
 */
abstract class String2NumberConverter<T extends Number> implements IString2DataConvertor<T> {

    /**
     * Parse an input string to a number. If the input string is null then null will be returned.
     *
     * @param data an input string to parse
     * @param format a format of parsed string. If it is null then a default format will be used.
     * @return a number or null
     * @throws NumberFormatException if the specified string cannot be parsed
     */
    @Override
    public T parse(String data, String format) {
        if (data == null)
            return null;
        if (data.length() == 0) {
            throw new NumberFormatException("Cannot convert an empty String to numeric type");
        }
        if ("%".equals(data)) {
            throw new NumberFormatException("Cannot convert \"%\" to numeric type");
        }
        DecimalFormat df = getFormatter(format);
        if (data.endsWith("%")) {
            // Configure to parse percents
            df.setMultiplier(100);
            data = data.substring(0, data.length() - 1);
        }
        ParsePosition position = new ParsePosition(0);
        Number number = df.parse(data, position);
        int index = position.getIndex();
        if (index < data.length()) {
            throw new NumberFormatException("Cannot convert \"" + data + "\" to numeric type");
        }
        return convert(number, data);
    }

    /**
     * Converts a number to the required type. Also this method should check range of numbers for avoiding overflows and
     * check type of numbers.
     *
     * @param number a number
     * @param data a parsed string to the number
     * @return a wrapped primitive type
     */
    abstract T convert(Number number, String data);

    DecimalFormat getFormatter(String format) {
        // Reset using a default locale and set force the US locale.
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setInfinity("Infinity");
        symbols.setNaN("NaN");

        // NOTE!!! Using new DecimalFormat(format), depends on the users locale.
        // E.g. if locale on the users machine is ru_RU, the ','(comma) delimiter will
        // be used. It is not appropriate for many cases, e.g. formatting the value for writing its
        // value to the Java class(Java expects '.' dot delimiter).
        //
        // NOTE2 DecimalFormat uses <code>RoundingMode.HALF_EVEN</code> by default. This is also known as banker's
        // rounding.
        // It is not the same as math rounding (normal/usual rounding) which is <code>RoundingMode.HALF_UP</code>
        //
        DecimalFormat df;
        if (format != null) {
            df = new DecimalFormat(format, symbols);
        } else {
            df = new DecimalFormat("", symbols);
            df.setGroupingUsed(false);
            df.setMinimumIntegerDigits(1);
        }
        return df;
    }
}
