package org.openl.rules.table.xls.formatters;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.formatters.ArrayFormatter;
import org.openl.rules.table.formatters.FormulaFormatter;
import org.openl.rules.table.xls.formatters.SegmentFormatter;
import org.openl.rules.table.xls.formatters.XlsDateFormatter;
import org.openl.rules.table.xls.formatters.XlsNumberFormatter;
import org.openl.types.IOpenClass;
import org.openl.util.formatters.BooleanFormatter;
import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.EnumFormatter;
import org.openl.util.formatters.IFormatter;

public class XlsDataFormatterFactory {

    public static final String GENERAL_XLS_FORMAT = "General";

    private Map<String, IFormatter> existingFormatters = new HashMap<String, IFormatter>();

    public IFormatter getFormatter(ICell cell) {
        IFormatter formatter = null;
        CellMetaInfo cellMetaInfo = cell.getMetaInfo();
        IOpenClass dataType = cellMetaInfo == null ? null : cellMetaInfo.getDataType();
        if (dataType != null) {
            Class<?> instanceClass = dataType.getInstanceClass();

            // Numeric
            if (ClassUtils.isAssignable(instanceClass, Number.class, true)) {
            //if (ClassUtils.isAssignable(instanceClass, double.class, true) // Simple numeric
                //|| instanceClass == BigInteger.class || instanceClass == BigDecimal.class) { // Unbounded numeric
                String format = cell.getStyle().getTextFormat();
                IFormatter numberFormatter = findXlsNumberFormatter(format);
                // Numeric Array
                if (cellMetaInfo.isMultiValue()) {
                    formatter = new ArrayFormatter(numberFormatter);
                } else {
                    formatter = numberFormatter;
                }

            // Date
            } else if (instanceClass == Date.class) {
                String format = cell.getStyle().getTextFormat();
                formatter = findXlsDateFormatter(format);

            // Boolean
            } else if (ClassUtils.isAssignable(instanceClass, Boolean.class, true)) {
                formatter = new BooleanFormatter();

            // Enum
            } else if (instanceClass.isEnum()) {
                IFormatter enumFormatter = new EnumFormatter(instanceClass);
                // Enum Array
                if (cellMetaInfo.isMultiValue()) {
                    formatter = new ArrayFormatter(enumFormatter);
                } else {
                    formatter = enumFormatter;
                }

            } else {
                formatter = new DefaultFormatter();
            }

            // Formula
            if (cell.getFormula() != null) {
                formatter = new FormulaFormatter(formatter);
            }
        }

        return formatter;
    }

    private static boolean isGeneralFormat(String format) {
        return format == null || GENERAL_XLS_FORMAT.equalsIgnoreCase(format);
    }    

    @Deprecated
    private XlsDateFormatter findXlsDateFormatter(String format) {
        if (StringUtils.isBlank(format) || isGeneralFormat(format)) {
            format = XlsDateFormatter.DEFAULT_XLS_DATE_FORMAT;
        }

        IFormatter formatter = existingFormatters.get(format);
        if (formatter instanceof XlsDateFormatter) {
            return (XlsDateFormatter) formatter;
        }

        XlsDateFormatter dateFormat = new XlsDateFormatter(format);
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

}
