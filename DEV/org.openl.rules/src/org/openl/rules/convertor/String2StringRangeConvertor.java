package org.openl.rules.convertor;

import org.openl.rules.helpers.StringRange;

public class String2StringRangeConvertor implements IString2DataConvertor<StringRange> {

    @Override
    public StringRange parse(String data, String format) {
        if (data == null) {
            return null;
        }
        return new StringRange(data);
    }
}