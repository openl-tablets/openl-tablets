package org.openl.util.formatters;

public class DefaultFormatter implements IFormatter {

    public DefaultFormatter() {
    }

    public String format(Object obj) {
        return obj == null ? null : obj.toString();
    }

    public Object parse(String value) {
        return value;
    }

}
