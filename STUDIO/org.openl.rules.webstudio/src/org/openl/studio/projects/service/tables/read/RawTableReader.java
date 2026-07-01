package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.tableeditor.model.ui.BorderStyle;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.studio.projects.model.tables.RawTableBorderLineStyle;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellBorder;
import org.openl.studio.projects.model.tables.RawTableCellBorderSide;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableView;

/**
 * Reads any table in raw format as a 2D matrix with explicit merge information.
 * <p>
 * This reader works with any table type (Data, Test, Spreadsheet, etc.) without interpreting or parsing
 * the table structure. It preserves the exact layout including merged cells through colspan/rowspan values.
 * <p>
 * Implementation uses {@code TableModel} from TableEditor component to properly handle:
 * <ul>
 *   <li>Row and column spans (merged cells with colspan/rowspan)
 *   <li>Cell content extraction with formula evaluation
 *   <li>Proper table dimensions (height/width)
 *   <li>All table types (works consistently across different table implementations)
 * </ul>
 * <p>
 * The resulting {@code RawTableView} contains:
 * <ul>
 *   <li>A 2D matrix ({@code source}) of {@code RawTableCell} objects
 *   <li>Each cell with explicit colspan/rowspan for merged regions
 *   <li>Covered cells marked with {@code covered=true} to indicate they're masked by another cell's span
 *   <li>No type information or validation - purely structural
 * </ul>
 *
 * @author Vladyslav Pikus
 */
@Component
public class RawTableReader extends TableReader<RawTableView, RawTableView.Builder> {

    public RawTableReader() {
        super(RawTableView::builder);
    }

    /** The value that tells {@link TableModel} not to cap rows; used when {@code maxRows} is unspecified. */
    private static final int NO_ROW_CAP = -1;

    @Override
    protected void initialize(RawTableView.Builder builder, IOpenLTable openLTable) {
        initialize(builder, openLTable, null, null, false);
    }

    /**
     * Read a window of a table in raw format, optionally including each cell's Excel style.
     * <p>
     * The window is the {@code maxRows} rows starting at {@code startRow}, so a caller can page through a
     * large table in slices — read a chunk, edit it through the table actions API, then read the next chunk.
     * Rows outside the window are cropped while building the grid model, so they are never materialised.
     * <p>
     * Cell addresses stay absolute, so a sliced cell keeps the same address it has in the whole table.
     * {@link RawTableView#totalRows} reports the full row count whenever the window omits rows. The matrix is
     * empty when {@code startRow} is past the last row.
     *
     * @param openLTable the table to read
     * @param startRow   the zero-based index of the first row to return; {@code null} starts at the top
     * @param maxRows    the maximum number of rows to return from {@code startRow}; {@code null} returns every
     *                   remaining row
     * @param withStyles whether to attach each cell's Excel style (background, font, alignment)
     * @return the raw table view
     */
    public RawTableView read(IOpenLTable openLTable, @Nullable Integer startRow, @Nullable Integer maxRows,
            boolean withStyles) {
        RawTableView.Builder builder = RawTableView.builder();
        initialize(builder, openLTable, startRow, maxRows, withStyles);
        return builder.build();
    }

    private void initialize(RawTableView.Builder builder, IOpenLTable openLTable, @Nullable Integer startRow,
            @Nullable Integer maxRows, boolean withStyles) {
        super.initialize(builder, openLTable);
        builder.pos(openLTable.getUriParser().getRange());
        var metaInfoReader = openLTable.getSyntaxNode().getMetaInfoReader();
        int fullHeight = openLTable.getGridTable().getHeight();
        // Crop from startRow first; TableModel then caps maxRows rows from the slice top. Both act on the grid
        // region, so rows outside the window are never materialised and cell addresses stay absolute.
        var gridTable = sliceFrom(openLTable.getGridTable(), startRow);
        int cap = maxRows == null ? NO_ROW_CAP : maxRows;
        var tableModel = gridTable == null ? null
                : TableModel.initializeTableModel(gridTable, cap, metaInfoReader);

        List<List<RawTableCell>> source = tableModel == null ? List.of()
                : convertTableModelToMatrix(tableModel, new CellValueReader(metaInfoReader), withStyles);
        // The grid model keeps one extra row rather than hiding a single row; trim to exactly maxRows so the
        // window size is predictable for paging.
        if (maxRows != null && source.size() > maxRows) {
            source = source.subList(0, maxRows);
        }
        builder.source(source);
        // Report the full height whenever the window omits rows (a non-zero offset or a top cap).
        if ((startRow != null && startRow > 0) || source.size() < fullHeight) {
            builder.totalRows(fullHeight);
        }
    }

    /** Crop the table to the rows at and below {@code startRow}, or {@code null} when the offset is past the end. */
    private static @Nullable IGridTable sliceFrom(IGridTable table, @Nullable Integer startRow) {
        if (startRow == null || startRow <= 0) {
            return table;
        }
        if (startRow >= table.getHeight()) {
            return null;
        }
        return table.getRows(startRow);
    }

    /**
     * Convert TableModel to raw 2D matrix representation with explicit span information.
     * <p>
     * Processing:
     * <ul>
     *   <li>Iterates through all cells in the TableModel
     *   <li>Extracts colspan/rowspan for each cell from CellModel
     *   <li>Retrieves cell values using the provided cellValueReader function
     *   <li>Marks cells that are covered by other cells' spans with RawTableCell.COVERED_CELL
     *   <li>Creates RawTableCell objects with explicit colspan/rowspan for origin cells
     * </ul>
     * <p>
     * Covered cells (those within a merged region but not the origin cell) are marked with
     * {@code RawTableCell.COVERED_CELL} to indicate they should be skipped during processing.
     *
     * @param tableModel      The TableModel containing cell layout and span information
     * @param cellValueReader Function to extract cell values from ICell instances
     * @return 2D list of RawTableCell objects representing the table matrix
     */
    private List<List<RawTableCell>> convertTableModelToMatrix(TableModel tableModel,
            Function<ICell, Object> cellValueReader, boolean withStyles) {
        List<List<RawTableCell>> matrix = new ArrayList<>();

        var cells = tableModel.getCells();
        int height = tableModel.getHeight();
        int width = height > 0 ? cells[0].length : 0;

        // Track which cells have already been covered as merged cells
        Set<CellRef> coveredCells = new HashSet<>();

        for (int row = 0; row < height; row++) {
            List<RawTableCell> rowCells = new ArrayList<>();

            for (int col = 0; col < width; col++) {
                if (coveredCells.contains(new CellRef(row, col))) {
                    // This cell was already covered as part of a merged region (covered by another cell's span)
                    rowCells.add(RawTableCell.COVERED_CELL);
                    continue;
                }

                var cm = (CellModel) cells[row][col];
                // Extract cell value
                var cell = tableModel.getGridTable().getCell(cm.getColumn(), cm.getRow());
                Object value = cellValueReader.apply(cell);
                // Cell address in A1 notation, matching the address reported by compilation messages
                var cellAddress = cell.getUri();

                // Check for merging
                int rowspan = cm.getRowspan();
                int colspan = cm.getColspan();

                var rawCell = RawTableCell.builder()
                        .cell(cellAddress)
                        .value(value)
                        .colspan(colspan)
                        .rowspan(rowspan)
                        .style(withStyles ? styleOf(cm) : null)
                        .build();

                if (colspan > 1 || rowspan > 1) {
                    // Mark spanned cells as covered
                    for (int r = row; r < row + rowspan && r < height; r++) {
                        for (int c = col; c < col + colspan && c < width; c++) {
                            if (r > row || c > col) {
                                coveredCells.add(new CellRef(r, c));
                            }
                        }
                    }
                }
                rowCells.add(rawCell);
            }

            matrix.add(rowCells);
        }

        return matrix;
    }

    /** The cell's Excel style, or {@code null} when every attribute is at its default. */
    private static @Nullable RawTableCellStyle styleOf(CellModel cm) {
        ICellFont font = cm.getFont();
        String background = nonDefault(cm.getRgbBackground(), "#ffffff");
        String color = font == null ? null : nonDefault(font.getFontColor(), "#000000");
        Boolean bold = font != null && font.isBold() ? Boolean.TRUE : null;
        Boolean italic = font != null && font.isItalic() ? Boolean.TRUE : null;
        Boolean underline = font != null && font.isUnderlined() ? Boolean.TRUE : null;
        Integer indent = cm.getIndent() > 0 ? cm.getIndent() : null;
        String align = cm.getHalign();
        String valign = cm.getValign();
        RawTableCellBorder border = borderOf(cm);
        if (background == null && color == null && bold == null && italic == null
                && underline == null && indent == null && align == null && valign == null && border == null) {
            return null;
        }
        return RawTableCellStyle.builder()
                .background(background)
                .color(color)
                .align(align)
                .valign(valign)
                .bold(bold)
                .italic(italic)
                .underline(underline)
                .indent(indent)
                .border(border)
                .build();
    }

    /** The cell's borders per side, or {@code null} when the cell has no border on any side. */
    private static @Nullable RawTableCellBorder borderOf(CellModel cm) {
        BorderStyle[] sides = cm.getBorderStyle();
        if (sides == null) {
            return null;
        }
        var top = borderSide(sides, ICellStyle.TOP);
        var right = borderSide(sides, ICellStyle.RIGHT);
        var bottom = borderSide(sides, ICellStyle.BOTTOM);
        var left = borderSide(sides, ICellStyle.LEFT);
        if (top == null && right == null && bottom == null && left == null) {
            return null;
        }
        return RawTableCellBorder.builder().top(top).right(right).bottom(bottom).left(left).build();
    }

    /** One border side, or {@code null} when that side has no border. */
    private static @Nullable RawTableCellBorderSide borderSide(BorderStyle[] sides, int side) {
        if (side >= sides.length) {
            return null;
        }
        BorderStyle bs = sides[side];
        if (bs == null || bs == BorderStyle.NONE || bs.getWidth() == 0) {
            return null;
        }
        RawTableBorderLineStyle style = switch (bs.getStyle() == null ? "solid" : bs.getStyle()) {
            case "dashed" -> RawTableBorderLineStyle.DASHED;
            case "dotted" -> RawTableBorderLineStyle.DOTTED;
            case "double" -> RawTableBorderLineStyle.DOUBLE;
            default -> RawTableBorderLineStyle.SOLID;
        };
        return RawTableCellBorderSide.builder().style(style).width(bs.getWidth()).build();
    }

    /** Hex form of an RGB triple, or {@code null} when it is missing or equals the given default colour. */
    private static @Nullable String nonDefault(short @Nullable [] rgb, String defaultHex) {
        if (rgb == null || rgb.length < 3) {
            return null;
        }
        String hex = String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
        return hex.equals(defaultHex) ? null : hex;
    }

    private record CellRef(int row, int col) {
    }

}
