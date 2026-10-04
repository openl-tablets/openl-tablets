package org.openl.studio.projects.service.tables.theme;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The look the theme gives each cell of one table.
 *
 * <p>Cells are named by their place on the sheet, not in the table. Every cell of a merged region is listed, so
 * a border reaches the whole region. A cell the theme does not reach is not listed and keeps its look.
 *
 * @param cells the look of each cell the theme reaches
 */
public record ThemedTable(Map<Cell, ThemedCell> cells) {

    /**
     * The look of a cell, or {@code null} when the theme does not reach it.
     *
     * @param row    the row of the cell on the sheet
     * @param column the column of the cell on the sheet
     */
    public @Nullable ThemedCell at(int row, int column) {
        return cells.get(new Cell(row, column));
    }

    /**
     * The place of a cell on the sheet.
     *
     * @param row    the row of the cell on the sheet
     * @param column the column of the cell on the sheet
     */
    public record Cell(int row, int column) {
    }

    /**
     * The look the theme gives one cell.
     *
     * @param style  the look of the cell
     * @param header the look of the pieces of the header text, or {@code null} for any cell but the one holding
     *               the header text
     */
    public record ThemedCell(ThemeStyle style, TableTheme.@Nullable Header header) {

        /**
         * The pieces the theme formats the cell text in.
         *
         * @param text the text of the cell
         * @return the pieces of the text, or an empty list when the theme keeps one font for the whole text
         */
        public List<ThemedRun> runs(@Nullable String text) {
            return header == null || text == null ? List.of() : HeaderRuns.split(text, style, header);
        }

        /**
         * Whether the text keeps the pieces the workbook formats it in. It does when the theme formats it in no
         * pieces of its own and names nothing of the font of the cell; otherwise the text is drawn and written in the
         * font of its cell.
         *
         * @param runs the pieces the theme formats the text in
         * @return {@code true} when the pieces of the workbook stay
         */
        public boolean keepsOwnRuns(List<ThemedRun> runs) {
            return runs.isEmpty() && !style.hasFont();
        }
    }

    /**
     * A piece of a cell text and the look the theme gives it.
     *
     * @param start the index of the first character of the piece
     * @param end   the index after the last character of the piece
     * @param style the look of the piece: the cell look with the look of its part laid over it
     */
    public record ThemedRun(int start, int end, ThemeStyle style) {
    }
}
