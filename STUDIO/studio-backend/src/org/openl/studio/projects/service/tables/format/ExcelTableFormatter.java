package org.openl.studio.projects.service.tables.format;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableSignaturePart;
import org.openl.studio.projects.model.tables.RawTableSignaturePiece;
import org.openl.studio.projects.service.tables.format.TableStyle.Line;
import org.openl.studio.projects.service.tables.format.WorkbookStyles.Edges;

/**
 * Writes the OpenL table style into the workbooks of tables.
 *
 * <p>Each cell of a table gets a cell format that links the built-in style of its role and draws the lines at the
 * edges of its region: a line above and below the signature, below the properties, below a section, above a group,
 * above and below the result, and below the body. What the cell had is kept: its number format, its indent, its text
 * wrapping and its text rotation. No value of a cell is changed.
 *
 * <p>The signature is the only cell written as rich text, in its pieces: the name of the table bold, the names of the
 * parameters in the text colour, and the rest lighter. Rich text in any other cell is replaced by the same text in the
 * font of its cell.
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
        return format(((XlsSheetGridModel) grid.getGrid()).getSheetToWrite(), grid.getRegion(), regions);
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
        if (!(sheet instanceof XSSFSheet xssf)) {
            return false;
        }
        var styles = workbooks.computeIfAbsent(xssf.getWorkbook(), WorkbookStyles::new);
        new TableWrite(xssf, table, regions, styles).write();
        return true;
    }

    /**
     * Formats a table being saved in its layout, fitted to the table as the save leaves it. A formatting that fails is
     * logged and leaves the table as it is written, so the save goes on.
     *
     * @param grid   the table as it stands on its sheet, header included
     * @param layout the regions of the table, moved along the lines the save inserted and removed
     */
    public static void formatOnSave(IGridTable grid, RegionShift layout) {
        try {
            new ExcelTableFormatter().format(grid, layout.fittedTo(grid));
        } catch (RuntimeException e) {
            log.warn("The table at {} is saved unformatted.", grid.getUri(), e);
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
     */
    private record TableWrite(XSSFSheet sheet, IGridRegion table, TableRegions regions, WorkbookStyles styles) {

        void write() {
            var bottom = table.getBottom() - table.getTop();
            var groupRows = Set.copyOf(regions.groups().rows());
            var groupColumns = Set.copyOf(regions.groups().columns());
            for (var region : regions.regions()) {
                var style = TableStyle.of(region.role());
                for (var row = region.top(); row <= region.bottom(); row++) {
                    var top = topOf(style, region, row, groupRows);
                    var below = bottomOf(style, region, row, bottom);
                    var plain = new Edges(top, below, null);
                    var grouped = new Edges(top, below, Line.GROUP);
                    for (var column = region.left(); column <= region.right(); column++) {
                        write(row, column, style, groupColumns.contains(column) ? grouped : plain);
                    }
                }
            }
            writeSignature();
        }

        /** The line above a cell of a region: the line of its role, or the line above a group. */
        private static @Nullable Line topOf(TableStyle style, RawTableRegion region, int row, Set<Integer> groups) {
            if (style == TableStyle.SIGNATURE || style == TableStyle.RESULT && row == region.top()) {
                return style.getLook().top();
            }
            return groups.contains(row) ? Line.GROUP : null;
        }

        /**
         * The line below a cell of a region: the line below its role at the bottom of the region, or the line closing
         * the table.
         */
        private static @Nullable Line bottomOf(TableStyle style, RawTableRegion region, int row, int bottom) {
            var below = style == TableStyle.SIGNATURE || row == region.bottom() ? style.lineBelow() : null;
            return below == null && row == bottom ? Line.MAJOR : below;
        }

        /** Gives a cell its cell format, and writes its text as plain text. */
        private void write(int row, int column, TableStyle style, Edges edges) {
            var cell = (XSSFCell) PoiExcelHelper.getOrCreateCell(table.getLeft() + column, table.getTop() + row, sheet);
            var ct = cell.getCTCell();
            var source = ct.isSetS() ? (int) ct.getS() : 0;
            ct.setS(styles.format(style, edges, source));
            if (style != TableStyle.SIGNATURE && isRich(cell)) {
                writeText(cell, new XSSFRichTextString(cell.getStringCellValue()));
            }
        }

        /** Whether the text of a cell is written in pieces of their own fonts. */
        private static boolean isRich(XSSFCell cell) {
            var type = cell.getCTCell().getT();
            return (type == STCellType.S || type == STCellType.INLINE_STR)
                    && cell.getRichStringCellValue().numFormattingRuns() > 0;
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
            var rich = new XSSFRichTextString(text);
            if (covers(regions.signature(), text)) {
                var from = 0;
                for (var piece : regions.signature()) {
                    rich.applyFont(from, piece.to(), pieceFont(piece.part()));
                    from = piece.to();
                }
            }
            writeText(cell, rich);
        }

        private static boolean covers(List<RawTableSignaturePiece> pieces, String text) {
            return !pieces.isEmpty() && pieces.getLast().to() == text.length();
        }

        /** The font of a piece of the header: the name bold, the names of the parameters in the text colour. */
        private XSSFFont pieceFont(RawTableSignaturePart part) {
            return switch (part) {
                case NAME -> styles.pieceFont(true, TableStyle.Look.TEXT);
                case PARAMETER_NAME -> styles.pieceFont(false, TableStyle.Look.TEXT);
                case KEYWORD, TYPE, PUNCTUATION, PARAMETER_TYPE -> styles.pieceFont(false, TableStyle.Look.MUTED);
            };
        }

        /**
         * Writes a text into a cell. A text written into the cell itself, as tools other than Excel write it, would
         * keep its characters and lose its pieces: blanking the cell first makes the text a shared string.
         */
        private static void writeText(XSSFCell cell, XSSFRichTextString text) {
            if (cell.getCTCell().getT() == STCellType.INLINE_STR) {
                cell.setBlank();
            }
            cell.setCellValue(text);
        }
    }
}
