package org.openl.rules.convertor;

import org.openl.rules.helpers.DoubleRange;

class String2DoubleRangeConvertor implements IString2DataConvertor<DoubleRange> {

    @Override
    public DoubleRange parse(String data, String format) {
        if (data == null)
            return null;
        return new DoubleRange(data);
    }
}
