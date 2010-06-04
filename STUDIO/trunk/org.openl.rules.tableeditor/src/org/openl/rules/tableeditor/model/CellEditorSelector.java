package org.openl.rules.tableeditor.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.apache.commons.lang.ClassUtils;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.rules.lang.xls.types.CellMetaInfo; //import org.openl.rules.helpers.IntRange;
import org.openl.rules.table.ICell;
import org.openl.util.EnumUtils;
import org.openl.util.NumberUtils;
import org.openl.types.IOpenClass;

// TODO Reimplement
public class CellEditorSelector {

    private ICellEditorFactory factory = new CellEditorFactory();

    private ICellEditor defaultEditor(int row, int col, TableEditorModel model) {
        final String s = model.getCell(row, col).getStringValue();
        return s != null && s.indexOf('\n') >= 0 ? factory.makeMultilineEditor() : factory.makeTextEditor();
    }

    @SuppressWarnings("unchecked")
    private ICellEditor selectEditor(CellMetaInfo meta) {
        ICellEditor result = null;
        IOpenClass dataType = meta == null ? null : meta.getDataType();
        if (dataType != null) {
            IDomain domain = dataType.getDomain();
            Class<?> instanceClass = dataType.getInstanceClass();

            // Numeric
            //if (ClassUtils.isAssignable(instanceClass, Number.class, true)) {
            if (ClassUtils.isAssignable(instanceClass, double.class, true) // Simple numeric
                || instanceClass == BigInteger.class || instanceClass == BigDecimal.class) {// Unbounded numeric
                if (domain == null && !meta.isMultiValue()) {
                    Number minValue = NumberUtils.getMinValue(instanceClass);
                    Number maxValue = NumberUtils.getMaxValue(instanceClass);
                    result = factory.makeNumericEditor(minValue, maxValue);
                }
            // String
            } else if (instanceClass == String.class) {
                if (domain instanceof EnumDomain) {
                    EnumDomain enumDomain = (EnumDomain) domain;
                    if (meta.isMultiValue()) {
                        result = factory.makeMultiSelectEditor((String[]) enumDomain.getEnum().getAllObjects());
                    } else {
                        result = factory.makeComboboxEditor((String[]) enumDomain.getEnum().getAllObjects());
                    }
                }
            // Date
            } else if (instanceClass == Date.class) {
                result = factory.makeDateEditor();
            // Boolean
            } else if (instanceClass == boolean.class || instanceClass == Boolean.class) {
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
            } else if (instanceClass.isArray()) {
                result = selectArrayEditor(instanceClass.getComponentType());
            }
        }

        return result;
    }

    private ICellEditor selectArrayEditor(Class<?> instanceClass) {
        if (ClassUtils.isAssignable(instanceClass, double.class, true)
            || instanceClass == BigInteger.class || instanceClass == BigDecimal.class) {
            return factory.makeArrayEditor(",", ICellEditor.CE_NUMERIC);
        }
        return null;
    }

    public ICellEditor selectEditor(int row, int col, TableEditorModel model) {
        ICell cell = model.getCell(row, col);
        if (cell != null && cell.getFormula() != null) {
            return factory.makeFormulaEditor();
        }
        ICellEditor editor = selectEditor(model.getCellMetaInfo(row, col));
        return editor == null ? defaultEditor(row, col, model) : editor;
    }

}
