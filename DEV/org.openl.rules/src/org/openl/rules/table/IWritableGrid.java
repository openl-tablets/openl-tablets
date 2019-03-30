package org.openl.rules.table;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.openl.rules.table.ui.ICellStyle;

/**
 * @author snshor
 * 
 */
public interface IWritableGrid extends IGrid {

    int addMergedRegion(IGridRegion reg);

    void clearCell(int col, int row);

    void createCell(int col, int row, Object value, String formula, ICellStyle style, ICellComment comment);

    void copyCell(int colFrom, int rowFrom, int colTo, int rowTo);

    /**
     * Finds a rectangular area of given width and height on the grid that can
     * be used for writing. The returned region should not intersect with or
     * touch existing not empty cells.
     * 
     * @param width rectangle width
     * @param height rectangle height
     * @return region representing required rectangle or <code>null</code> if
     *         not found
     */
    IGridRegion findEmptyRect(int width, int height);

    void removeMergedRegion(IGridRegion to);

    void removeMergedRegion(int x, int y);

    void setCellStyle(int col, int row, ICellStyle style);
    
    void setCellBorderStyle(int col, int row, ICellStyle style);

    void setCellAlignment(int col, int row, HorizontalAlignment alignment);

    void setCellIndent(int col, int row, int indent);

    void setCellFillColor(int col, int row, short[] color);

    void setCellFontBold(int col, int row, boolean bold);

    void setCellFontItalic(int col, int row, boolean italic);

    void setCellFontUnderline(int col, int row, boolean underlined);

    void setCellFontColor(int col, int row, short[] color);

    void setCellComment(int col, int row, ICellComment comment);

    void setCellValue(int col, int row, Object value);

    void setCellStringValue(int col, int row, String value);

    void setCellFormula(int col, int row, String formula);
}
