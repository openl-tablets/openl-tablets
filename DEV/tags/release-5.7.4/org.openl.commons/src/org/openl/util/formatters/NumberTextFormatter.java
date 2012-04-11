package org.openl.util.formatters;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

import org.openl.util.Log;

public class NumberTextFormatter implements IFormatter {

    private DecimalFormat format;    

    public NumberTextFormatter(DecimalFormat fmt) {
        format = fmt;        
    }

    public NumberTextFormatter(String fmt) {
        format = new DecimalFormat(fmt);
    }

    public NumberTextFormatter(String fmt, Locale locale) {
        format = new DecimalFormat(fmt, new DecimalFormatSymbols(locale));
    }

    public String format(Object obj) {
        return format.format(obj);
    }
    
    /**
     * Tries to parse value using inner {@link DecimalFormat}, if can`t, tries
     * to parse the income value by {@link DateFormat#parse(String)}
     */
    public Object parse(String value) {
        try {
            return format.parse(value);
        } catch (ParseException e) {
            Log.warn("Could not parse number: " + value);
        }

        try {
            return DateFormat.getDateInstance().parse(value);
        } catch (ParseException pe) {
            return value;
        }

    }

}
