package org.openl.rules.convertor;

import org.openl.rules.helpers.IntRange;

class String2IntRangeConvertor implements IString2DataConvertor<IntRange> {

    @Override
    public IntRange parse(String data, String format) {
        if (data == null) {
            return null;
        }
        return new IntRange(data);
    }
}
