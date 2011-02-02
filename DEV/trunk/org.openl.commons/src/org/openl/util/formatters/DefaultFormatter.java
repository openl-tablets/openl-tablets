package org.openl.util.formatters;

public class DefaultFormatter implements IFormatter {

    public DefaultFormatter() {        
    }

    public String format(Object obj) {
        return String.valueOf(obj);
    }

    public Object parse(String value) {
        return value;
    }

}
