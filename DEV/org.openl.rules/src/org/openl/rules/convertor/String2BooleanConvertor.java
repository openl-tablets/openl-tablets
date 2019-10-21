package org.openl.rules.convertor;

import org.openl.util.BooleanUtils;

class String2BooleanConvertor implements IString2DataConvertor<Boolean> {

    @Override
    public Boolean parse(String data, String format) {
        if (data == null) {
            return null;
        }

        Boolean boolValue = BooleanUtils.toBooleanObject(data);

        if (boolValue == null) {
            throw new IllegalArgumentException("Cannon convert '" + data + "' to boolean type");
        }

        return boolValue;
    }
}
