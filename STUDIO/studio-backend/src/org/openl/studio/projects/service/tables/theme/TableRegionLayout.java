package org.openl.studio.projects.service.tables.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.theme.TableRegions.Region;

/**
 * Lays a table out in regions: the rectangles of its cells that take their look from the same parts of a theme,
 * whatever its kind.
 *
 * <p>The screen, the editor and the project-wide writer all draw a theme from this layout, so the screen shows what
 * writing the theme gives. The body of a table is laid out by what the compiler read of it, see {@link CompiledReads}:
 * each part stands where the compiler found it. A table the compiler read none of has no parts it knows, so its body
 * takes the look every cell starts from rather than a look guessed from where its cells stand.
 *
 * <p>The kind of a table, {@link ThemeKind}, tells which {@link BodyLayout} lays out its body. A table of no kind a
 * theme styles is not laid out. Each layout names the parts of a place of the body; every cell of the sheet a place
 * takes gets those parts, and a cell that reaches the bottom of the table gets the last row over them.
 *
 * <p>Every kind lays out its header the same way. The header reaches across the table, and the cell holding the header
 * text is formatted in pieces. The rows of properties a table declares between its header and its body take the
 * properties.
 *
 * <p>A layout depends on the compiled table and on its cells, so the layout of a compiled table is kept until the
 * table is compiled again ({@link #regionsOf}). Every page of a large table reuses it.
 */
final class TableRegionLayout {

    /** The layouts of the compiled tables, kept until a compilation replaces the table. */
    private static final Map<TableSyntaxNode, TableRegions> LAYOUTS = Collections.synchronizedMap(new WeakHashMap<>());

    private TableRegionLayout() {
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
     * The regions of a table as it stands on its grid, laid out once for each compilation of the table.
     *
     * <p>The layout of the compiled table is reused while the workbook of the table was not saved since the
     * compilation read it. A workbook saved since then may hold cells the compiled table does not know, so the table
     * is laid out again for each such call, as it stands.
     *
     * @param table the table, which tells its kind
     * @param grid  the table as it stands on its grid, header included
     * @return the regions of the table, or {@code null} for a table of a kind no theme styles
     */
    static @Nullable TableRegions regionsOf(IOpenLTable table, IGridTable grid) {
        var node = table.getSyntaxNode();
        if (!sameRegion(grid.getRegion(), node.getGridTable().getRegion()) || isSavedSinceRead(node)) {
            return of(table, grid);
        }
        var regions = LAYOUTS.get(node);
        if (regions == null) {
            regions = of(table, grid);
            if (regions != null) {
                LAYOUTS.put(node, regions);
            }
        }
        return regions;
    }

    /** Whether the workbook of a compiled table was saved since the compilation read it. */
    private static boolean isSavedSinceRead(TableSyntaxNode node) {
        var sheet = node.getXlsSheetSourceCodeModule();
        return sheet == null || sheet.getWorkbookSource().isSavedSinceRead();
    }

    private static boolean sameRegion(IGridRegion one, IGridRegion other) {
        return one.getTop() == other.getTop() && one.getLeft() == other.getLeft()
                && one.getBottom() == other.getBottom() && one.getRight() == other.getRight();
    }

    /**
     * Lays out the regions of a table, as it stands on its grid.
     *
     * @param table the table, which tells its kind
     * @param grid  the table as it stands on its grid, header included
     * @return the regions of the table, or {@code null} for a table of a kind no theme styles
     */
    static @Nullable TableRegions of(IOpenLTable table, IGridTable grid) {
        var kind = ThemeKind.of(table);
        if (kind == null) {
            return null;
        }
        var region = grid.getRegion();
        var cells = new Parts[grid.getHeight()][grid.getWidth()];
        var logical = LogicalTableHelper.logicalTable(grid);
        layOutHead(cells, region, grid, logical);
        var rows = bodyOf(logical);
        if (rows != null) {
            var node = table.getSyntaxNode();
            var read = kind.readOf(node);
            var body = ThemedBody.builder()
                    .rows(rows)
                    .transposed(read.orElse(false))
                    .compiled(new CompiledTable(node))
                    .build();
            var placed = read.isPresent() ? kind.getLayout().layOut(body) : BodyLayout.Placed.plain(body);
            layOutPlaces(cells, region, placed, rows.getSource().getRegion().getBottom());
        }
        var text = logical.getCell(0, 0).getStringValue();
        return new TableRegions(regionsOf(cells), text == null ? List.of() : HeaderRuns.split(text));
    }

    /**
     * Names the parts of the header row and of the properties under it.
     *
     * @param cells   the parts of each cell of the table, in table coordinates, which the body adds its cells to
     * @param region  where the table stands on its sheet
     * @param table   the table as it stands on its grid
     * @param logical the same table read with its merged cells
     */
    private static void layOutHead(Parts[][] cells, IGridRegion region, IGridTable table, ILogicalTable logical) {
        // The header reaches across the table, whether the header is merged over it or not.
        var header = Parts.BASE.with(Parts.Part.HEADER);
        for (var cell : holdersOf(table.getSubtable(0, 0, table.getWidth(), 1))) {
            cover(cells, region, cell, header);
        }
        if (PropertiesHelper.getPropertiesTableSection(logical) != null) {
            // The whole row of the section: the keyword, and the properties it names.
            layOutProperties(cells, region, logical.getRows(1, 1).getSource());
        }
    }

    /**
     * Names the parts of the rows of table properties.
     *
     * <p>The section is one row of the table, though each property stands on a row of the sheet of its own. A line
     * the look draws above or below the properties goes round the whole section, not round each of its rows.
     */
    private static void layOutProperties(Parts[][] cells, IGridRegion region, IGridTable rows) {
        var top = rows.getCell(0, 0).getAbsoluteRow();
        var bottom = top + rows.getHeight() - 1;
        var properties = Parts.BASE.with(Parts.Part.PROPERTIES);
        for (var cell : holdersOf(rows)) {
            // A cell merged over several properties, such as the keyword, reaches the edges of all of them.
            var merged = cell.getAbsoluteRegion();
            cover(cells, region, cell,
                    properties.atEdges(Parts.BASE, merged.getTop() <= top, merged.getBottom() >= bottom));
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
     * Gives every cell of the sheet a body takes the parts of its place, and the last row over them where it reaches
     * the bottom of the table.
     *
     * <p>A place can take several cells of the sheet: a row written over several rows of the sheet, or a column over
     * several columns. Every one of them takes the parts of the place. A cell inside a merged region takes the parts
     * of the cell that holds the region.
     *
     * @param cells  the parts of each cell of the table, which the body adds its cells to
     * @param region where the table stands on its sheet
     * @param placed the body as its layout reads it, a place in each of its cells, and the parts of each place
     * @param bottom the last row of the sheet the table takes
     */
    private static void layOutPlaces(Parts[][] cells, IGridRegion region, BodyLayout.Placed placed, int bottom) {
        var places = placed.places();
        for (var row = 0; row < places.getHeight(); row++) {
            for (var column = 0; column < places.getWidth(); column++) {
                for (var cell : holdersOf(places.getSubtable(column, row, 1, 1).getSource())) {
                    var parts = placed.look().at(cell, column, row);
                    cover(cells, region, cell, cell.getAbsoluteRegion().getBottom() < bottom
                            ? parts
                            : parts.with(Parts.Part.LAST_ROW));
                }
            }
        }
    }

    /**
     * The regions of the cells of a table: each row cut into runs of cells naming the same parts, and a run joined to
     * the region above it when that region spans the same columns with the same parts.
     *
     * @param cells the parts of each cell of the table, in table coordinates; {@code null} where no part reaches
     * @return the regions, in the order of their top rows
     */
    private static List<Region> regionsOf(@Nullable Parts[][] cells) {
        var regions = new ArrayList<Region>();
        // The regions reaching the row above, by the columns they span and their parts.
        var open = new HashMap<Run, Integer>();
        for (var row = 0; row < cells.length; row++) {
            var reached = new HashMap<Run, Integer>();
            var line = cells[row];
            var column = 0;
            while (column < line.length) {
                var end = column;
                while (end + 1 < line.length && Objects.equals(line[end + 1], line[column])) {
                    end++;
                }
                var parts = line[column];
                if (parts != null) {
                    var run = new Run(column, end, parts);
                    var above = open.get(run);
                    if (above == null) {
                        regions.add(new Region(parts, row, column, row, end));
                        reached.put(run, regions.size() - 1);
                    } else {
                        var region = regions.get(above);
                        regions.set(above, new Region(parts, region.top(), column, row, end));
                        reached.put(run, above);
                    }
                }
                column = end + 1;
            }
            open = reached;
        }
        return regions;
    }

    /** A run of cells of one row naming the same parts. */
    private record Run(int left, int right, Parts parts) {
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
     * The cells of the sheet a place takes that hold a merged region, or are merged with none. Covering them lays out
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

    /**
     * Gives every cell of the merged region a cell covers the same parts. A region of empty cells does not widen the
     * table, so it may be merged past the edge of the table: the cells beyond the edge are not the table's.
     */
    private static void cover(Parts[][] cells, IGridRegion table, ICell cell, Parts parts) {
        var region = cell.getAbsoluteRegion();
        var top = Math.max(region.getTop(), table.getTop());
        var bottom = Math.min(region.getBottom(), table.getBottom());
        var left = Math.max(region.getLeft(), table.getLeft());
        var right = Math.min(region.getRight(), table.getRight());
        for (var row = top; row <= bottom; row++) {
            for (var column = left; column <= right; column++) {
                cells[row - table.getTop()][column - table.getLeft()] = parts;
            }
        }
    }

    /** The parts a layout names for a place of the body. */
    @FunctionalInterface
    interface PlaceLook {

        /**
         * The parts of a place.
         *
         * @param cell   a cell of the sheet the place takes, holding a merged region or merged with none
         * @param column the column of the place in the body as the compiler reads it
         * @param row    the row of the place in the body as the compiler reads it
         * @return the parts of the cell
         */
        Parts at(ICell cell, int column, int row);
    }
}
