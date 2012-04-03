package org.openl.rules.table.xls.formatters;

import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.formatters.ArrayFormatter;
import org.openl.rules.table.formatters.FormulaFormatter;
import org.openl.rules.table.xls.XlsCell;
import org.openl.rules.table.xls.formatters.XlsDateFormatter;
import org.openl.rules.table.xls.formatters.XlsNumberFormatter;
import org.openl.types.IOpenClass;
import org.openl.util.formatters.BooleanFormatter;
import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.EnumFormatter;
import org.openl.util.formatters.IFormatter;

public class XlsDataFormatterFactory {

    public static final String GENARAL_FORMAT = "General";

    private DataFormatter dataFormatter;
    private Locale locale;

    public XlsDataFormatterFactory() {
        this(null);
    }

    public XlsDataFormatterFactory(Locale locale) {
        this.locale = locale;
        this.dataFormatter = new DataFormatter(locale);
    }

    public IFormatter getFormatter(ICell cell) {
        IFormatter formatter = null;
        CellMetaInfo cellMetaInfo = cell.getMetaInfo();
        IOpenClass dataType = cellMetaInfo == null ? null : cellMetaInfo.getDataType();
        if (dataType != null) {
            Class<?> instanceClass = dataType.getInstanceClass();

            // Numeric
            if (ClassUtils.isAssignable(instanceClass, Number.class, true)) {
                IFormatter numberFormatter = getNumberFormatter(cell);
                // Numeric Array
                if (cellMetaInfo.isMultiValue()) {
                    formatter = new ArrayFormatter(numberFormatter);
                } else {
                    formatter = numberFormatter;
                }

            // Date
            } else if (instanceClass == Date.class) {
                formatter = getDateFormatter(cell);

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

    private IFormatter getNumberFormatter(ICell cell) {
        IFormatter formatter = null;

        CellStyle xlsStyle = ((XlsCell) cell).getXlsCell().getCellStyle();

        if (xlsStyle != null) {
            short formatIndex = xlsStyle.getDataFormat();
            String format = xlsStyle.getDataFormatString();

            formatter = new XlsNumberFormatter(
                    formatIndex, format, dataFormatter, locale);

        }

        return formatter;
    }

    private IFormatter getDateFormatter(ICell cell) {
        IFormatter formatter = null;

        CellStyle xlsStyle = ((XlsCell) cell).getXlsCell().getCellStyle();

        if (xlsStyle != null) {
            String format = xlsStyle.getDataFormatString();
            if (StringUtils.isBlank(format) || format.equals(GENARAL_FORMAT)) {
                format = XlsDateFormatter.DEFAULT_XLS_DATE_FORMAT;
            }
            formatter = new XlsDateFormatter(format);
        }

        return formatter;
    }

}
