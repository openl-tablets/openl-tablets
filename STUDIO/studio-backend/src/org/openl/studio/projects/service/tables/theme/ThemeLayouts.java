package org.openl.studio.projects.service.tables.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.theme.ThemedTable.Cell;
import org.openl.studio.projects.service.tables.theme.ThemedTable.ThemedCell;

/**
 * Decides which look a theme gives each cell of a table, whatever its kind.
 *
 * <p>The screen, the editor and the project-wide writer all ask here, so the screen shows what writing the theme
 * gives. The body of a table is themed by what the compiler read of it, see {@link CompiledReads}: each part stands
 * where the compiler found it. A table the compiler read none of has no parts it knows, so its body takes the look
 * every cell starts from rather than a look guessed from where its cells stand.
 *
 * <p>The kind of a table, {@link ThemeKind}, tells which part of a theme it takes its look from and which
 * {@link BodyLayout} lays out its body. A table of no kind a theme styles takes no theme. Each layout tells the look of
 * a place of the body; every cell of the sheet a place takes gets that look, and a cell that reaches the bottom of the
 * table gets the last-row look over it.
 *
 * <p>Every kind themes its header the same way. The header look reaches across the table, and the cell holding the
 * header text formats it in pieces. The rows of properties a table declares between its header and its body get the
 * properties look.
 */
final class ThemeLayouts {

    private ThemeLayouts() {
    }

    /**
     * Whether a table is of a kind every theme styles: every kind of table but a part of a table split into several
     * ({@code TablePart}) and a table of no kind OpenL knows, which OpenL Studio shows as of the type Other.
     *
     * @param table the table
     * @return {@code true} when a theme can be drawn over the table and written into it
     */
    static boolean styles(IOpenLTable table) {
        return OpenLTableUtils.kindOf(table) != TableKind.OTHER;
    }

    /**
     * The look a theme gives each cell of a table.
     *
     * @param table the table, which tells its kind
     * @param grid  the table as it stands on its grid, header included
     * @param theme the theme
     * @return the look of each cell the theme reaches, or {@code null} for a table of a kind no theme styles
     */
    static @Nullable ThemedTable of(IOpenLTable table, IGridTable grid, TableTheme theme) {
        var kind = ThemeKind.of(table);
        if (kind == null) {
            return null;
        }
        var logical = LogicalTableHelper.logicalTable(grid);
        var look = kind.lookIn(theme);
        var base = ThemeStyle.NONE.with(look.style());
        var cells = themeHead(grid, logical, base, look);
        var rows = bodyOf(logical);
        if (rows != null) {
            var node = table.getSyntaxNode();
            var read = kind.readOf(node);
            var body = ThemedBody.builder()
                    .rows(rows)
                    .base(base)
                    .look(look)
                    .transposed(read.orElse(false))
                    .compiled(new CompiledTable(node))
                    .build();
            var placed = read.isPresent() ? kind.getLayout().layOut(body) : BodyLayout.Placed.plain(body);
            themePlaces(cells, placed, rows.getSource().getRegion().getBottom(), look.lastRow());
        }
        // A region of empty cells does not widen the table, so it may be merged past the edge of the table: the cells
        // beyond the edge are not the table's.
        var region = grid.getRegion();
        cells.keySet().removeIf(cell -> !GridRegionUtils.contains(region, cell.column(), cell.row()));
        return new ThemedTable(Collections.unmodifiableMap(cells));
    }

    /**
     * The look of the header row and of the properties under it, which the body of the table joins.
     *
     * @param table   the table as it stands on its grid
     * @param logical the same table read with its merged cells
     * @param base    the look every cell of the table starts from
     * @param look    the look of the table
     * @return the look of each cell of the header and the properties, in a map the body adds its cells to
     */
    private static Map<Cell, ThemedCell> themeHead(IGridTable table, ILogicalTable logical, ThemeStyle base,
                                                   TableTheme.Look look) {
        var header = Optional.ofNullable(look.header()).orElseGet(() -> TableTheme.Header.builder().build());
        var style = base.with(header.style());
        var cells = new HashMap<Cell, ThemedCell>();
        // The header look reaches across the table, whether the header is merged over it or not.
        for (var cell : holdersOf(table.getSubtable(0, 0, table.getWidth(), 1))) {
            cover(cells, cell, style);
        }
        // Only the cell holding the text formats it in pieces; the rest of the merged header carries none.
        var text = logical.getCell(0, 0).getAbsoluteRegion();
        cells.put(new Cell(text.getTop(), text.getLeft()), new ThemedCell(style, header));
        if (PropertiesHelper.getPropertiesTableSection(logical) != null) {
            // The whole row of the section: the keyword, and the properties it names.
            themeProperties(cells, logical.getRows(1, 1).getSource(), base, base.with(look.properties()));
        }
        return cells;
    }

    /**
     * Gives the rows of table properties the properties look.
     *
     * <p>The section is one row of the table, though each property stands on a row of the sheet of its own. A line
     * the look draws above or below the properties goes round the whole section, not round each of its rows.
     */
    private static void themeProperties(Map<Cell, ThemedCell> cells, IGridTable rows, ThemeStyle base,
                                        ThemeStyle look) {
        var top = rows.getCell(0, 0).getAbsoluteRow();
        var bottom = top + rows.getHeight() - 1;
        for (var cell : holdersOf(rows)) {
            // A cell merged over several properties, such as the keyword, reaches the edges of all of them.
            var region = cell.getAbsoluteRegion();
            cover(cells, cell, look.atEdges(base, region.getTop() <= top, region.getBottom() >= bottom));
        }
    }

    /**
     * The body of a table: its rows under the header and the properties it declares.
     *
     * @param logical the table read with its merged cells
     * @return the body, or {@code null} for a table that has none
     */
    private static @Nullable ILogicalTable bodyOf(ILogicalTable logical) {
        var bodyStart = PropertiesHelper.getPropertiesTableSection(logical) == null ? 1 : 2;
        return logical.getHeight() > bodyStart ? logical.getRows(bodyStart) : null;
    }

    /**
     * Gives every cell of the sheet a body takes the look of its place, and the last-row look over it where it
     * reaches the bottom of the table.
     *
     * <p>A place can take several cells of the sheet: a row written over several rows of the sheet, or a column over
     * several columns. Every one of them takes the look of the place. A cell inside a merged region is themed with the
     * cell that holds the region.
     *
     * @param cells   the looks of the cells, which the body adds its cells to
     * @param placed  the body as its layout reads it, a place in each of its cells, and the look of each place
     * @param bottom  the last row of the sheet the table takes
     * @param lastRow the look laid over a cell that reaches the bottom of the table
     */
    private static void themePlaces(Map<Cell, ThemedCell> cells, BodyLayout.Placed placed, int bottom,
                                    @Nullable ThemeStyle lastRow) {
        var places = placed.places();
        for (var row = 0; row < places.getHeight(); row++) {
            for (var column = 0; column < places.getWidth(); column++) {
                for (var cell : holdersOf(places.getSubtable(column, row, 1, 1).getSource())) {
                    var style = placed.look().at(cell, column, row);
                    cover(cells, cell, cell.getAbsoluteRegion().getBottom() < bottom ? style : style.with(lastRow));
                }
            }
        }
    }

    /**
     * The ids a TBasic or a ColumnMatch table names its columns by, in the first row of its body.
     *
     * <p>The ids are read as the compiler reads them: trimmed and in lower case. A column the table names nothing has
     * an empty id.
     *
     * @param body the body of the table: its rows under the header and the properties
     * @return the id of each column of the body, in the order of the columns
     */
    static List<String> idsOf(ILogicalTable body) {
        var ids = new ArrayList<String>(body.getWidth());
        for (var column = 0; column < body.getWidth(); column++) {
            var text = Objects.requireNonNullElse(body.getCell(column, 0).getStringValue(), "");
            ids.add(text.trim().toLowerCase(Locale.ROOT));
        }
        return ids;
    }

    /**
     * The cells of the sheet a place takes that hold a merged region, or are merged with none. Covering them themes
     * each region once, whichever of its cells the place takes.
     */
    private static List<ICell> holdersOf(IGridTable place) {
        var holders = new ArrayList<ICell>(place.getWidth() * place.getHeight());
        for (var row = 0; row < place.getHeight(); row++) {
            for (var column = 0; column < place.getWidth(); column++) {
                var cell = place.getCell(column, row);
                if (holdsRegion(cell)) {
                    holders.add(cell);
                }
            }
        }
        return holders;
    }

    /** Whether the cell is the one a merged region is held by, or a cell merged with none. */
    private static boolean holdsRegion(ICell cell) {
        var region = cell.getAbsoluteRegion();
        return region.getLeft() == cell.getAbsoluteColumn() && region.getTop() == cell.getAbsoluteRow();
    }

    /** Gives every cell of the region a cell covers the same look. */
    private static void cover(Map<Cell, ThemedCell> cells, ICell cell, ThemeStyle style) {
        var region = cell.getAbsoluteRegion();
        for (var row = region.getTop(); row <= region.getBottom(); row++) {
            for (var column = region.getLeft(); column <= region.getRight(); column++) {
                cells.put(new Cell(row, column), new ThemedCell(style, null));
            }
        }
    }

    /** The look a layout gives a place of the body. */
    @FunctionalInterface
    interface PlaceLook {

        /**
         * The look of a place.
         *
         * @param cell   a cell of the sheet the place takes, holding a merged region or merged with none
         * @param column the column of the place in the body as the compiler reads it
         * @param row    the row of the place in the body as the compiler reads it
         * @return the look of the cell
         */
        ThemeStyle at(ICell cell, int column, int row);
    }
}
