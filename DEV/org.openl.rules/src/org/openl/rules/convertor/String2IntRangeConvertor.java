package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.rules.helpers.IntRange;

class String2IntRangeConvertor implements IString2DataConvertor<IntRange> {

    @Override
    public String format(IntRange data, String format) {
        if (data == null) return null;
        return data.toString();
    }

    @Override
    public IntRange parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;
        return new IntRange(data);
    }
}
