package org.openl.rules.table;

import org.apache.poi.ss.usermodel.HorizontalAlignment;

import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;

/**
 * @author snshor
 */
public interface IWritableGrid extends IGrid {

    int addMergedRegion(IGridRegion reg);

    void clearCell(int col, int row);

    void createCell(int col, int row, Object value, String formula, ICellStyle style, String comment, String prevCommentAuthor);

    void copyCell(int colFrom, int rowFrom, int colTo, int rowTo);

    /**
     * Finds a rectangular area of given width and height on the grid that can be used for writing. The returned region
     * should not intersect with or touch existing not empty cells.
     *
     * @param width  rectangle width
     * @param height rectangle height
     * @return region representing required rectangle or <code>null</code> if not found
     */
    IGridRegion findEmptyRect(int width, int height);

    void removeMergedRegion(IGridRegion to);

    void removeMergedRegion(int x, int y);

    void setCellStyle(int col, int row, ICellStyle style);

    void setCellBorderStyle(int col, int row, ICellStyle style);

    void setCellAlignment(int col, int row, HorizontalAlignment alignment);

    void setCellIndent(int col, int row, int indent);

    void setCellFillColor(int col, int row, short[] color);

    /**
     * Fills a cell with a theme colour of Excel, made lighter or darker.
     *
     * <p>A workbook of a theme takes the theme colour itself, so the cell takes the colours of its theme. A workbook
     * without a theme, such as an {@code .xls} one, takes the colour Office draws the theme colour in.
     *
     * @param col   the column of the cell
     * @param row   the row of the cell
     * @param color the theme colour
     */
    void setCellFillColor(int col, int row, ThemedColor color);

    void setCellFontBold(int col, int row, boolean bold);

    void setCellFontItalic(int col, int row, boolean italic);

    void setCellFontUnderline(int col, int row, boolean underlined);

    void setCellFontColor(int col, int row, short[] color);

    /**
     * Colours the text of a cell with a theme colour of Excel, made lighter or darker, as
     * {@link #setCellFillColor(int, int, ThemedColor)} fills it.
     *
     * @param col   the column of the cell
     * @param row   the row of the cell
     * @param color the theme colour
     */
    void setCellFontColor(int col, int row, ThemedColor color);

    void setCellComment(int col, int row, String comment, String prevCommentAuthor);

    void setCellValue(int col, int row, Object value);

    void setCellStringValue(int col, int row, String value);

    void setCellFormula(int col, int row, String formula);
}
