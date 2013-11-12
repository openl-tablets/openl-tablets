package org.openl.rules.convertor;

import java.text.DecimalFormat;
import java.text.ParseException;

import org.openl.binding.IBindingContext;
import org.openl.util.RuntimeExceptionWrapper;

public class String2ByteConvertor implements IString2DataConvertor {

    public String format(Object data, String format) {
        if (format == null) {
            return String.valueOf(data);
        }
        DecimalFormat df = new DecimalFormat(format);
        return df.format(((Byte) data).byteValue());
    }

    public Object parse(String data, String format, IBindingContext cxt) {
        if (format == null) {
            return Byte.valueOf(data);
        }
        DecimalFormat df = new DecimalFormat(format);

        Number n;
        try {
            n = df.parse(data);
        } catch (ParseException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }

        return Byte.valueOf(n.byteValue());
    }
}
