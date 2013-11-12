package org.openl.rules.convertor;

import java.text.DecimalFormat;
import java.text.ParseException;

import org.openl.binding.IBindingContext;
import org.openl.util.RuntimeExceptionWrapper;

public class String2FloatConvertor extends NumberConvertor implements IString2DataConvertor {

    private static final String DEFAULT_FLOAT_FORMAT = "#0.00";

    public String format(Object data, String format) {
        if (format == null) {
            format = DEFAULT_FLOAT_FORMAT;
        }

        DecimalFormat df = new DecimalFormat(format);

        return df.format(((Float) data).floatValue());
    }
    
    public Object parse(String xdata, String format, IBindingContext cxt) {

        if (format != null) {
            DecimalFormat df = new DecimalFormat(format);
            try {
                Number n = df.parse(xdata);

                return new Float(n.floatValue());
            } catch (ParseException e) {
                throw RuntimeExceptionWrapper.wrap("", e);
            }
        }

        String data = numberStringWithoutModifier(xdata);

        float floatValue = Float.parseFloat(data);

        return xdata == data ? Float.valueOf(floatValue) : Float.valueOf((float) (floatValue * numberModifier(xdata)));
    }
}
