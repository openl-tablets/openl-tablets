package org.openl.rules.table.xls.formatters;

import java.util.Date;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.formatters.ArrayFormatter;
import org.openl.rules.table.formatters.FormulaFormatter;
import org.openl.rules.table.xls.XlsCell;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.util.formatters.BooleanFormatter;
import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.EnumFormatter;
import org.openl.util.formatters.IFormatter;

public class XlsDataFormatterFactory {

    public static final String GENERAL_FORMAT = "General";

    private static Locale locale = Locale.US;
    private static DataFormatter dataFormatter = new DataFormatter(locale);

    public static IFormatter getFormatter(XlsCell cell) {
        IFormatter formatter = null;
        CellMetaInfo cellMetaInfo = cell.getMetaInfo();
        IOpenClass dataType = cellMetaInfo == null ? null : cellMetaInfo.getDataType();
        if (dataType != null) {
            Class<?> instanceClass = dataType.getInstanceClass();

            // Numeric
            if (ClassUtils.isAssignable(instanceClass, Number.class)) {
                IFormatter numberFormatter = getNumberFormatter(cell);
                // Numeric Array
                if (cellMetaInfo.isMultiValue() && numberFormatter != null) {
                    formatter = new ArrayFormatter(numberFormatter);
                } else {
                    formatter = numberFormatter;
                }

            // Date
            } else if (instanceClass == Date.class) {
                formatter = getDateFormatter(cell);

            // Boolean
            } else if (ClassUtils.isAssignable(instanceClass, Boolean.class)) {
                BooleanFormatter booleanFormatter = new BooleanFormatter();
                formatter = cellMetaInfo.isMultiValue() ? new ArrayFormatter(booleanFormatter) : booleanFormatter;

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

    private static IFormatter getNumberFormatter(XlsCell cell) {
        IFormatter formatter = null;

        Cell xlsCell = cell.getXlsCell();
        CellStyle xlsStyle = xlsCell != null ? xlsCell.getCellStyle() : null;

        if (xlsStyle != null) {
            short formatIndex = xlsStyle.getDataFormat();
            String format = xlsStyle.getDataFormatString();
            if (format.contains("#\" \"")) {
                format = format.replaceAll("#\" \"", "# ");
            }
            formatter = new XlsNumberFormatter(formatIndex, format, dataFormatter, locale);
        }

        return formatter;
    }

    public static IFormatter getDateFormatter(XlsCell cell) {
        IFormatter formatter = null;

        Cell xlsCell = cell.getXlsCell();
        CellStyle xlsStyle = xlsCell != null ? xlsCell.getCellStyle() : null;

        if (xlsStyle != null) {
            String format = xlsStyle.getDataFormatString();
            if (StringUtils.isBlank(format) || format.equals(GENERAL_FORMAT)) {
                format = XlsDateFormatter.DEFAULT_XLS_DATE_FORMAT;
            }
            formatter = new XlsDateFormatter(format);
        }

        return formatter;
    }

}
