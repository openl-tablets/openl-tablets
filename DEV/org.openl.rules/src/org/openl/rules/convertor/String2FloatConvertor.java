package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.util.RuntimeExceptionWrapper;

import java.text.ParseException;

public class String2FloatConvertor implements IString2DataConvertor {

    private NumberFormatHelper formatHelper = new NumberFormatHelper("0.##");

    @Override
    public String format(Object data, String format) {
        return formatHelper.format((Float) data, format);
    }

    @Override
    public Float parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;
        try {
            Number n = formatHelper.parse(data, format);
            return n.floatValue();
        } catch (ParseException e) {
            throw RuntimeExceptionWrapper.wrap("", e);
        }
    }
}
