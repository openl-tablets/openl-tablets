/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui.filters;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.IGridSelector;
import org.openl.rules.table.xls.formatters.AXlsFormatter;
import org.openl.rules.table.xls.formatters.SegmentFormatter;
import org.openl.rules.table.xls.formatters.XlsDateFormatter;
import org.openl.rules.table.xls.formatters.XlsFormattersManager;
import org.openl.rules.table.xls.formatters.XlsFormulaFormatter;
import org.openl.rules.table.xls.formatters.XlsNumberFormatter;

/**
 * @author snshor
 *
 */
public class XlsSimpleFilter implements IGridFilter {

    public static final String GENERAL_XLS_FORMAT = "General";

    private Map<String, AXlsFormatter> existingFormatters = new HashMap<String, AXlsFormatter>();

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
    
    private XlsDateFormatter findXlsDateFormat(String format) {
        XlsDateFormatter dateFormat = (XlsDateFormatter) existingFormatters.get(format);
        if (dateFormat != null) {
            return dateFormat;
        }
        dateFormat = (XlsDateFormatter)XlsFormattersManager.getFormatter(Date.class, format);
        existingFormatters.put(format, dateFormat);
        return dateFormat;
    }
  
    private XlsNumberFormatter findXlsNumberFormat(String format) {
        if (isGeneralFormat(format)) {
            return XlsNumberFormatter.General;
        }

        XlsNumberFormatter numberFormat = (XlsNumberFormatter) existingFormatters.get(format);
        if (numberFormat != null) {
            return numberFormat;
        }
        numberFormat = makeFormat(format);
        existingFormatters.put(format, numberFormat);
        return numberFormat;
    }
    
    private FormattedCell formatDate(FormattedCell formattedCell) {
        String format = formattedCell.getStyle().getTextFormat();
        XlsDateFormatter dateFormat = findXlsDateFormat(format);
        DateFilter dateFilter = new DateFilter(dateFormat);
        
        return dateFilter.filterFormat(formattedCell);
    }

    private FormattedCell formatNumber(FormattedCell formattedCell) {
        CellStyle style = formattedCell.getStyle();
        if (style.getHorizontalAlignment() == ICellStyle.ALIGN_GENERAL) {
            style.setHorizontalAlignment(ICellStyle.ALIGN_RIGHT);
        }

        String format = style.getTextFormat();

        XlsNumberFormatter xnf = findXlsNumberFormat(format);
        NumberFilter numFilter = new NumberFilter(xnf); 

        return numFilter.filterFormat(formattedCell);
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
        AXlsFormatter format = getFormat(fc);
        AXlsFormatter formulaFormat = new XlsFormulaFormatter(format);
        
        return new FormulaFilter(formulaFormat).filterFormat(fc);
    }

    private AXlsFormatter getFormat(FormattedCell formattedCell) {
        String format = formattedCell.getStyle().getTextFormat();
        if (isDateFormat(format)) {
            return findXlsDateFormat(format);
        } else if (formattedCell.getObjectValue() instanceof Number) {
            return findXlsNumberFormat(format);
        } else {
            return null;
        }
    }
    
    private XlsNumberFormatter makeFormat(String format) {
        if (GENERAL_XLS_FORMAT.equals(format)) {
            return XlsNumberFormatter.General;
        }

        return XlsNumberFormatter.makeFormat(format, existingFmts);
    }

    public AXlsFormatter getFormatter() {
        // TODO Auto-generated method stub
        return null;
    }    

}
