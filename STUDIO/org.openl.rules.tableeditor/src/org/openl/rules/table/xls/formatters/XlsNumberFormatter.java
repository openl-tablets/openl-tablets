package org.openl.rules.table.xls.formatters;

import java.util.Locale;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.openl.util.formatters.IFormatter;
import org.openl.util.formatters.NumberFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides default conversion of MS Excel formats to Java formats. There is no way for practical and
 * technical reasons to map it completely 100%. Therefore this class will be supplemented by pre-defined hardcoded
 * mapping for most of embedded MS Excel formats.
 *
 * @author snshor
 */
class XlsNumberFormatter implements IFormatter {

    private final Logger log = LoggerFactory.getLogger(XlsNumberFormatter.class);

    private int formatIndex;
    private String format;
    private DataFormatter dataFormatter;
    private Locale locale;

    public XlsNumberFormatter(int xlsFormatIndex, String xlsFormat, DataFormatter xlsDataFormatter, Locale locale) {
        this.formatIndex = xlsFormatIndex;
        this.format = xlsFormat;
        this.dataFormatter = xlsDataFormatter;
        this.locale = locale;
    }

    @Override
    public String format(Object value) {
        if (!(value instanceof Number)) {
            log.debug("Should be Number: {}", value);
            return null;
        }

        double doubleValue = ((Number) value).doubleValue();
        String formattedDate = dataFormatter.formatRawCellContents(doubleValue, formatIndex, format);

        if (format.startsWith("# ?/") || format.startsWith("# ??/") || format.startsWith("# ???/")) {
            if (doubleValue < 1 && doubleValue > 0) {
                formattedDate = formattedDate.substring(2);
            } else if (doubleValue < 0 && doubleValue > -1) {
                formattedDate = "-" + formattedDate.substring(3);
            }
        }
        return formattedDate;
    }

    @Override
    public Object parse(String value) {
        NumberFormatter numberFormatter = new NumberFormatter(locale);
        return numberFormatter.parse(value);
    }

}
