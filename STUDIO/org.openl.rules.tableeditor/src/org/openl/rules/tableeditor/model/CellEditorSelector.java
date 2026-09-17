package org.openl.rules.tableeditor.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.formatters.ArrayFormatter;
import org.openl.rules.table.xls.formatters.XlsDataFormatterFactory;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.EnumUtils;
import org.openl.util.IntegerValuesUtils;
import org.openl.util.NumberUtils;
import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.IFormatter;

/** Picks the editor a cell is written with from what the compiler knows about the cell. */
public class CellEditorSelector {

    public ICellEditor selectEditor(ICell cell, CellMetaInfo meta) {
        if (cell.getFormula() != null) {
            return new FormulaCellEditor();
        }
        var editor = selectEditor(cell, cell.getStringValue(), meta);
        return editor == null ? defaultEditor(cell) : editor;
    }

    private ICellEditor selectEditor(ICell cell, String initialValue, CellMetaInfo meta) {
        ICellEditor result = null;
        IOpenClass dataType = meta == null ? null : meta.getDataType();
        if (dataType != null) {
            if (CellMetaInfo.isCellContainsNodeUsages(meta)) {
                return defaultEditor(cell);
            }
            IDomain<?> domain = dataType.getDomain();
            Class<?> instanceClass = dataType.getInstanceClass();

            if (domain instanceof EnumDomain<?> enumDomain) {
                var allObjects = enumDomain.getAllObjects();

                if (allObjects instanceof String[] allObjectValues) {
                    return choiceEditor(allObjectValues, allObjectValues, meta.isMultiValue());
                } else if (allObjects != null) {
                    IFormatter formatter = XlsDataFormatterFactory.getFormatter(cell, meta);
                    if (formatter instanceof ArrayFormatter arrayFormatter) {
                        // We need a formatter for each element of an array.
                        formatter = arrayFormatter.getElementFormat();
                        if (formatter == null) {
                            formatter = new DefaultFormatter();
                        }
                    }

                    String[] allObjectValues = new String[allObjects.length];
                    for (var i = 0; i < allObjects.length; i++) {
                        var value = allObjects[i];
                        allObjectValues[i] = value instanceof String s ? s : formatter.format(value);
                    }

                    return choiceEditor(allObjectValues, allObjectValues, meta.isMultiValue());
                }
            }

            // Numeric
            if (ClassUtils.isAssignable(instanceClass, Number.class)) {
                if (domain == null) {
                    var intOnly = IntegerValuesUtils.isIntegerValue(instanceClass);
                    if (!meta.isMultiValue()) {
                        Number minValue = NumberUtils.getMinValue(instanceClass);
                        Number maxValue = NumberUtils.getMaxValue(instanceClass);
                        result = new NumericCellEditor(minValue, maxValue, intOnly);
                    } else {
                        // Numeric Array
                        return new ArrayCellEditor(ArrayCellEditor.DEFAULT_SEPARATOR, ICellEditor.CE_NUMERIC, intOnly);
                    }
                }

                // Date
            } else if (ClassUtils.isAssignable(instanceClass, Date.class)
                    || ClassUtils.isAssignable(instanceClass, LocalDate.class)
                    || ClassUtils.isAssignable(instanceClass, LocalDateTime.class)
                    || ClassUtils.isAssignable(instanceClass, LocalTime.class)
                    || ClassUtils.isAssignable(instanceClass, ZonedDateTime.class)
                    || ClassUtils.isAssignable(instanceClass, Instant.class)) {
                result = new DateCellEditor();

                // Boolean
            } else if (ClassUtils.isAssignable(instanceClass, Boolean.class)) {
                result = new BooleanCellEditor();

                // Enum
            } else if (instanceClass.isEnum()) {
                result = choiceEditor(EnumUtils.getNames(instanceClass),
                        EnumUtils.getValues(instanceClass),
                        meta.isMultiValue());
                // Range
            } else if (ClassUtils.isAssignable(instanceClass, IntRange.class) && DecisionTableHelper
                    .parsableAs(initialValue, instanceClass, null)) {
                result = new NumberRangeEditor(ICellEditor.CE_INTEGER, initialValue);
            } else if (ClassUtils.isAssignable(instanceClass, DoubleRange.class) && DecisionTableHelper
                    .parsableAs(initialValue, instanceClass, null)) {
                result = new NumberRangeEditor(ICellEditor.CE_DOUBLE, initialValue);
            }
        }
        return result;
    }

    /** One choice among the given values, or several of them when the cell holds many. */
    private static ICellEditor choiceEditor(String[] choices, String[] displayValues, boolean multiValue) {
        return multiValue ? new MultiSelectCellEditor(choices, displayValues)
                : new ComboBoxCellEditor(choices, displayValues);
    }

    private ICellEditor defaultEditor(ICell cell) {
        final var cellValue = cell.getStringValue();
        return cellValue != null && cellValue.indexOf('\n') >= 0 ? new MultilineEditor() : new TextCellEditor();
    }

}
