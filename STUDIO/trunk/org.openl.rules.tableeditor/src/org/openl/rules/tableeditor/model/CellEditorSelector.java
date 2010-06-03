package org.openl.rules.tableeditor.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.apache.commons.lang.ClassUtils;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IntRangeDomain;
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

            // Simple numeric
            if (ClassUtils.isAssignable(instanceClass, double.class, true)) {
                Number minValue = null;
                Number maxValue = null;
                if (domain == null && !meta.isMultiValue()) {
                    minValue = NumberUtils.getMinValue(instanceClass);
                    maxValue = NumberUtils.getMaxValue(instanceClass);
                } else if (domain instanceof IntRangeDomain) {
                    IntRangeDomain range = (IntRangeDomain) domain;
                    minValue = range.getMin();
                    maxValue = range.getMax();
                }
                result = factory.makeNumericEditor(minValue, maxValue);
            // Unbounded numeric
            } else if (instanceClass == BigInteger.class || instanceClass == BigDecimal.class) {
                result = factory.makeNumericEditor();
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
            }
        }
        
        return result;
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
