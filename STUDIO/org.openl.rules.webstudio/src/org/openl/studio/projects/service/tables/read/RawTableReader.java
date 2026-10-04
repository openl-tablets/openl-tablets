package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.TextRun;
import org.openl.rules.tableeditor.model.CellEditorSelector;
import org.openl.rules.tableeditor.model.ui.BorderStyle;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellBorder;
import org.openl.studio.projects.model.tables.RawTableCellBorderSide;
import org.openl.studio.projects.model.tables.RawTableCellMetaInfo;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableCellUsage;
import org.openl.studio.projects.model.tables.RawTableHorizontalAlign;
import org.openl.studio.projects.model.tables.RawTableTextRun;
import org.openl.studio.projects.model.tables.RawTableUsageKind;
import org.openl.studio.projects.model.tables.RawTableVerticalAlign;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.service.tables.TableModules;
import org.openl.studio.projects.service.tables.theme.ThemedTable;
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

    /** Picks the editor a cell asks for, the way the Editor picks it. */
    private static final CellEditorSelector EDITOR_SELECTOR = new CellEditorSelector();

    @Override
    protected void initialize(RawTableView.Builder builder, IOpenLTable openLTable) {
        initialize(builder, openLTable, RawTableRead.builder().build());
    }

    /**
     * Read a window of a table in raw format, with what the read asks to report besides the values of the cells:
     * the Excel style of each cell, what the compiler knows about it, the look the table theme gives it.
     * <p>
     * The window is the {@code maxRows} rows starting at {@code startRow}, so a caller can page through a
     * large table in slices — read a chunk, edit it through the table actions API, then read the next chunk.
     * Rows outside the window are cropped while building the grid model, so they are never materialised.
     * <p>
     * Cell addresses stay absolute, so a sliced cell keeps the same address it has in the whole table.
     * {@link RawTableView#totalRows} reports the full row count whenever the window omits rows. The matrix is
     * empty when {@code startRow} is past the last row.
     * <p>
     * A read naming a table theme reports the look the theme gives each cell in its style and its runs, in place of
     * the formatting of the workbook, the theme named as the source of the style. Such a read is a view only: an edit
     * is made from a read of the workbook, so it never writes the theme. A cell the theme does not reach keeps the
     * style the workbook holds.
     *
     * @param openLTable the table to read
     * @param read       the window of rows to read, and what to report besides the values of the cells
     * @return the raw table view
     */
    public RawTableView read(IOpenLTable openLTable, RawTableRead read) {
        RawTableView.Builder builder = RawTableView.builder();
        initialize(builder, openLTable, read);
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
        var tableModel = TableModel.initializeTableModel(openLTable.getGridTable(), TableWindow.EVERY_ROW, metaInfoReader);
        return tableModel == null ? List.of()
                : convertTableModelToMatrix(tableModel, metaInfoReader,
                        RawTableRead.builder().withStyles(withStyles).build());
    }

    /** The table's meta info, or an empty one when the table carries none. */
    private static MetaInfoReader metaInfoReaderOf(IOpenLTable openLTable) {
        var metaInfoReader = openLTable.getSyntaxNode().getMetaInfoReader();
        return metaInfoReader == null ? EmptyMetaInfoReader.getInstance() : metaInfoReader;
    }

    private void initialize(RawTableView.Builder builder, IOpenLTable openLTable, RawTableRead read) {
        super.initialize(builder, openLTable);
        builder.pos(openLTable.getUriParser().getRange());
        var metaInfoReader = metaInfoReaderOf(openLTable);
        var fullHeight = openLTable.getGridTable().getHeight();
        // Crop from startRow first; TableModel then caps maxRows rows from the slice top. Both act on the grid
        // region, so rows outside the window are never materialised and cell addresses stay absolute.
        var window = TableWindow.of(openLTable.getGridTable(), read.startRow(), read.maxRows());
        var gridTable = sliceFrom(openLTable.getGridTable(), window.startRow());
        var tableModel = gridTable == null ? null
                : TableModel.initializeTableModel(gridTable, window.rows(), metaInfoReader);

        List<List<RawTableCell>> source = tableModel == null ? List.of()
                : convertTableModelToMatrix(tableModel, metaInfoReader, read);
        // The grid model keeps one extra row rather than hiding a single row; trim to exactly the window so
        // its size is predictable for paging.
        if (window.rows() != TableWindow.EVERY_ROW && source.size() > window.rows()) {
            source = source.subList(0, window.rows());
        }
        builder.source(source);
        builder.headerHeight(headerHeightOf(openLTable));
        // Report the full height whenever the window omits rows (a non-zero offset or a top cap).
        if (window.partial(fullHeight)) {
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
    private List<List<RawTableCell>> convertTableModelToMatrix(TableModel tableModel, MetaInfoReader metaInfoReader,
            RawTableRead read) {
        var cellValueReader = new CellValueReader(metaInfoReader);
        var matrix = new ArrayList<List<RawTableCell>>();

        var cells = tableModel.getCells();
        var height = tableModel.getHeight();
        int width = height > 0 ? cells[0].length : 0;

        // Track which cells have already been covered as merged cells
        var coveredCells = new CoveredCells(height, width);

        for (var row = 0; row < height; row++) {
            var rowCells = new ArrayList<RawTableCell>();
            for (var col = 0; col < width; col++) {
                // A cell inside a merged region that is not the one it starts at carries nothing of its own.
                if (coveredCells.holds(row, col)) {
                    rowCells.add(RawTableCell.COVERED_CELL);
                    continue;
                }
                var cellModel = (CellModel) cells[row][col];
                var cell = tableModel.getGridTable().getCell(cellModel.getColumn(), cellModel.getRow());
                rowCells.add(readCell(cell, cellModel, read, metaInfoReader, cellValueReader));
                coveredCells.mark(row, col, cellModel);
            }
            matrix.add(rowCells);
        }
        var theme = read.theme();
        if (theme != null) {
            // A table read a window at a time goes on under the rows read: the theme knows how it draws them.
            var region = tableModel.getGridTable().getRegion();
            var under = region.getTop() + height;
            ThemeLines.share(matrix, column -> lineOver(theme, under, region.getLeft() + column));
        }
        return matrix;
    }

    /**
     * The line the theme draws over a cell of the table, as a read reports it, or {@code null} when it draws none there
     * or the cell is outside the table.
     */
    private static @Nullable RawTableCellBorderSide lineOver(ThemedTable theme, int row, int column) {
        var themed = theme.at(row, column);
        return themed == null ? null : ThemeStyles.topLine(themed.style());
    }

    /** One cell as the API reports it: what it holds, what it was written with, and how far it reaches. */
    private RawTableCell readCell(ICell cell, CellModel cellModel, RawTableRead read,
            MetaInfoReader metaInfoReader, CellValueReader cellValueReader) {
        // A cell carries both what it computes and what it was written with, so a screen showing formulas
        // chooses between them without asking for the table again.
        var formula = cell.getFormula();
        var builder = RawTableCell.builder()
                // Cell address in A1 notation, matching the address reported by compilation messages
                .cell(cell.getUri())
                .value(cellValueReader.apply(cell))
                .formula(StringUtils.isBlank(formula) ? null : "=" + formula)
                .comment(commentOf(cell))
                .colspan(cellModel.getColspan())
                .rowspan(cellModel.getRowspan())
                .metaInfo(read.withMetaInfo() ? metaInfoOf(cell, metaInfoReader, read.modules()) : null);
        var themed = Optional.ofNullable(read.theme())
                .map(theme -> theme.at(cell.getAbsoluteRow(), cell.getAbsoluteColumn()))
                .orElse(null);
        if (themed != null) {
            drawInTheme(builder, cell, styleOf(cellModel), themed);
        } else if (read.withStyles() || read.theme() != null) {
            builder.style(styleOf(cellModel)).runs(runsOf(cell));
        }
        return builder.build();
    }

    /** The pieces of the cell text formatted with fonts of their own, or {@code null} when it takes the cell font. */
    private static @Nullable List<RawTableTextRun> runsOf(ICell cell) {
        var runs = cell.getTextRuns();
        if (runs.isEmpty()) {
            return null;
        }
        return runs.stream()
                .map(run -> new RawTableTextRun(run.text(), fontOf(run)))
                .toList();
    }

    /** The font of a run, or {@code null} when the run takes the font of the cell. */
    private static @Nullable RawTableCellStyle fontOf(TextRun run) {
        var font = run.font();
        return font == null ? null : RawTableStyles.font(RawTableCellStyle.builder(), font).build();
    }

    /**
     * Reports a cell as the table theme draws it: in the style and the pieces of text the theme gives it, in place of
     * the formatting of the workbook.
     *
     * <p>The style is the cell style with the attributes the theme sets laid over it. The header is drawn in the
     * pieces of the theme. Any other text formatted in pieces of its own keeps them where the theme names nothing of
     * the font of its cell, and is drawn in that font otherwise, as writing the theme gives.
     *
     * @param style  the style the cell has in the workbook, or {@code null} when it has none
     * @param themed how the theme draws the cell
     */
    private static void drawInTheme(RawTableCell.RawTableCellBuilder builder, ICell cell,
            @Nullable RawTableCellStyle style, ThemedTable.ThemedCell themed) {
        // Only the cell holding the header text is formatted in the pieces of the theme.
        var text = themed.header() == null ? null : cell.getStringValue();
        var runs = themed.runs(text);
        builder.style(ThemeStyles.over(style, themed.style()))
                .runs(themed.keepsOwnRuns(runs) ? runsOf(cell) : runsOf(text, runs, style));
    }

    /** The pieces of the theme as the Tables API reports them, or {@code null} when the theme formats none. */
    private static @Nullable List<RawTableTextRun> runsOf(@Nullable String text, List<ThemedTable.ThemedRun> runs,
            @Nullable RawTableCellStyle style) {
        if (text == null || runs.isEmpty()) {
            return null;
        }
        return runs.stream()
                .map(run -> new RawTableTextRun(text.substring(run.start(), run.end()),
                        ThemeStyles.fontOf(style, run.style())))
                .toList();
    }

    /** The note a reader left on the cell in Excel, which a screen marks the cell by. */
    private static @Nullable String commentOf(ICell cell) {
        var comment = cell.getComment();
        return comment == null ? null : StringUtils.trimToNull(comment.getText());
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
                .editor(editor == null ? null : editor.getEditorTypeAndMetadata().editor())
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
                            // Named only where a module holds the table. A word can resolve to a table the
                            // engine wrote itself while compiling — the one that chooses between the versions
                            // of an overloaded rule — which sits in no workbook and can be opened nowhere. The
                            // reader is told what the word means and offered no way in, rather than a way in
                            // that leads nowhere.
                            .tableId(where == null ? null : TableUtils.makeTableId(node.getUri()))
                            .module(where == null ? null : where.module())
                            .projectId(where == null ? null : where.projectId())
                            .kind(RawTableUsageKind.of(node.getNodeType()))
                            .build();
                })
                .toList();
    }

    /** The cell's Excel style, or {@code null} when every attribute is at its default. */
    private static @Nullable RawTableCellStyle styleOf(CellModel cm) {
        var style = RawTableCellStyle.builder()
                .background(RawTableStyles.hex(cm.getRgbBackground(), RawTableStyles.WHITE))
                .align(horizontalAlign(cm.getHalign()))
                .valign(verticalAlign(cm.getValign()))
                .indent(positive(cm.getIndent()))
                .border(borderOf(cm));
        var font = cm.getFont();
        if (font != null) {
            RawTableStyles.font(style, font);
        }
        var read = style.build();
        return read.isEmpty() ? null : read;
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
        return side >= sides.length ? null : RawTableStyles.borderSide(sides[side]);
    }

}
