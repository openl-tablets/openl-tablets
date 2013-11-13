package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;

public class String2StringConvertor implements IString2DataConvertor {

    public String format(Object data, String format) {
        return String.valueOf(data);
    }

    public Object parse(String data, String format, IBindingContext cxt) {
        return data;
    }

}
