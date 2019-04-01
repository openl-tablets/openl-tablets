package org.openl.rules.convertor;

import org.openl.meta.DoubleValue;

class String2DoubleValueConvertor implements IString2DataConvertor<DoubleValue> {

    @Override
    public DoubleValue parse(String data, String format) {
        if (data == null) {
            return null;
        }
        return new DoubleValue(data);
    }
}