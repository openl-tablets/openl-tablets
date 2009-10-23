package org.openl.rules.lang.xls.utils;

import java.util.Date;

import org.openl.rules.table.xls.XlsDateFormat;

public class StringToDateCaster implements ICustomCaster<String, Date> {

    public Date cast(String value) {
        XlsDateFormat dateFormat = new XlsDateFormat(XlsDateFormat.DEFAULT_JAVA_DATE_FORMAT);        
        Date newDate =(Date) dateFormat.parse(value);
        return newDate;
    }

}
