package org.openl.rules.table.xls.formatters;

public class XlsStringFormatter extends AXlsFormatter {
    
    public String format(Object value) {
        return value.toString();
    }
    
    public Object parse(String value) {
        return value;
    }
}
