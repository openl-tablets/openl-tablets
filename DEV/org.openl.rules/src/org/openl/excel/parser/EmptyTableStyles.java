package org.openl.excel.parser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

/**
 * The styles of a table whose source keeps no formatting: every cell has the default look.
 */
@RequiredArgsConstructor
final class EmptyTableStyles implements TableStyles {

    @Getter
    private final IGridRegion region;

    @Override
    public ICellStyle getStyle(int row, int column) {
        return null;
    }

    @Override
    public ICellFont getFont(int row, int column) {
        return null;
    }

    @Override
    public ICellComment getComment(int row, int column) {
        return null;
    }

    @Override
    public String getFormula(int row, int column) {
        return null;
    }
}
