package org.openl.rules.table.xls.formatters;

import java.time.*;
import java.util.Date;
import java.util.Locale;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ICell;
import org.openl.rules.table.formatters.ArrayFormatter;
import org.openl.rules.table.formatters.FormulaFormatter;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.util.formatters.BooleanFormatter;
import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.EnumFormatter;
import org.openl.util.formatters.IFormatter;

public final class XlsDataFormatterFactory {

    private XlsDataFormatterFactory() {
    }

    private static Locale locale = Locale.US;
    private static DataFormatter dataFormatter = new DataFormatter(locale);

    /**
     * Determine formatter depending on type retrieved from Excel. Warning! Don't invoke this method from core. It can
     * be memory and time consuming operation. Formatting values only from UI is allowed.
     *
     * @param cell formatting cell
     * @param cellMetaInfo meta info for the cell
     * @return found formatter
     */
    public static IFormatter getFormatter(ICell cell, CellMetaInfo cellMetaInfo) {
        IFormatter formatter = null;
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
            } else if (ClassUtils.isAssignable(instanceClass, Date.class) || ClassUtils.isAssignable(instanceClass,
                LocalDateTime.class) || ClassUtils.isAssignable(instanceClass, LocalDate.class) || ClassUtils
                    .isAssignable(instanceClass, LocalTime.class) || ClassUtils.isAssignable(instanceClass,
                        ZonedDateTime.class) || ClassUtils.isAssignable(instanceClass, Instant.class)) {
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
                if (cellMetaInfo.isMultiValue()) {
                    formatter = new ArrayFormatter(formatter);
                }
            }

            // Formula
            if (cell.getFormula() != null) {
                formatter = new FormulaFormatter(formatter);
            }
        }

        return formatter;
    }

    private static IFormatter getNumberFormatter(ICell cell) {
        IFormatter formatter = null;

        ICellStyle xlsStyle = cell == null ? null : cell.getStyle();

        if (xlsStyle != null) {
            short formatIndex = xlsStyle.getFormatIndex();
            String format = xlsStyle.getFormatString();
            if (format.contains("#\" \"")) {
                format = format.replaceAll("#\" \"", "# ");
            }
            formatter = new XlsNumberFormatter(formatIndex, format, dataFormatter, locale);
        }

        return formatter;
    }

    private static IFormatter getDateFormatter(ICell cell) {
        IFormatter formatter = null;

        ICellStyle xlsStyle = cell == null ? null : cell.getStyle();

        if (xlsStyle != null) {
            String format = xlsStyle.getFormatString();
            if (StringUtils.isBlank(format) || format.equals(FormatConstants.GENERAL_FORMAT)) {
                format = FormatConstants.DEFAULT_XLS_DATE_FORMAT;
            }
            formatter = new XlsDateFormatter(format);
        }

        return formatter;
    }

    /**
     * Get formatted value in a user-friendly format Warning! Don't invoke this method from core. It can be memory and
     * time consuming operation. Formatting values only from UI is allowed.
     *
     * @param cell formatting cell
     * @param meta meta info for the cell
     * @return Formatted string value
     */
    public static String getFormattedValue(ICell cell, CellMetaInfo meta) {
        if (cell instanceof FormattedCell) {
            return ((FormattedCell) cell).getFormattedValue();
        }

        Object value = cell.getObjectValue();

        String formattedValue = null;
        if (value != null) {
            IFormatter cellDataFormatter = getFormatter(cell, meta);

            if (cellDataFormatter == null && value instanceof Date) {
                // Cell type is unknown but in Excel it's stored as a Date.
                // We cannot override getDataFormatter() or XlsDataFormatterFactory.getFormatter() to support this case
                // because they are also invoked when editing a cell. When editing cells with unknown type null must be
                // returned to be able to edit such cell as if it can contain any text.
                // But we can safely format it's value when just viewing it's value.
                cellDataFormatter = getDateFormatter(cell);
            }

            if (cellDataFormatter != null) {
                formattedValue = cellDataFormatter.format(value);
            }
        }

        if (formattedValue == null) {
            formattedValue = cell.getStringValue();
            if (formattedValue == null) {
                formattedValue = StringUtils.EMPTY;
            }
        }

        return formattedValue;
    }
}
