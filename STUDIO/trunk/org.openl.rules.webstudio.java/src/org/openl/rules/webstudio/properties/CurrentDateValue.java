package org.openl.rules.webstudio.properties;

import java.util.Calendar;

import org.openl.rules.table.xls.XlsDateFormat;

/**
 * Handles current date value of the system.
 * 
 * @author DLiauchuk
 *
 */
public class CurrentDateValue implements ISystemValue {
    
    public CurrentDateValue(){};
    
    public String getValue() {
        Calendar cal = Calendar.getInstance();
        XlsDateFormat dateFormat = new XlsDateFormat(XlsDateFormat.DEFAULT_JAVA_DATE_FORMAT);
        return dateFormat.format(cal.getTime()); 
    }

}
