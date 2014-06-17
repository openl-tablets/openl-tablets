package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.meta.DoubleValue;

class String2DoubleValueConvertor implements IString2DataConvertor<DoubleValue> {

    @Override
    public String format(DoubleValue data, String format) {
        if (data == null) return null;
        return data.toString();
    }

    @Override
    public DoubleValue parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;
        return new DoubleValue(data);
    }
}