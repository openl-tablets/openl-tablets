package org.openl.rules.convertor;

import org.openl.rules.helpers.CharRange;

public class String2CharRangeConvertor implements IString2DataConvertor<CharRange> {

    @Override
    public CharRange parse(String data, String format) {
        if (data == null) {
            return null;
        }
        return new CharRange(data);
    }
}