package org.openl.util.formatters;



public class ConstTextFormatter implements IFormatter {

    public ConstTextFormatter() {        
    }

    public String format(Object obj) {
        return String.valueOf(obj);
    }

    public Object parse(String value) {
        return value;
    }

}
