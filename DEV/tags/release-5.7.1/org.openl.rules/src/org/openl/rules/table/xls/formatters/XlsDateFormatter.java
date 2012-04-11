/**
 * Created Feb 28, 2007
 */
package org.openl.rules.table.xls.formatters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openl.util.Log;

/**
 * @author snshor
 *
 */
public class XlsDateFormatter extends AXlsFormatter {
    
    public static String DEFAULT_JAVA_DATE_FORMAT = "MM/dd/yyyy";
    public static String DEFAULT_XLS_DATE_FORMAT = "m/d/yy";
    
    private SimpleDateFormat format;
    
    public static String convertTojavaFormat(String fmt) {
        // TODO this will require much more work than that

        return fmt.replace('m', 'M');
    }
        
    public XlsDateFormatter(SimpleDateFormat fmt) {
        format = fmt;
    }

    public XlsDateFormatter(String fmt) {
        String javaFormat = convertTojavaFormat(fmt);
        format = new SimpleDateFormat(javaFormat);
    }    
    
    public String format(Object value) {
        if (!(value instanceof Date)) {
            Log.error("Should be date" + value);
            return null;
        }
        Date date = (Date) value;
        String fDate = format.format(date);
        return fDate;
    }

    public Object parse(String value) {
        try {
            return format.parse(value);
        } catch (ParseException e) {
            Log.warn("Could not parse Date: " + value, e);
            return value;
        }
    }
    
    public void setFormat(String format) {
        String javaFormat = convertTojavaFormat(format);
        this.format = new SimpleDateFormat(javaFormat);
    }
}
