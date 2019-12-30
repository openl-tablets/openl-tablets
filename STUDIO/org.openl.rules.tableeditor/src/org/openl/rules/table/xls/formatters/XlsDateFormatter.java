package org.openl.rules.table.xls.formatters;

import org.openl.util.formatters.DateFormatter;

import java.util.regex.Pattern;

/**
 * @author snshor
 */
public class XlsDateFormatter extends DateFormatter {

    private static final Pattern date_ptrn = Pattern.compile("^\\[\\$\\-.*?\\]");

    @Deprecated
    public static String convertToJavaFormat(String xlsFormat) {
        xlsFormat = xlsFormat.replaceAll("m", "M");
        if (Pattern.compile("h.MM").matcher(xlsFormat).find()) {
            xlsFormat = xlsFormat.replaceAll("h.MM", xlsFormat.substring(xlsFormat.lastIndexOf("h"), xlsFormat.lastIndexOf("h") + 2) + "mm");
        }
        xlsFormat = xlsFormat.replaceAll("Y", "y");
        xlsFormat = xlsFormat.replaceAll("\\\\-", "-");
        xlsFormat = xlsFormat.replaceAll(";@", "");
        xlsFormat = xlsFormat.replaceAll("\\\\ ", " ");
        xlsFormat = date_ptrn.matcher(xlsFormat).replaceAll("");
        return xlsFormat;
    }

    public XlsDateFormatter(String xlsFormat) {
        super(convertToJavaFormat(xlsFormat));
    }

}
