package org.openl.rules.table.ui;

public class ConstTextFormatter implements ITextFormatter {

    private String format;

    public ConstTextFormatter(String format) {
        this.format = format;
    }

    public String format(Object obj) {
        return format;
    }

    public Object parse(String value) {
        return value;
    }

}
