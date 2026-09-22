package org.openl.rules.convertor;


import org.jspecify.annotations.Nullable;

import org.openl.util.BooleanUtils;

class String2BooleanConvertor implements IString2DataConvertor<Boolean> {

    @Override
    public @Nullable Boolean parse(String data, String format) {
        if (data == null) {
            return null;
        }

        Boolean boolValue = BooleanUtils.toBooleanObject(data);

        if (boolValue == null) {
            throw new IllegalArgumentException("Cannon convert '%s' to boolean type".formatted(data));
        }

        return boolValue;
    }
}
