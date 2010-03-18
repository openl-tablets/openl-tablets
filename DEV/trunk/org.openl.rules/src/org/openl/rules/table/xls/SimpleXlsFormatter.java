/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.xls;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.IGridSelector;
import org.openl.rules.table.xls.SegmentFormatter;

/**
 * @author snshor
 *
 */
public class SimpleXlsFormatter implements IGridFilter {

    public static final String GENERAL_XLS_FORMAT = "General";

    private Map<String, XlsFormat> existingFormatters = new HashMap<String, XlsFormat>();

    private Map<String, SegmentFormatter> existingFmts = new HashMap<String, SegmentFormatter>();
    
    public Object parse(String value) {
        throw new UnsupportedOperationException("This format does not parse");
    }
    
    public IGridSelector getGridSelector() {
        return null;
    }
    
    public FormattedCell filterFormat(FormattedCell fc) {
        switch (fc.getType()) {
            case IGrid.CELL_TYPE_NUMERIC:
                return formatNumberOrDate(fc);
            case IGrid.CELL_TYPE_FORMULA:
                return formatFormula(fc);
        }

        return fc;
    }
    
    private static boolean containsAny(String src, String test) {
        char[] tst = test.toCharArray();
        for (int i = 0; i < tst.length; i++) {
            if (src.indexOf(tst[i]) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDateFormat(String format) {
        // fmt = fmt.toLowerCase();
        if (format == null) {
            return false;
        }

        if (GENERAL_XLS_FORMAT.equalsIgnoreCase(format)) {
            return false;
        }

        if (containsAny(format, "#0?")) {
            return false;
        }

        return true;
    }

    private static boolean isGeneralFormat(String format) {
        return format == null || GENERAL_XLS_FORMAT.equalsIgnoreCase(format);
    }    
    
    private XlsDateFormat findXlsDateFormat(String format) {
        XlsDateFormat dateFormat = (XlsDateFormat) existingFormatters.get(format);
        if (dateFormat != null) {
            return dateFormat;
        }
        dateFormat = (XlsDateFormat)XlsFormat.getFormatter(Date.class, format);
        existingFormatters.put(format, dateFormat);
        return dateFormat;
    }
  
    private XlsNumberFormat findXlsNumberFormat(String format) {
        if (isGeneralFormat(format)) {
            return XlsNumberFormat.General;
        }

        XlsNumberFormat numberFormat = (XlsNumberFormat) existingFormatters.get(format);
        if (numberFormat != null) {
            return numberFormat;
        }
        numberFormat = makeFormat(format);
        existingFormatters.put(format, numberFormat);
        return numberFormat;
    }
    
    private FormattedCell formatDate(FormattedCell formattedCell) {
        String format = formattedCell.getStyle().getTextFormat();
        XlsDateFormat dateFormat = findXlsDateFormat(format);
        dateFormat.filterFormat(formattedCell);

        return formattedCell;
    }

    private FormattedCell formatNumber(FormattedCell formattedCell) {
        CellStyle style = formattedCell.getStyle();
        if (style.getHorizontalAlignment() == ICellStyle.ALIGN_GENERAL) {
            style.setHorizontalAlignment(ICellStyle.ALIGN_RIGHT);
        }

        String fmt = style.getTextFormat();

        XlsNumberFormat xnf = findXlsNumberFormat(fmt);
        xnf.filterFormat(formattedCell);

        return formattedCell;
    }
    
    private FormattedCell formatNumberOrDate(FormattedCell formattedCell) {
        // the only way to tell a date from a number (as far as I can
        // understand)
        // is to check format, so let's do it

        if (isDateFormat(formattedCell.getStyle().getTextFormat())) {
            return formatDate(formattedCell);
        }

        return formatNumber(formattedCell);
    }

    private FormattedCell formatFormula(FormattedCell fc) {
        XlsFormat format = getFormat(fc);
        return new XlsFormulaFormat(format).filterFormat(fc);
    }

    private XlsFormat getFormat(FormattedCell formattedCell) {
        String format = formattedCell.getStyle().getTextFormat();
        if (isDateFormat(format)) {
            return findXlsDateFormat(format);
        } else if (formattedCell.getObjectValue() instanceof Number) {
            return findXlsNumberFormat(format);
        } else {
            return null;
        }
    }
    
    private XlsNumberFormat makeFormat(String format) {
        if (GENERAL_XLS_FORMAT.equals(format)) {
            return XlsNumberFormat.General;
        }

        return XlsNumberFormat.makeFormat(format, existingFmts);
    }    

}
