package org.openl.rules.table.xls.formatters;

import org.apache.commons.lang.ObjectUtils;

public class XlsStringFormatter extends AXlsFormatter {
    
    public String format(Object value) {
        return ObjectUtils.toString(value, null);
    }
    
    public Object parse(String value) {
        return value;
    }
}
