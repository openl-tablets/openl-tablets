package org.openl.util.formatters;



public class ConstTextFormatter implements IFormatter {

    private String format;

    public ConstTextFormatter(String format) {
        this.format = format;
    }

    public String format(Object obj) {
        return String.valueOf(obj);
    }

    public Object parse(String value) {
        return value;
    }

}
