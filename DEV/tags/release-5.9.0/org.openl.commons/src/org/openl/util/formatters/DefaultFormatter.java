package org.openl.util.formatters;

import org.apache.commons.lang.ObjectUtils;

public class DefaultFormatter implements IFormatter {

    public DefaultFormatter() {        
    }

    public String format(Object obj) {
        return ObjectUtils.toString(obj, null);
    }

    public Object parse(String value) {
        return value;
    }

}
