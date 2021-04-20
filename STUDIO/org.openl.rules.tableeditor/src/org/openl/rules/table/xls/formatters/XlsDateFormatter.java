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
        xlsFormat = xlsFormat.replace("m", "M");
        if (Pattern.compile("h.MM").matcher(xlsFormat).find()) {
            xlsFormat = xlsFormat.replaceAll("h.MM",
                xlsFormat.substring(xlsFormat.lastIndexOf("h"), xlsFormat.lastIndexOf("h") + 2) + "mm");
        }
        xlsFormat = xlsFormat.replace("Y", "y");
        xlsFormat = xlsFormat.replace("\\-", "-");
        xlsFormat = xlsFormat.replace(";@", "");
        // from the description of the Excel date format:
        // If the format contains AM or PM, the hour is based on the 12-hour clock.
        // Otherwise, the hour is based on the 24-hour clock.
        // options: AM/PM, am/pm, A/P, a/p
        Pattern amPmPattern = Pattern.compile("((AM/PM)|(A/P))", Pattern.CASE_INSENSITIVE);
        if (!amPmPattern.matcher(xlsFormat).find()) {
            xlsFormat = xlsFormat.replace("h", "H");
        }
        xlsFormat = amPmPattern.matcher(xlsFormat).replaceAll("a");
        xlsFormat = xlsFormat.replace("\\ ", " ");
        xlsFormat = date_ptrn.matcher(xlsFormat).replaceAll("");
        return xlsFormat;
    }

    public XlsDateFormatter(String xlsFormat) {
        super(convertToJavaFormat(xlsFormat));
    }

}
