package org.openl.util.formatters;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;

import org.openl.util.Log;

public class NumberTextFormatter implements IFormatter {

    private DecimalFormat format;    

    public NumberTextFormatter(DecimalFormat fmt) {
        format = fmt;        
    }

    public NumberTextFormatter(String fmt) {
        format = new DecimalFormat(fmt);
    }

    public String format(Object obj) {
        return format.format(obj);
    }

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
