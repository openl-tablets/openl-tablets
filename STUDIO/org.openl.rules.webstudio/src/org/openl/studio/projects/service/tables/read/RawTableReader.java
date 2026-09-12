package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.EmptyMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.tableeditor.model.CellEditorSelector;
import org.openl.rules.tableeditor.model.ui.BorderStyle;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.studio.projects.model.tables.RawTableBorderLineStyle;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellBorder;
import org.openl.studio.projects.model.tables.RawTableCellBorderSide;
import org.openl.studio.projects.model.tables.RawTableCellMetaInfo;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableCellUsage;
import org.openl.studio.projects.model.tables.RawTableHorizontalAlign;
import org.openl.studio.projects.model.tables.RawTableUsageKind;
import org.openl.studio.projects.model.tables.RawTableVerticalAlign;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.service.tables.TableModules;
import org.openl.util.StringUtils;

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

    /** Picks the editor a cell asks for, the way the Editor picks it. */
    private static final CellEditorSelector EDITOR_SELECTOR = new CellEditorSelector();

    @Override
    protected void initialize(RawTableView.Builder builder, IOpenLTable openLTable) {
        initialize(builder, openLTable, null, null, false, false, TableModules.none());
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
     * @param withStyles   whether to attach each cell's Excel style (background, font, alignment)
     * @param withMetaInfo whether to attach what the compiler knows about each cell — the pieces of its text
     *                     that refer to something, the type it holds, the editor it asks for
     * @param modules      the modules a usage's table is looked up in, so a reader can be sent to it
     * @return the raw table view
     */
    public RawTableView read(IOpenLTable openLTable, @Nullable Integer startRow, @Nullable Integer maxRows,
            boolean withStyles, boolean withMetaInfo, TableModules modules) {
        RawTableView.Builder builder = RawTableView.builder();
        initialize(builder, openLTable, startRow, maxRows, withStyles, withMetaInfo, modules);
        return builder.build();
    }

    /**
     * Reads the cells of a table as a matrix, without the identity a table has in a project.
     *
     * <p>Used where the table belongs to no open project — the two sides of a comparison come from
     * files of their own — so it has no id, no kind and no properties to report.
     *
     * @param openLTable the table to read
     * @param withStyles whether to attach each cell's Excel style (background, font, alignment)
     * @return the cells of the table, row by row
     */
    public List<List<RawTableCell>> readCells(IOpenLTable openLTable, boolean withStyles) {
        var metaInfoReader = metaInfoReaderOf(openLTable);
        var tableModel = TableModel.initializeTableModel(openLTable.getGridTable(), NO_ROW_CAP, metaInfoReader);
        return tableModel == null ? List.of()
                : convertTableModelToMatrix(tableModel, withStyles, metaInfoReader, false, TableModules.none());
    }

    /** The table's meta info, or an empty one when the table carries none. */
    private static MetaInfoReader metaInfoReaderOf(IOpenLTable openLTable) {
        var metaInfoReader = openLTable.getSyntaxNode().getMetaInfoReader();
        return metaInfoReader == null ? EmptyMetaInfoReader.getInstance() : metaInfoReader;
    }

    private void initialize(RawTableView.Builder builder, IOpenLTable openLTable, @Nullable Integer startRow,
            @Nullable Integer maxRows, boolean withStyles, boolean withMetaInfo, TableModules modules) {
        super.initialize(builder, openLTable);
        builder.pos(openLTable.getUriParser().getRange());
        var metaInfoReader = metaInfoReaderOf(openLTable);
        var fullHeight = openLTable.getGridTable().getHeight();
        // Crop from startRow first; TableModel then caps maxRows rows from the slice top. Both act on the grid
        // region, so rows outside the window are never materialised and cell addresses stay absolute.
        var gridTable = sliceFrom(openLTable.getGridTable(), startRow);
        int cap = maxRows == null ? NO_ROW_CAP : maxRows;
        var tableModel = gridTable == null ? null
                : TableModel.initializeTableModel(gridTable, cap, metaInfoReader);

        List<List<RawTableCell>> source = tableModel == null ? List.of()
                : convertTableModelToMatrix(tableModel, withStyles, metaInfoReader, withMetaInfo, modules);
        // The grid model keeps one extra row rather than hiding a single row; trim to exactly maxRows so the
        // window size is predictable for paging.
        if (maxRows != null && source.size() > maxRows) {
            source = source.subList(0, maxRows);
        }
        builder.source(source);
        builder.headerHeight(headerHeightOf(openLTable));
        // Report the full height whenever the window omits rows (a non-zero offset or a top cap).
        if ((startRow != null && startRow > 0) || source.size() < fullHeight) {
            builder.totalRows(fullHeight);
        }
    }

    /**
     * How many rows at the top of the table its header takes.
     *
     * <p>The engine keeps a business view of every table it knows — the same table without its header line, its
     * properties section and, in a decision table, the service rows. What that view leaves out at the top is
     * what a screen hiding the header leaves out, so the two agree without the screen knowing any table's shape.
     *
     * <p>A table with no business view of its own — one the engine could not bind, or one whose whole body is
     * its header — has nothing to hide, and the answer is 0.
     */
    private static int headerHeightOf(IOpenLTable openLTable) {
        var business = openLTable.getGridTable(IXlsTableNames.VIEW_BUSINESS);
        var whole = openLTable.getGridTable();
        if (business == null || business == whole) {
            return 0;
        }
        return Math.max(0, whole.getHeight() - business.getHeight());
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
     * @param tableModel The TableModel containing cell layout and span information
     * @return 2D list of RawTableCell objects representing the table matrix
     */
    private List<List<RawTableCell>> convertTableModelToMatrix(TableModel tableModel, boolean withStyles,
            MetaInfoReader metaInfoReader, boolean withMetaInfo, TableModules modules) {
        var cellValueReader = new CellValueReader(metaInfoReader);
        var matrix = new ArrayList<List<RawTableCell>>();

        var cells = tableModel.getCells();
        var height = tableModel.getHeight();
        int width = height > 0 ? cells[0].length : 0;

        // Track which cells have already been covered as merged cells
        var coveredCells = new HashSet<CellRef>();

        for (var row = 0; row < height; row++) {
            var rowCells = new ArrayList<RawTableCell>();

            for (var col = 0; col < width; col++) {
                if (coveredCells.contains(new CellRef(row, col))) {
                    // This cell was already covered as part of a merged region (covered by another cell's span)
                    rowCells.add(RawTableCell.COVERED_CELL);
                    continue;
                }

                var cm = (CellModel) cells[row][col];
                // Extract cell value
                var cell = tableModel.getGridTable().getCell(cm.getColumn(), cm.getRow());
                var value = cellValueReader.apply(cell);
                // A cell carries both what it computes and what it was written with, so a screen showing
                // formulas chooses between them without asking for the table again.
                var formula = cell.getFormula();
                // Cell address in A1 notation, matching the address reported by compilation messages
                var cellAddress = cell.getUri();

                // Check for merging
                var rowspan = cm.getRowspan();
                var colspan = cm.getColspan();

                var rawCell = RawTableCell.builder()
                        .cell(cellAddress)
                        .value(value)
                        .formula(StringUtils.isBlank(formula) ? null : "=" + formula)
                        .colspan(colspan)
                        .rowspan(rowspan)
                        .style(withStyles ? styleOf(cm) : null)
                        .metaInfo(withMetaInfo ? metaInfoOf(cell, metaInfoReader, modules) : null)
                        .build();

                if (colspan > 1 || rowspan > 1) {
                    // Mark spanned cells as covered
                    for (var r = row; r < row + rowspan && r < height; r++) {
                        for (var c = col; c < col + colspan && c < width; c++) {
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

    /**
     * What the compiler knows about the cell, or {@code null} when it knows nothing worth reporting.
     *
     * <p>The pieces of text the compiler resolved are reported as ranges over the cell's own text, each with
     * the table it refers to — by the identifier the Tables API addresses a table by, not the location the
     * engine knows it at.
     */
    private static @Nullable RawTableCellMetaInfo metaInfoOf(ICell cell, MetaInfoReader metaInfoReader,
            TableModules modules) {
        CellMetaInfo metaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        if (metaInfo == null) {
            return null;
        }
        var dataType = metaInfo.getDataType();
        var editor = EDITOR_SELECTOR.selectEditor(cell, metaInfo);
        var reported = RawTableCellMetaInfo.builder()
                .usages(usagesOf(metaInfo, modules))
                .type(dataType == null ? null : dataType.getDisplayName(INamedThing.SHORT))
                .returnCell(metaInfo.isReturnCell() ? Boolean.TRUE : null)
                .editor(editor == null ? null : editor.getEditorTypeAndMetadata().getEditor())
                .build();
        return reported.isEmpty() ? null : reported;
    }

    /** The pieces of the cell's text the compiler resolved, in the order they appear. */
    private static List<RawTableCellUsage> usagesOf(CellMetaInfo metaInfo, TableModules modules) {
        var usedNodes = metaInfo.getUsedNodes();
        if (usedNodes == null) {
            return List.of();
        }
        return usedNodes.stream()
                .map(node -> {
                    var where = modules.locationOf(node.getUri());
                    return RawTableCellUsage.builder()
                            .start(node.getStart())
                            .end(node.getEnd())
                            .description(node.getDescription())
                            .tableId(node.getUri() == null ? null : TableUtils.makeTableId(node.getUri()))
                            .module(where == null ? null : where.module())
                            .projectId(where == null ? null : where.projectId())
                            .kind(RawTableUsageKind.of(node.getNodeType()))
                            .build();
                })
                .toList();
    }

    /** The cell's Excel style, or {@code null} when every attribute is at its default. */
    private static @Nullable RawTableCellStyle styleOf(CellModel cm) {
        var font = cm.getFont();

        var style = RawTableCellStyle.builder()
                .background(nonDefault(cm.getRgbBackground(), "#ffffff"))
                .color(font == null ? null : nonDefault(font.getFontColor(), "#000000"))
                .align(horizontalAlign(cm.getHalign()))
                .valign(verticalAlign(cm.getValign()))
                .bold(flag(font, ICellFont::isBold))
                .italic(flag(font, ICellFont::isItalic))
                .underline(flag(font, ICellFont::isUnderlined))
                .indent(positive(cm.getIndent()))
                .border(borderOf(cm))
                .build();

        return style.isEmpty() ? null : style;
    }

    /** The font flag as {@link Boolean#TRUE}, or {@code null} when the font is absent or the flag is off. */
    private static @Nullable Boolean flag(@Nullable ICellFont font, Predicate<ICellFont> predicate) {
        return font != null && predicate.test(font) ? Boolean.TRUE : null;
    }

    /** The value when positive, or {@code null} otherwise. */
    private static @Nullable Integer positive(int value) {
        return value > 0 ? value : null;
    }

    /** The horizontal alignment mapped to the API enum, or {@code null} for the default (left). */
    private static @Nullable RawTableHorizontalAlign horizontalAlign(@Nullable String halign) {
        if (halign == null) {
            return null;
        }
        return switch (halign) {
            case "right" -> RawTableHorizontalAlign.RIGHT;
            case "center" -> RawTableHorizontalAlign.CENTER;
            case "justify" -> RawTableHorizontalAlign.JUSTIFY;
            default -> null;
        };
    }

    /** The vertical alignment mapped to the API enum, or {@code null} for the default (bottom). */
    private static @Nullable RawTableVerticalAlign verticalAlign(@Nullable String valign) {
        if (valign == null) {
            return null;
        }
        return switch (valign) {
            case "center" -> RawTableVerticalAlign.CENTER;
            case "top" -> RawTableVerticalAlign.TOP;
            default -> null;
        };
    }

    /** The cell's borders per side, or {@code null} when the cell has no border on any side. */
    private static @Nullable RawTableCellBorder borderOf(CellModel cm) {
        var sides = cm.getBorderStyle();
        if (sides == null) {
            return null;
        }
        var border = RawTableCellBorder.builder()
                .top(borderSide(sides, ICellStyle.TOP))
                .right(borderSide(sides, ICellStyle.RIGHT))
                .bottom(borderSide(sides, ICellStyle.BOTTOM))
                .left(borderSide(sides, ICellStyle.LEFT))
                .build();
        return border.isEmpty() ? null : border;
    }

    /** One border side, or {@code null} when that side has no border. */
    private static @Nullable RawTableCellBorderSide borderSide(BorderStyle[] sides, int side) {
        if (side >= sides.length) {
            return null;
        }
        var bs = sides[side];
        if (bs == null || bs == BorderStyle.NONE || bs.getWidth() == 0) {
            return null;
        }
        var style = switch (bs.getStyle() == null ? "solid" : bs.getStyle()) {
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
        // Mask each component to an unsigned byte so a negative short never sign-extends to 8 hex digits.
        String hex = "#%02x%02x%02x".formatted(rgb[0] & 0xff, rgb[1] & 0xff, rgb[2] & 0xff);
        return hex.equals(defaultHex) ? null : hex;
    }

    private record CellRef(int row, int col) {
    }

}
