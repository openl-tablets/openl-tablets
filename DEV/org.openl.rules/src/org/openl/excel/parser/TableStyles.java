package org.openl.excel.parser;

import java.util.List;

import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.TextRun;

/**
 * Needed to retrieve styles, fonts, comments for a given table.
 */
public interface TableStyles {
    IGridRegion getRegion();

    ICellStyle getStyle(int row, int column);

    ICellFont getFont(int row, int column);

    ICellComment getComment(int row, int column);

    String getFormula(int row, int column);

    /**
     * Returns the pieces of a cell text formatted with fonts of their own.
     *
     * @return the runs of the cell text, or an empty list when the text takes the font of its cell
     */
    default List<TextRun> getTextRuns(int row, int column) {
        return List.of();
    }
}
