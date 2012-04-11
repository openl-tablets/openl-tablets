package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.rules.helpers.IntRange;

public class String2IntRangeConvertor implements IString2DataConvertor {

    public String format(Object data, String format) {
        return String.valueOf(data);
    }

    public Object parse(String data, String format, IBindingContext cxt) {
        return new IntRange(data);
    }

}
