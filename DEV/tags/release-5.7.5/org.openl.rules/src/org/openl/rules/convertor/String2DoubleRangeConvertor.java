package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.rules.helpers.DoubleRange;

public class String2DoubleRangeConvertor implements IString2DataConvertor {

    public String format(Object data, String format) {
        return String.valueOf(data);
    }

    public Object parse(String data, String format, IBindingContext bindingContext) {        
        return new DoubleRange(data);
    }

}
