package org.openl.studio.projects.service.tables.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.Apostrophes.Quote;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableSignaturePart;
import org.openl.studio.projects.service.tables.format.WorkbookStyles.Edges;

/**
 * Writes the OpenL table style into the workbooks of tables.
 *
 * <p>Each cell of a table gets a cell format that links the built-in style of its role and draws the lines at the edges
 * of its region: a line above and below the signature, the properties and the code, above the titles and a section,
 * above a group, above and below the result, and below the body. What the cell had is kept: its number format, its
 * indent, its text wrapping and its text rotation. No value of a cell is changed.
 *
 * <p>The signature is the only cell written as rich text, in its pieces: the name of the table bold, the names of the
 * parameters in the text colour, and the rest lighter. Rich text in any other cell keeps its marks, each bold in its
 * colour in the font of the cell; every other piece takes the font of its cell.
 *
 * <p>A table of an {@code .xls} workbook, and a table of the type Other, are left as they are. One formatter serves a
 * batch of tables: what it writes into a workbook for one table is reused by the others.
 *
 * @author Yury Molchan
 */
@Slf4j
public final class ExcelTableFormatter {

    private final Map<XSSFWorkbook, WorkbookStyles> workbooks = new IdentityHashMap<>();

    /**
     * Writes the style into a table, as it stands on its grid. The caller opens the sheet for writing and saves the
     * workbook.
     *
     * @param table the table
     * @param grid  the table as it stands on its sheet, header included
     * @return whether the table was formatted: a table of the type Other, or of an {@code .xls} workbook, is not
     */
    public boolean format(IOpenLTable table, IGridTable grid) {
        var regions = TableRegionLayout.regionsOf(table, grid);
        return regions != null && format(grid, regions);
    }

    /**
     * Writes the style into a table laid out in the given regions, as when a table is saved with no compile. The
     * caller opens the sheet for writing and saves the workbook.
     *
     * @param grid    the table as it stands on its sheet, header included
     * @param regions the regions of the table, in table coordinates
     * @return whether the table was formatted: a table of an {@code .xls} workbook is not
     */
    public boolean format(IGridTable grid, TableRegions regions) {
        var model = (XlsSheetGridModel) grid.getGrid();
        return format(model.getSheetToWrite(), model, grid.getRegion(), regions, Lines.WHOLE);
    }

    /**
     * Writes the style into a table standing on a sheet, laid out in the given regions, as when a table is written
     * into a workbook of its own.
     *
     * @param sheet   the sheet the table stands on
     * @param table   where the table stands on the sheet
     * @param regions the regions of the table, in table coordinates
     * @return whether the table was formatted: a table of an {@code .xls} workbook is not
     */
    public boolean format(Sheet sheet, IGridRegion table, TableRegions regions) {
        return format(sheet, null, table, regions, Lines.WHOLE);
    }

    /**
     * Writes the style into the given lines of a table standing on a sheet.
     *
     * @param model the grid model the sheet is read through, which keeps the merged regions of the sheet, or
     *              {@code null} for a sheet read through none
     */
    boolean format(Sheet sheet, @Nullable XlsSheetGridModel model, IGridRegion table, TableRegions regions,
                   Lines lines) {
        if (!(sheet instanceof XSSFSheet xssf)) {
            return false;
        }
        var styles = workbooks.computeIfAbsent(xssf.getWorkbook(), WorkbookStyles::new);
        new TableWrite(xssf, model, table, regions, styles, lines).write();
        return true;
    }

    /**
     * Formats a table being saved in its layout, fitted to the table as the save leaves it, every line of it. A
     * formatting that fails is logged and leaves the table as it is written, so the save goes on.
     *
     * @param grid   the table as it stands on its sheet, header included
     * @param layout the regions of the table, moved along the lines the save inserted and removed
     * @return the regions the table stands formatted in, or {@code null} when it is saved unformatted
     */
    public static @Nullable TableRegions formatOnSave(IGridTable grid, RegionShift layout) {
        return formatOnSave(grid, layout, null, false, null);
    }

    /**
     * The regions of a layout fitted to a table being saved. A fitting that fails is logged, so the save goes on.
     *
     * @param grid   the table as it stands on its sheet, header included
     * @param layout the regions of the table, moved along the lines the save inserted and removed
     * @return the regions fitted to the table, or {@code null} when they cannot be fitted
     */
    public static @Nullable TableRegions fittedTo(IGridTable grid, RegionShift layout) {
        try {
            return layout.fittedTo(grid);
        } catch (RuntimeException e) {
            log.warn("The table at {} is saved unformatted.", grid.getUri(), e);
            return null;
        }
    }

    /**
     * Formats a table being saved in its layout, fitted to the table as the save leaves it, or in the regions the table
     * was bound in as it stands, when it was. A formatting that fails is logged and leaves the table as it is written,
     * so the save goes on.
     *
     * <p>A table formatted before, in the regions the edit started from, needs only the lines the edit touched: the
     * lines it inserted or restyled, and the lines next to where it inserted or removed some. Its header is written
     * in its pieces again, as a value written into it is plain text. A header and properties that came to take
     * another number of rows, and bound regions of other roles than the layout gives, as an edit of a header can
     * give, have the table formatted whole.
     *
     * @param grid      the table as it stands on its sheet, header included
     * @param layout    the regions of the table, moved along the lines the save inserted and removed
     * @param shifted   the layout already fitted to the table, or {@code null} to fit it here
     * @param formatted whether the table stands formatted in the regions the edit started from
     * @param bound     the regions of the table bound as it stands, or {@code null} when it was not
     * @return the regions the table stands formatted in, or {@code null} when it is saved unformatted
     */
    public static @Nullable TableRegions formatOnSave(IGridTable grid, RegionShift layout,
                                                      @Nullable TableRegions shifted, boolean formatted,
                                                      @Nullable TableRegions bound) {
        try {
            var fitted = shifted != null ? shifted : layout.fittedTo(grid);
            var regions = bound == null ? fitted : bound;
            var sameRoles = bound == null || TableRegionLayout.sameRoles(bound, fitted);
            var lines = formatted && !layout.bodyMoved() && sameRoles
                    ? new Lines(false, layout.touchedRows(), layout.touchedColumns())
                    : Lines.WHOLE;
            var model = (XlsSheetGridModel) grid.getGrid();
            new ExcelTableFormatter().format(model.getSheetToWrite(), model, grid.getRegion(), regions, lines);
            return regions;
        } catch (RuntimeException e) {
            log.warn("The table at {} is saved unformatted.", grid.getUri(), e);
            return null;
        }
    }

    /**
     * The lines of a table to format: every line, or the given rows across the table and columns down the table.
     *
     * @param all     whether every line is formatted
     * @param rows    the rows, in table coordinates, unless every line is
     * @param columns the columns, in table coordinates, unless every line is
     */
    record Lines(boolean all, NavigableSet<Integer> rows, NavigableSet<Integer> columns) {

        /** Every line of a table. */
        static final Lines WHOLE = new Lines(true, Collections.emptyNavigableSet(), Collections.emptyNavigableSet());

        /** Whether any cell is to be formatted. */
        boolean reachCells() {
            return all || !rows.isEmpty() || !columns.isEmpty();
        }
    }

    /**
     * Removes the cell formats no cell of a workbook uses, and the named styles no cell format links. The cells, the
     * rows and the columns keep the formats they had.
     *
     * <p>It reads every cell of the workbook, so only a formatting of the whole workbook runs it.
     *
     * @param workbook the workbook
     */
    public static void removeUnusedStyles(XSSFWorkbook workbook) {
        UnusedStyles.remove(workbook);
    }

    /**
     * The write of one table.
     *
     * @param sheet   the sheet the table stands on
     * @param table   where the table stands on the sheet
     * @param regions the regions of the table
     * @param styles  the style in the workbook
     * @param lines   the lines of the table to format
     */
    private record TableWrite(XSSFSheet sheet, @Nullable XlsSheetGridModel model, IGridRegion table,
                              TableRegions regions, WorkbookStyles styles,
                              Lines lines) {

        void write() {
            if (lines.reachCells()) {
                writeCells();
            }
            // A value written into the header is plain text, which no line touched tells: it is written every time.
            mergeSignature();
            writeSignature();
        }

        /**
         * Merges the header into one cell across the table: the header is the whole first row of a table. A row whose
         * other cells hold a value, or that a merged region reaches out of, is left as it is, so no value is lost and
         * no other merge is cut; a merge of a part of the row is replaced by the whole row.
         *
         * <p>A row under a block of filled cells that ends right above it and starts over one of its cells after the
         * first is left as it is too. The merged header would join that block to the table, and OpenL would read both
         * as one table, which no longer starts with the header.
         */
        private void mergeSignature() {
            var top = table.getTop();
            var left = table.getLeft();
            var right = table.getRight();
            if (left == right || holdsValuesBesideHeader(top, left, right)) {
                return;
            }
            var inRow = new ArrayList<CellRangeAddress>();
            for (var merged : sheet.getMergedRegions()) {
                if (merged.getFirstRow() <= top && top <= merged.getLastRow() && merged.getFirstColumn() <= right
                        && left <= merged.getLastColumn()) {
                    if (merged.getFirstRow() != top || merged.getLastRow() != top || merged.getFirstColumn() < left
                            || merged.getLastColumn() > right) {
                        return;
                    }
                    inRow.add(merged);
                }
            }
            if (inRow.size() == 1 && inRow.getFirst().getFirstColumn() == left
                    && inRow.getFirst().getLastColumn() == right) {
                return;
            }
            if (joinsBlockAbove(top, left, right)) {
                return;
            }
            var whole = new GridRegion(top, left, top, right);
            if (model != null) {
                // The grid model keeps the merged regions of the sheet: it is told of each change.
                inRow.forEach(merged -> model.removeMergedRegion(merged.getFirstColumn(), merged.getFirstRow()));
                model.addMergedRegion(whole);
            } else {
                for (var merged : inRow) {
                    sheet.removeMergedRegion(indexOf(merged));
                }
                sheet.addMergedRegion(new CellRangeAddress(top, top, left, right));
            }
        }

        /** Whether a cell of the header row after its first one holds a value. */
        private boolean holdsValuesBesideHeader(int top, int left, int right) {
            var row = sheet.getRow(top);
            if (row == null) {
                return false;
            }
            for (var column = left + 1; column <= right; column++) {
                var cell = row.getCell(column);
                if (cell != null && cell.getCellType() != CellType.BLANK
                        && !(cell.getCellType() == CellType.STRING && cell.getStringCellValue().isBlank())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Whether the merged header would join the table to a block of cells above it, as OpenL finds the tables of a
         * sheet: a block that ends right above the header and starts over one of its cells after the first.
         *
         * <p>A block above that already touches the first cell of the header is joined to the table with no merge. The
         * merge changes nothing for it.
         */
        private boolean joinsBlockAbove(int top, int left, int right) {
            if (top == 0) {
                return false;
            }
            var grid = model != null ? model : XlsSheetGridModel.of(sheet);
            var above = top - 1;
            // Such a block has a filled cell in the row above, past the first column: only then is the sheet split.
            var filledAbove = IntStream.rangeClosed(left + 1, lastColumnOf(grid, above, right))
                    .anyMatch(column -> GridSplitter.containsCell(grid, column, above));
            return filledAbove && Arrays.stream(new GridSplitter(grid).split())
                    .map(IGridTable::getRegion)
                    .anyMatch(block -> block.getBottom() == above && left < block.getLeft()
                            && block.getLeft() <= right);
        }

        /**
         * The last column a filled cell of a row can stand in: the last cell of the row, the last column of a merged
         * region reaching into the row, or the given column, whichever is the furthest.
         */
        private static int lastColumnOf(XlsSheetGridModel grid, int row, int column) {
            var last = Math.max(column, grid.getMaxColumnIndex(row));
            for (var at = 0; at < grid.getNumberOfMergedRegions(); at++) {
                var merged = grid.getMergedRegion(at);
                if (merged.getTop() <= row && row <= merged.getBottom()) {
                    last = Math.max(last, merged.getRight());
                }
            }
            return last;
        }

        /** The index of a merged region of the sheet. */
        private int indexOf(CellRangeAddress merged) {
            var all = sheet.getMergedRegions();
            for (var at = 0; at < all.size(); at++) {
                if (all.get(at).equals(merged)) {
                    return at;
                }
            }
            return -1;
        }

        /** Gives the cells of the lines to format their cell formats, region by region. */
        private void writeCells() {
            var groupRows = Set.copyOf(regions.groups().rows());
            var groupColumns = Set.copyOf(regions.groups().columns());
            for (var region : regions.regions()) {
                var columns = lines.columns().subSet(region.left(), true, region.right(), true);
                for (var row = region.top(); row <= region.bottom(); row++) {
                    if (lines.all() || lines.rows().contains(row)) {
                        writeRow(region, row, groupRows, groupColumns, null);
                    } else if (!columns.isEmpty()) {
                        writeRow(region, row, groupRows, groupColumns, columns);
                    }
                }
            }
        }

        /**
         * Gives cells of one row of a region their cell formats, a line before each column starting a group. The line
         * starts at the titles: it does not cross the signature, the properties or the code.
         *
         * @param columns the columns of the cells, or {@code null} for every cell of the row
         */
        private void writeRow(RawTableRegion region, int row, Set<Integer> groupRows, Set<Integer> groupColumns,
                              @Nullable Set<Integer> columns) {
            var style = TableStyle.of(region.role());
            var edges = Edges.of(style, region, row, groupRows, table.getBottom() - table.getTop());
            var grouped = edges.grouped(style);
            var sheetRow = rowOf(table.getTop() + row);
            if (columns == null) {
                for (var column = region.left(); column <= region.right(); column++) {
                    write(sheetRow, column, style, groupColumns.contains(column) ? grouped : edges, region.returned());
                }
            } else {
                for (var column : columns) {
                    write(sheetRow, column, style, groupColumns.contains(column) ? grouped : edges, region.returned());
                }
            }
        }

        /** The row of the sheet, created when it has none. */
        private XSSFRow rowOf(int at) {
            var row = sheet.getRow(at);
            return row != null ? row : sheet.createRow(at);
        }

        /**
         * Gives a cell its cell format, bold when the table returns it among other cells. A text in pieces of their own
         * fonts keeps its marks, in the font of the role of the cell, and loses every other font. An OpenL formula gets
         * the apostrophe of Excel, and anything but a text loses it.
         */
        private void write(XSSFRow row, int column, TableStyle style, Edges edges, boolean bold) {
            var at = table.getLeft() + column;
            var found = row.getCell(at);
            var cell = found != null ? found : row.createCell(at);
            var ct = cell.getCTCell();
            // An absent cell format reads as the first one, as the schema of a workbook gives it.
            var source = (int) ct.getS();
            var text = textOf(cell);
            var format = styles.format(style, edges, bold, source, Quote.of(text == null ? null : text.getString()));
            if (format != source) {
                ct.setS(format);
            }
            if (style != TableStyle.SIGNATURE && text != null && text.numFormattingRuns() > 0) {
                TextMarks.writeText(cell, TextMarks.write(text.getString(), TextMarks.of(text, sheet.getWorkbook()),
                        color -> styles.markFont(style, color)));
            }
        }

        /**
         * The text of a cell, or {@code null} when the cell holds no text.
         *
         * <p>A cell marked as a text may hold an Excel formula, as Apache POI leaves a text turned into one, and a
         * cell marked as the text of a formula may hold a text, as POI leaves a formula removed.
         */
        private static @Nullable XSSFRichTextString textOf(XSSFCell cell) {
            var type = cell.getCTCell().getT();
            // A number or an empty cell is told by its mark alone: a formula pass reads every cell of the table.
            var marked = type == STCellType.S || type == STCellType.INLINE_STR || type == STCellType.STR;
            return marked && cell.getCellType() == CellType.STRING ? cell.getRichStringCellValue() : null;
        }

        /**
         * Writes the header text in its pieces. A header whose pieces do not cover its text, or that has none, is
         * written as plain text.
         */
        private void writeSignature() {
            var row = sheet.getRow(table.getTop());
            var cell = row == null ? null : row.getCell(table.getLeft());
            var type = cell == null ? null : cell.getCTCell().getT();
            if (type != STCellType.S && type != STCellType.INLINE_STR) {
                return;
            }
            var text = cell.getStringCellValue();
            TextMarks.writeText(cell, regions.splits(text) ? inPieces(text) : new XSSFRichTextString(text));
        }

        /** The header text, a run of its own font for each piece. */
        private XSSFRichTextString inPieces(String text) {
            // One run appended per piece: a font applied to a range rebuilds every run of the text.
            var rich = new XSSFRichTextString();
            var from = 0;
            for (var piece : regions.signature()) {
                if (piece.to() > from) {
                    rich.append(text.substring(from, piece.to()), pieceFont(piece.part()));
                    from = piece.to();
                }
            }
            return rich;
        }

        /** The font of a piece of the header. */
        private XSSFFont pieceFont(RawTableSignaturePart part) {
            var piece = TableStyle.Piece.of(part);
            return styles.pieceFont(piece.bold(), piece.colour());
        }
    }
}
