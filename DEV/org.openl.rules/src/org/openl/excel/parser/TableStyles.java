package org.openl.excel.parser;

import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

/**
 * Needed to retrieve styles, fonts, comments for a given table.
 */
public interface TableStyles {
    IGridRegion getRegion();

    ICellStyle getStyle(int row, int column);

    ICellFont getFont(int row, int column);

    ICellComment getComment(int row, int column);

    String getFormula(int row, int column);
}
