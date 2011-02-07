/**
 * Created Feb 28, 2007
 */
package org.openl.rules.table.xls.formatters;

import org.openl.util.formatters.DateFormatter;

/**
 * @author snshor
 *
 */
public class XlsDateFormatter extends DateFormatter {

    public static final String DEFAULT_XLS_DATE_FORMAT = "m/d/yy";

    @Deprecated
    public static String convertToJavaFormat(String xlsFormat) {
        // TODO this will require much more work than that
        return xlsFormat.replace('m', 'M');
    }

    public XlsDateFormatter(String xlsFormat) {
        super(convertToJavaFormat(xlsFormat));
    }    

}
