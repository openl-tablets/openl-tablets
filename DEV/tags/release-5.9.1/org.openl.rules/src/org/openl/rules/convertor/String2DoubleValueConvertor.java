package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.meta.DoubleValue;

public class String2DoubleValueConvertor implements IString2DataConvertor {

    public String format(Object data, String format) {
        return String.valueOf(data);
    }

    public Object parse(String data, String format, IBindingContext cxt) {
        return new DoubleValue(data);
    }

}