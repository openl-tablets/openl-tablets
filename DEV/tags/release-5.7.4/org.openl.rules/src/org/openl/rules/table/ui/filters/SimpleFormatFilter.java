/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui.filters;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ui.IGridSelector;
import org.openl.rules.table.xls.formatters.AXlsFormatter;
import org.openl.rules.table.xls.formatters.SegmentFormatter;
import org.openl.rules.table.xls.formatters.XlsArrayFormatter;
import org.openl.rules.table.xls.formatters.XlsBooleanFormatter;
import org.openl.rules.table.xls.formatters.XlsDateFormatter;
import org.openl.rules.table.xls.formatters.XlsEnumFormatter;
import org.openl.rules.table.xls.formatters.XlsFormattersManager;
import org.openl.rules.table.xls.formatters.XlsFormulaFormatter;
import org.openl.rules.table.xls.formatters.XlsNumberFormatter;
import org.openl.rules.table.xls.formatters.XlsStringFormatter;
import org.openl.types.IOpenClass;
import org.openl.util.formatters.IFormatter;

/**
 * @author snshor
 */
public class SimpleFormatFilter implements IGridFilter {

    public static final String GENERAL_XLS_FORMAT = "General";

    private Map<String, IFormatter> existingFormatters = new HashMap<String, IFormatter>();

    public Object parse(String value) {
        throw new UnsupportedOperationException("This format does not parse");
    }

    public IGridSelector getGridSelector() {
        return null;
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        IFormatter cellFormatter = getCellFormatter(cell);
        if (cellFormatter != null) {
            FormatFilter formatFilter = new FormatFilter(cellFormatter);
            Object cellValue = cell.getObjectValue();
            if (cellValue instanceof String) {
                Object cellObjectValue = formatFilter.parse(cellValue.toString());
                cell.setObjectValue(cellObjectValue);
            }
            
            // Try to format cell value.
            //
            FormattedCell formattedCell = formatFilter.filterFormat(cell);
            
            // If cell value is not null and cell formatted value is null then
            // a formatter cannot to format cell value in right way. In this
            // case we are using original string value of cell as formatted
            // value.
            //
            if (cellValue != null && formattedCell.getFormattedValue() == null) {
                formattedCell.setFormattedValue(cellValue.toString());
            }
            
            return formattedCell;
        }
        
        return cell;
    }

    // TODO Move to factory class
    private IFormatter getCellFormatter(ICell cell) {
        AXlsFormatter formatter = null;
        CellMetaInfo cellMetaInfo = cell.getMetaInfo();
        IOpenClass dataType = cellMetaInfo == null ? null : cellMetaInfo.getDataType();
        if (dataType != null) {
            Class<?> instanceClass = dataType.getInstanceClass();
            // Numeric
            if (ClassUtils.isAssignable(instanceClass, Number.class, true)) {
//            if (ClassUtils.isAssignable(instanceClass, double.class, true) // Simple numeric
//                || instanceClass == BigInteger.class || instanceClass == BigDecimal.class) {// Unbounded numeric
                String format = cell.getStyle().getTextFormat();
                AXlsFormatter numberFormatter = findXlsNumberFormatter(format);
                // Numeric Array
                if (cellMetaInfo.isMultiValue()) {
                    formatter = new XlsArrayFormatter(numberFormatter);
                } else {
                    formatter = numberFormatter;
                }
            // Date
            } else if (instanceClass == Date.class) {
                String format = cell.getStyle().getTextFormat();
                formatter = findXlsDateFormatter(format);
            // Boolean
            } else if (instanceClass == boolean.class || instanceClass == Boolean.class) {
                formatter = new XlsBooleanFormatter();
            // Enum
            } else if (instanceClass.isEnum()) {
                AXlsFormatter enumFormatter = new XlsEnumFormatter(instanceClass);
                // Enum Array
                if (cellMetaInfo.isMultiValue()) {
                    formatter = new XlsArrayFormatter(enumFormatter);
                } else {
                    formatter = enumFormatter;
                }
            } else {
                formatter = new XlsStringFormatter();
            }
            // Formula
            if (cell.getFormula() != null) {
                formatter = new XlsFormulaFormatter(formatter);
            }
        }
        return formatter;
    }

    private static boolean isGeneralFormat(String format) {
        return format == null || GENERAL_XLS_FORMAT.equalsIgnoreCase(format);
    }    

    @Deprecated
    private XlsDateFormatter findXlsDateFormatter(String format) {
        if (isGeneralFormat(format)) {
            format = XlsDateFormatter.DEFAULT_XLS_DATE_FORMAT;
        }
        
        IFormatter formatter = existingFormatters.get(format);
        if (formatter instanceof XlsDateFormatter) {
            return (XlsDateFormatter) formatter;
        }

        XlsDateFormatter dateFormat = (XlsDateFormatter) XlsFormattersManager.getFormatter(Date.class, format);
        existingFormatters.put(format, dateFormat);

        return dateFormat;
    }

    @Deprecated
    private XlsNumberFormatter findXlsNumberFormatter(String format) {
        Locale locale = Locale.US;
        if (isGeneralFormat(format)) {
            return XlsNumberFormatter.getGeneralFormatter(locale);
        }

        IFormatter formatter = existingFormatters.get(format);

        if (formatter instanceof XlsNumberFormatter) {
            return (XlsNumberFormatter) formatter;
        }

        XlsNumberFormatter numberFormatter = XlsNumberFormatter.makeFormat(format,
                new HashMap<String, SegmentFormatter>(), locale);
        existingFormatters.put(format, numberFormatter);

        return numberFormatter;
    }

    public IFormatter getFormatter() {
        return null;
    }    

}
