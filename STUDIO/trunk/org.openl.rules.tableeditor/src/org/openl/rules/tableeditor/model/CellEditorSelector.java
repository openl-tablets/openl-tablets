package org.openl.rules.tableeditor.model;

import java.util.Date;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.lang.xls.types.CellMetaInfo; //import org.openl.rules.helpers.IntRange;
import org.openl.rules.table.ICell;
import org.openl.util.EnumUtils;
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
            if (instanceClass == int.class || instanceClass == Integer.class) {
                if (domain == null && !meta.isMultiValue()) {
                    result = factory.makeIntEditor(Integer.MIN_VALUE, Integer.MAX_VALUE);
                } else if (domain instanceof IntRangeDomain) {
                    IntRangeDomain range = (IntRangeDomain) domain;
                    result = factory.makeIntEditor(range.getMin(), range.getMax());
                } else if (meta.isMultiValue()) {
                    factory.makeTextEditor();
                }
            } else if (instanceClass == String.class) {
                if (domain instanceof EnumDomain) {
                    EnumDomain enumDomain = (EnumDomain) domain;
                    if (meta.isMultiValue()) {
                        result = factory.makeMultiSelectEditor((String[]) enumDomain.getEnum().getAllObjects());
                    } else {
                        result = factory.makeComboboxEditor((String[]) enumDomain.getEnum().getAllObjects());
                    }
                }
            } else if (instanceClass == Date.class) {
                result = factory.makeDateEditor();
            } else if (instanceClass == boolean.class || instanceClass == Boolean.class) {
                result = factory.makeBooleanEditor();
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
