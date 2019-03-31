package org.openl.rules.tableeditor.model;

import java.util.Date;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.rules.helpers.CharRange;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.INumberRange;
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

// TODO Reimplement
public class CellEditorSelector {

    private ICellEditorFactory factory = new CellEditorFactory();

    public ICellEditor selectEditor(ICell cell, CellMetaInfo meta) {
        if (cell.getFormula() != null) {
            return factory.makeFormulaEditor();
        }
        ICellEditor editor = selectEditor(cell, cell.getStringValue(), meta);
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

            if (domain instanceof EnumDomain) {
                Object[] allObjects = ((EnumDomain<?>) domain).getEnum().getAllObjects();

                if (allObjects instanceof String[]) {
                    String[] allObjectValues = (String[]) allObjects;

                    if (meta.isMultiValue()) {
                        return factory.makeMultiSelectEditor(allObjectValues);
                    } else {
                        return factory.makeComboboxEditor(allObjectValues);
                    }
                } else if (allObjects != null) {
                    IFormatter formatter = XlsDataFormatterFactory.getFormatter(cell, meta);
                    if (formatter instanceof ArrayFormatter) {
                        // We need a formatter for each element of an array.
                        formatter = ((ArrayFormatter) formatter).getElementFormat();
                        if (formatter == null) {
                            formatter = new DefaultFormatter();
                        }
                    }

                    String[] allObjectValues = new String[allObjects.length];
                    for (int i = 0; i < allObjects.length; i++) {
                        Object value = allObjects[i];
                        allObjectValues[i] = value instanceof String ? (String) value : formatter.format(value);
                    }

                    if (meta.isMultiValue()) {
                        return factory.makeMultiSelectEditor(allObjectValues);
                    } else {
                        return factory.makeComboboxEditor(allObjectValues);
                    }
                }
            }

            // Numeric
            if (ClassUtils.isAssignable(instanceClass, Number.class)) {
                if (domain == null) {
                    if (!meta.isMultiValue()) {
                        Number minValue = NumberUtils.getMinValue(instanceClass);
                        Number maxValue = NumberUtils.getMaxValue(instanceClass);
                        result = factory
                            .makeNumericEditor(minValue, maxValue, IntegerValuesUtils.isIntegerValue(instanceClass));
                    } else {
                        // Numeric Array
                        return factory.makeArrayEditor(ArrayCellEditor.DEFAULT_SEPARATOR,
                            ICellEditor.CE_NUMERIC,
                            IntegerValuesUtils.isIntegerValue(instanceClass));
                    }
                }

                // Date
            } else if (instanceClass == Date.class) {
                result = factory.makeDateEditor();

                // Boolean
            } else if (ClassUtils.isAssignable(instanceClass, Boolean.class)) {
                result = factory.makeBooleanEditor();

                // Enum
            } else if (instanceClass.isEnum()) {
                String[] values = EnumUtils.getNames(instanceClass);
                String[] displayValues = EnumUtils.getValues(instanceClass);

                if (meta.isMultiValue()) {
                    result = factory.makeMultiSelectEditor(values, displayValues);
                } else {
                    result = factory.makeComboboxEditor(values, displayValues);
                }

                // Range
            } else if (ClassUtils.isAssignable(instanceClass,
                INumberRange.class) && (!instanceClass.equals(CharRange.class))) {
                if (ClassUtils.isAssignable(instanceClass, IntRange.class)) {
                    result = factory.makeNumberRangeEditor(ICellEditor.CE_INTEGER, initialValue);
                } else if (ClassUtils.isAssignable(instanceClass, DoubleRange.class)) {
                    result = factory.makeNumberRangeEditor(ICellEditor.CE_DOUBLE, initialValue);
                }
            }

        }

        return result;
    }

    private ICellEditor defaultEditor(ICell cell) {
        final String cellValue = cell.getStringValue();
        return cellValue != null && cellValue.indexOf('\n') >= 0 ? factory.makeMultilineEditor()
                                                                 : factory.makeTextEditor();
    }

}
