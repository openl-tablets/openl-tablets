package org.openl.rules.convertor;

import java.text.DecimalFormat;
import java.text.ParseException;

import org.openl.binding.IBindingContext;
import org.openl.util.RuntimeExceptionWrapper;

public class String2DoubleConvertor extends NumberConvertor implements IString2DataConvertor {
    
    private static final String DEFAULT_DOUBLE_FORMAT = "#0.00";

    public String format(Object data, String format) {
        if (format == null) {
            format = DEFAULT_DOUBLE_FORMAT;
        }

        DecimalFormat df = new DecimalFormat(format);

        return df.format(((Number) data).doubleValue());
    }

    public Object parse(String xdata, String format, IBindingContext cxt) {

        if (format != null) {
            DecimalFormat df = new DecimalFormat(format);
            try {
                Number n = df.parse(xdata);

                return new Double(n.doubleValue());
            } catch (ParseException e) {
                throw RuntimeExceptionWrapper.wrap("", e);
            }
        }

        String data = numberStringWithoutModifier(xdata);

        double d = Double.parseDouble(data);

        return xdata == data ? new Double(d) : new Double(d * numberModifier(xdata));
    }

}