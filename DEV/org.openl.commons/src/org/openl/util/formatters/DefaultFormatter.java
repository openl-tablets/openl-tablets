package org.openl.util.formatters;

import java.util.Arrays;

public class DefaultFormatter implements IFormatter {

    @Override
    public String format(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.getClass().isArray() ? Arrays.deepToString((Object[]) obj) : obj.toString();
    }

    @Override
    public Object parse(String value) {
        return value;
    }

}
