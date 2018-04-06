package org.openl.rules.table.xls.formatters;

import java.util.regex.Pattern;

import org.openl.util.formatters.DateFormatter;

/**
 * @author snshor
 *
 */
public class XlsDateFormatter extends DateFormatter {

    private static final Pattern date_ptrn = Pattern.compile("^\\[\\$\\-.*?\\]");

    @Deprecated
    public static String convertToJavaFormat(String xlsFormat) {
        // TODO this will require much more work than that
        xlsFormat = xlsFormat.replace('m', 'M');
        xlsFormat = xlsFormat.replaceAll("\\\\-", "-");
        xlsFormat = xlsFormat.replaceAll(";@", "");
        xlsFormat = xlsFormat.replaceAll("\\\\ "," ");
        xlsFormat = date_ptrn.matcher(xlsFormat).replaceAll("");
        return xlsFormat;
    }

    public XlsDateFormatter(String xlsFormat) {
        super(convertToJavaFormat(xlsFormat));
    }    

}
