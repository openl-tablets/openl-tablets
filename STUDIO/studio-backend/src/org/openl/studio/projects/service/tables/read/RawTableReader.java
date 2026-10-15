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
import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableHorizontalAlign;
import org.openl.studio.projects.model.tables.RawTableTextRun;
import org.openl.studio.projects.model.tables.RawTableUsageKind;
import org.openl.studio.projects.model.tables.RawTableValueType;
import org.openl.studio.projects.model.tables.RawTableVerticalAlign;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.service.tables.TableModules;
import org.openl.studio.projects.service.tables.theme.TableRegionLayout;
import org.openl.studio.projects.service.tables.theme.TableRegions;
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
     * the Excel style of each cell and the roles of the cells, what the compiler knows about each cell.
     * <p>
     * The window is the {@code maxRows} rows starting at {@code startRow}, so a caller can page through a
     * large table in slices — read a chunk, edit it through the table actions API, then read the next chunk.
     * Rows outside the window are cropped while building the grid model, so they are never materialised.
     * <p>
     * Cell addresses stay absolute, so a sliced cell keeps the same address it has in the whole table.
     * {@link RawTableView#totalRows} reports the full row count whenever the window omits rows. The matrix is
     * empty when {@code startRow} is past the last row.
     * <p>
     * A read with the styles names each style its cells use once, and reports the roles of the cells of the whole
     * table, its groups and the pieces of its header, which every window of its rows shares, laid out once for each
     * compilation of the table. A cell whose value is of another type than its region names its own type.
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
     * @return the cells of the table, row by row, and the styles they name, or no styles when none are read
     */
    public RawTableCells readCells(IOpenLTable openLTable, boolean withStyles) {
        var metaInfoReader = metaInfoReaderOf(openLTable);
        var tableModel = TableModel.initializeTableModel(openLTable.getGridTable(), TableWindow.EVERY_ROW, metaInfoReader);
        var styled = withStyles ? new Styled(null) : null;
        var source = tableModel == null ? List.<List<RawTableCell>>of()
                : convertTableModelToMatrix(tableModel, metaInfoReader,
                        RawTableRead.builder().withStyles(withStyles).build(), styled);
        return new RawTableCells(source, styled == null ? null : styled.styles.styles());
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

        var styled = read.withStyles() ? styledOf(openLTable) : null;
        List<List<RawTableCell>> source = tableModel == null ? List.of()
                : convertTableModelToMatrix(tableModel, metaInfoReader, read, styled);
        // The grid model keeps one extra row rather than hiding a single row; trim to exactly the window so
        // its size is predictable for paging.
        if (window.rows() != TableWindow.EVERY_ROW && source.size() > window.rows()) {
            source = source.subList(0, window.rows());
        }
        builder.source(source);
        if (styled != null) {
            builder.styles(styled.styles.styles());
            Optional.ofNullable(styled.layout).map(Layout::regions).ifPresent(regions -> builder
                    .regions(regions.regions())
                    // A table without groups sends none, rather than a group of no lines.
                    .groups(RawTableGroups.NONE.equals(regions.groups()) ? null : regions.groups())
                    .signature(regions.signature()));
        }
        builder.headerHeight(headerHeightOf(openLTable));
        // Report the full height whenever the window omits rows (a non-zero offset or a top cap).
        if (window.partial(fullHeight)) {
            builder.totalRows(fullHeight);
        }
    }

    /** What a read with the styles reports besides the values of the cells, for a table of a project. */
    private static Styled styledOf(IOpenLTable openLTable) {
        var regions = TableRegionLayout.regionsOf(openLTable);
        var region = openLTable.getGridTable().getRegion();
        return new Styled(regions == null ? null : new Layout(regions, region.getTop(), region.getLeft()));
    }

    /** What a read with the styles reports besides the values of the cells, gathered as the cells are read. */
    private static final class Styled {

        /** The styles the cells name. */
        private final RawTableStyleDictionary styles = new RawTableStyleDictionary();

        /** The roles of the cells of the table, or {@code null} for a table that has none. */
        private final @Nullable Layout layout;

        Styled(@Nullable Layout layout) {
            this.layout = layout;
        }

        /** The number of the style of a cell. */
        @Nullable
        Integer styleOf(CellModel cellModel) {
            return styles.numberOf(RawTableReader.styleOf(cellModel));
        }
    }

    /**
     * The roles of the cells of a table, and where the table stands on its sheet.
     *
     * @param regions the roles of the cells, in table coordinates
     * @param top     the row of the sheet the table starts at
     * @param left    the column of the sheet the table starts at
     */
    private record Layout(TableRegions regions, int top, int left) {

        /** The type of the value of a cell of the sheet, when it differs from the type of its region. */
        @Nullable
        RawTableValueType typeAt(ICell cell) {
            return regions.typeAt(cell.getAbsoluteRow() - top, cell.getAbsoluteColumn() - left);
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
     * <p>
     * Each cell reports the borders it has itself; a line two cells share may be reported by both of them, and a
     * screen draws the wider one.
     *
     * @param tableModel The TableModel containing cell layout and span information
     * @param styled     what a read with the styles reports besides the values, or {@code null} for the values alone
     * @return 2D list of RawTableCell objects representing the table matrix
     */
    private List<List<RawTableCell>> convertTableModelToMatrix(TableModel tableModel, MetaInfoReader metaInfoReader,
            RawTableRead read, @Nullable Styled styled) {
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
                rowCells.add(readCell(cell, cellModel, read, metaInfoReader, cellValueReader, styled));
                coveredCells.mark(row, col, cellModel);
            }
            matrix.add(rowCells);
        }
        return matrix;
    }

    /** One cell as the API reports it: what it holds, what it was written with, and how far it reaches. */
    private RawTableCell readCell(ICell cell, CellModel cellModel, RawTableRead read,
            MetaInfoReader metaInfoReader, CellValueReader cellValueReader, @Nullable Styled styled) {
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
        if (styled != null) {
            builder.style(styled.styleOf(cellModel))
                    .runs(runsOf(cell, styled.styles))
                    .type(styled.layout == null ? null : styled.layout.typeAt(cell));
        }
        return builder.build();
    }

    /**
     * The pieces of the cell text formatted with fonts of their own, each named by where it ends in the text, or
     * {@code null} when the text takes the cell font.
     */
    private static @Nullable List<RawTableTextRun> runsOf(ICell cell, RawTableStyleDictionary styles) {
        var runs = cell.getTextRuns();
        if (runs.isEmpty()) {
            return null;
        }
        var pieces = new ArrayList<RawTableTextRun>(runs.size());
        var to = 0;
        for (var run : runs) {
            to += run.text().length();
            pieces.add(new RawTableTextRun(to, styles.numberOf(fontOf(run))));
        }
        return pieces;
    }

    /** The font of a run, or {@code null} when the run takes the font of the cell. */
    private static @Nullable RawTableCellStyle fontOf(TextRun run) {
        return Optional.ofNullable(run.font())
                .map(font -> RawTableStyles.font(RawTableCellStyle.builder(), font).build())
                .orElse(null);
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
                            // Named only where a module holds the table. The table the engine built to choose
                            // between the versions of an overloaded rule sits in no workbook, and is opened
                            // through the module it is reached from. A word naming no table is told what it
                            // means and offered no way in.
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
        Optional.ofNullable(cm.getFont()).ifPresent(font -> RawTableStyles.font(style, font));
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
