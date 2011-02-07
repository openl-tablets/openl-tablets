package org.openl.rules.table.xls.formatters;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.openl.util.formatters.IFormatter;
import org.openl.util.formatters.NumberFormatter;

/**
 * This class provides default conversion of MS Excel formats to Java formats.
 * There is no way for practical and technical reasons to map it completely
 * 100%. Therefore this class will be supplemented by pre-defined hardcoded
 * mapping for most of embedded MS Excel formats.
 *
 * @author snshor
 */
public class XlsNumberFormatter implements IFormatter {

    private static final Log LOG = LogFactory.getLog(XlsNumberFormatter.class);

    private int formatIndex;
    private String format;
    private DataFormatter dataFormatter;
    private Locale locale;

    public XlsNumberFormatter(int xlsFormatIndex, String xlsFormat, DataFormatter xlsDataFormatter) {
        this(xlsFormatIndex, xlsFormat, xlsDataFormatter, null);
    }

    public XlsNumberFormatter(int xlsFormatIndex, String xlsFormat, DataFormatter xlsDataFormatter, Locale locale) {
        this.formatIndex = xlsFormatIndex;
        this.format = xlsFormat;
        this.dataFormatter = xlsDataFormatter;
        this.locale = locale;
    }

    public String format(Object value) {
        if (!(value instanceof Number)) {
            LOG.error("Should be Number: " + value);
            return null;
        }

        double doubleValue = ((Number) value).doubleValue();
        String formattedDate = dataFormatter.formatRawCellContents(doubleValue, formatIndex, format);

        return formattedDate;
    }

    public Object parse(String value) {
        NumberFormatter numberFormatter = new NumberFormatter(locale);
        return numberFormatter.parse(value);
    }

}
