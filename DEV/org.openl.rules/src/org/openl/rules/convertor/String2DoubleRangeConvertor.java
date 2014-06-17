package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.rules.helpers.DoubleRange;

class String2DoubleRangeConvertor implements IString2DataConvertor<DoubleRange> {

    @Override
    public String format(DoubleRange data, String format) {
        if (data == null) return null;
        return data.toString();
    }

    @Override
    public DoubleRange parse(String data, String format, IBindingContext bindingContext) {
        if (data == null) return null;
        return new DoubleRange(data);
    }
}
