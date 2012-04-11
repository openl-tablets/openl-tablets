package org.openl.rules.tableeditor.model;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.lang.xls.types.CellMetaInfo; //import org.openl.rules.helpers.IntRange;
import org.openl.types.IOpenClass;

public class CellEditorSelector {
    private ICellEditorFactory factory = new CellEditorFactory();

    private ICellEditor defaultEditor(int row, int col, TableEditorModel model) {
        final String s = model.getCellValue(row, col);
        return s != null && s.indexOf('\n') >= 0 ? factory.makeMultilineEditor() : factory.makeTextEditor();
    }

    @SuppressWarnings("unchecked")
    private ICellEditor selectEditor(CellMetaInfo meta) {
        IOpenClass dataType = meta == null ? null : meta.getDataType();

        if (dataType == null) {
            return null;
        }
        IDomain domain = dataType.getDomain();

        if (dataType.getInstanceClass() == int.class /*
                                                         * ||
                                                         * dataType.getInstanceClass() ==
                                                         * IntRange.class
                                                         */) {

            if (domain == null) {
                return factory.makeIntEditor(Integer.MIN_VALUE, Integer.MAX_VALUE);
            }

            if (domain instanceof IntRangeDomain) {
                IntRangeDomain range = (IntRangeDomain) domain;
                return factory.makeIntEditor(range.getMin(), range.getMax());
            }
        }

        if (dataType.getInstanceClass() == String.class) {

            if (domain instanceof EnumDomain) {
                EnumDomain enumDomain = (EnumDomain) domain;
                return factory.makeComboboxEditor((String[]) enumDomain.getEnum().getAllObjects());
            }

        }

        return null;
    }

    public ICellEditor selectEditor(int row, int col, TableEditorModel model) {
        ICellEditor editor = selectEditor(model.getCellMetaInfo(row, col));
        return editor == null ? defaultEditor(row, col, model) : editor;
    }

}
