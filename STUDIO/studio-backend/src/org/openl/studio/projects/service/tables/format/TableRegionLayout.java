package org.openl.studio.projects.service.tables.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableSignaturePiece;
import org.openl.studio.projects.model.tables.RawTableValueType;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.service.tables.OpenLTableUtils;

/**
 * Lays a table out in regions: the rectangles of its cells that take one role, whatever its kind.
 *
 * <p>The screen draws a table from this layout, and the formatter writes the OpenL table style from it, so both read a
 * table alike. The body of a table is laid out by what the compiler read of it, see {@link CompiledReads}: each part
 * stands where the compiler found it. A table the compiler read none of, because a type or a table it names is
 * missing, is laid out by the parts its text writes where its kind writes them, see {@link GuessedReads}.
 *
 * <p>The kind of a table, {@link LayoutKind}, tells which {@link BodyLayout} lays out its body. A table of no kind
 * OpenL Studio formats, which it shows as of the type Other, is not laid out. Each layout tells what a place of the
 * body is; every cell of the sheet a place takes is that.
 *
 * <p>Every kind lays out its header the same way: the header reaches across the table as its signature, and its text
 * is split into pieces. The rows of properties a table declares between its header and its body take the
 * properties.
 *
 * <p>A value takes the type the compiler bound it to ({@link ValueTypes}). The values of a role in one line of the
 * table — a column, or a row of a table written the other way round — share the type most of them have, which their
 * region takes; a value of another type names its own.
 *
 * <p>A layout depends on the compiled table and on its cells, so the layout of a compiled table is kept until the
 * table is compiled again ({@link #regionsOf}). Every page of a large table reuses it.
 */
public final class TableRegionLayout {

    /** The layouts of the compiled tables, kept until a compilation replaces the table. */
    private static final Map<TableSyntaxNode, TableRegions> LAYOUTS = Collections.synchronizedMap(new WeakHashMap<>());

    private TableRegionLayout() {
    }

    /**
     * Whether a table is of a kind OpenL Studio formats: every kind of table but a part of a table split into several
     * ({@code TablePart}) and a table of no kind OpenL knows, which OpenL Studio shows as of the type Other.
     *
     * @param table the table
     * @return {@code true} when the table is laid out in roles
     */
    public static boolean formats(IOpenLTable table) {
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
     * @return the regions of the table, or {@code null} for a table of a kind OpenL Studio does not format
     */
    public static @Nullable TableRegions regionsOf(IOpenLTable table) {
        return regionsOf(table, table.getGridTable());
    }

    /**
     * The regions of a table as it stands on a grid, laid out once for each compilation of the table when the grid is
     * the one the table was compiled from, see {@link #regionsOf(IOpenLTable)}.
     *
     * @param table the table, which tells its kind
     * @param grid  the table as it stands on its grid, header included
     * @return the regions of the table, or {@code null} for a table of a kind OpenL Studio does not format
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
     * @return the regions of the table, or {@code null} for a table of a kind OpenL Studio does not format
     */
    static @Nullable TableRegions of(IOpenLTable table, IGridTable grid) {
        var kind = LayoutKind.of(table);
        if (kind == null) {
            return null;
        }
        var region = grid.getRegion();
        var cells = new Place[grid.getHeight()][grid.getWidth()];
        var logical = LogicalTableHelper.logicalTable(grid);
        layOutHead(cells, region, grid, logical);
        var rows = bodyOf(logical);
        var node = table.getSyntaxNode();
        var transposed = false;
        var groups = RawTableGroups.NONE;
        if (rows != null) {
            var read = kind.readOf(node);
            transposed = read.orElseGet(() -> kind.isTurned(rows));
            var body = TableBody.builder()
                    .rows(rows)
                    .transposed(transposed)
                    .compiled(new CompiledTable(node))
                    .guessed(read.isEmpty())
                    .build();
            var placed = kind.getLayout().layOut(body);
            var bottom = rows.getSource().getRegion().getBottom();
            layOutPlaces(cells, region, placed);
            groups = groupsOf(placed, region, bottom);
        }
        var types = typesOf(cells, grid, ValueTypes.of(node, region));
        var text = Objects.requireNonNullElse(logical.getCell(0, 0).getStringValue(), "");
        return new Roles(cells, types, transposed).regions(groups, SignatureSplitter.split(text));
    }

    /**
     * The header and the properties of a table, laid out from its grid alone: the header reaches across the table, and
     * the properties take the rows of the section that declares them.
     *
     * @param grid the table as it stands on its grid, header included
     * @return the rows the header and the properties take, and their regions
     */
    static Head headOf(IGridTable grid) {
        var logical = LogicalTableHelper.logicalTable(grid);
        var right = grid.getWidth() - 1;
        var signature = logical.getRows(0, 0).getSource().getHeight();
        var regions = new ArrayList<RawTableRegion>(2);
        regions.add(new RawTableRegion(RawTableRole.SIGNATURE, null, 0, 0, signature - 1, right));
        var properties = PropertiesHelper.getPropertiesTableSection(logical);
        var height = signature;
        if (properties != null) {
            height += properties.getSource().getHeight();
            regions.add(new RawTableRegion(RawTableRole.PROPERTIES, null, signature, 0, height - 1, right));
        }
        return new Head(height, regions);
    }

    /**
     * The header and the properties of a table.
     *
     * @param height  how many rows they take
     * @param regions their regions
     */
    public record Head(int height, List<RawTableRegion> regions) {

        /**
         * A header of one row reaching across the table, and the rows of properties under it.
         *
         * @param width      how many columns the table has
         * @param properties how many rows the properties take: {@code 0} for a table without them
         * @return the header and the properties
         */
        public static Head of(int width, int properties) {
            var signature = new RawTableRegion(RawTableRole.SIGNATURE, null, 0, 0, 0, width - 1);
            return properties == 0
                    ? new Head(1, List.of(signature))
                    : new Head(1 + properties, List.of(signature,
                    new RawTableRegion(RawTableRole.PROPERTIES, null, 1, 0, properties, width - 1)));
        }
    }

    /**
     * The regions of a table as its last compile laid it out, which a save of it is formatted with when the save does
     * not compile.
     *
     * @param table the table, as the last compile read it
     * @return the regions, or {@code null} for a table of a kind OpenL Studio does not format, or one whose workbook
     *         was written to since the compile read it, when what the compile read no longer answers for the sheet
     */
    public static @Nullable TableRegions compiledRegionsOf(IOpenLTable table) {
        return isSavedSinceRead(table.getSyntaxNode()) ? null : regionsOf(table);
    }

    /**
     * Lays out the header row and the properties under it.
     *
     * @param cells   what each cell of the table is, in table coordinates, which the body adds its cells to
     * @param region  where the table stands on its sheet
     * @param table   the table as it stands on its grid
     * @param logical the same table read with its merged cells
     */
    private static void layOutHead(Place[][] cells, IGridRegion region, IGridTable table, ILogicalTable logical) {
        // The header reaches across the table, whether the header is merged over it or not.
        var header = Place.of(RawTableRole.SIGNATURE);
        for (var cell : holdersOf(table.getSubtable(0, 0, table.getWidth(), 1))) {
            cover(cells, region, cell, header);
        }
        if (PropertiesHelper.getPropertiesTableSection(logical) != null) {
            // The whole row of the section: the keyword, and the properties it names.
            layOutProperties(cells, region, logical.getRows(1, 1).getSource());
        }
    }

    /**
     * Lays out the rows of table properties.
     *
     * <p>The section is one row of the table, though each property stands on a row of the sheet of its own.
     */
    private static void layOutProperties(Place[][] cells, IGridRegion region, IGridTable rows) {
        var properties = Place.of(RawTableRole.PROPERTIES);
        for (var cell : holdersOf(rows)) {
            cover(cells, region, cell, properties);
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
     * Gives every cell of the sheet a body takes what its place is.
     *
     * <p>A place can take several cells of the sheet: a row written over several rows of the sheet, or a column over
     * several columns. Every one of them takes what the place is. A cell inside a merged region takes what the cell
     * that holds the region is.
     *
     * @param cells  what each cell of the table is, which the body adds its cells to
     * @param region where the table stands on its sheet
     * @param placed the body as its layout reads it, a place in each of its cells, and what each place is
     */
    private static void layOutPlaces(Place[][] cells, IGridRegion region, BodyLayout.Placed placed) {
        var places = placed.places();
        for (var row = 0; row < places.getHeight(); row++) {
            for (var column = 0; column < places.getWidth(); column++) {
                for (var cell : holdersOf(places.getSubtable(column, row, 1, 1).getSource())) {
                    cover(cells, region, cell, placed.roles().at(cell, column, row));
                }
            }
        }
    }

    /** The lines of the body that start a group, in table coordinates, inside the table. */
    private static RawTableGroups groupsOf(BodyLayout.Placed placed, IGridRegion region, int bottom) {
        var rows = placed.groupRows()
                .stream()
                .filter(row -> row > region.getTop() && row <= bottom)
                .map(row -> row - region.getTop())
                .sorted()
                .toList();
        var columns = placed.groupColumns()
                .stream()
                .filter(column -> column > region.getLeft() && column <= region.getRight())
                .map(column -> column - region.getLeft())
                .sorted()
                .toList();
        return new RawTableGroups(rows, columns);
    }

    /** The type of the value of each cell holding a merged region, or merged with none. */
    private static @Nullable RawTableValueType[][] typesOf(Place[][] cells, IGridTable grid, ValueTypes values) {
        var types = new RawTableValueType[cells.length][];
        for (var row = 0; row < cells.length; row++) {
            types[row] = new RawTableValueType[cells[row].length];
            for (var column = 0; column < cells[row].length; column++) {
                var place = cells[row][column];
                var cell = grid.getCell(column, row);
                if (place != null && holdsRegion(cell)) {
                    types[row][column] = values.of(cell, place.typing());
                }
            }
        }
        return types;
    }

    /** A run of cells of one row taking the same key. */
    private record Run<K>(int left, int right, K key) {
    }

    /** A rectangle of cells, in table coordinates, that grows down the rows. */
    private static final class Rectangle {
        private final int top;
        private final int left;
        private final int right;
        private int bottom;

        Rectangle(int top, int left, int bottom, int right) {
            this.top = top;
            this.left = left;
            this.bottom = bottom;
            this.right = right;
        }
    }

    /**
     * The roles of the cells of a table and the types of their values, made into regions. Nothing compares two of
     * them, so they are not a record: a record of arrays would compare the arrays by identity.
     */
    @RequiredArgsConstructor
    private static final class Roles {

        /** What each cell of the table is, in table coordinates. */
        private final @Nullable Place[][] cells;

        /** The type of the value of each cell holding one. */
        private final @Nullable RawTableValueType[][] types;

        /** Whether the lines of the table are its columns, rather than its rows. */
        private final boolean transposed;

        /**
         * The rectangles of the cells of a table that take an equal key: each row cut into runs of cells taking one
         * key, and a run joined to the rectangle above it when that rectangle spans the same columns with the same
         * key.
         *
         * @param cells  the cells of the table, in table coordinates, for their size
         * @param keyAt  the key of a cell, or {@code null} for a cell no rectangle takes
         * @param result the rectangle made of a key and its place
         * @return the rectangles, in the order of their top rows
         */
        private static <K, R> List<R> rectangles(Object[][] cells, BiFunction<Integer, Integer, @Nullable K> keyAt,
                                                BiFunction<K, Rectangle, R> result) {
            var found = new ArrayList<Rectangle>();
            var keys = new ArrayList<K>();
            // The rectangles reaching the row above, by the columns they span and their key.
            var open = new HashMap<Run<K>, Integer>();
            for (var row = 0; row < cells.length; row++) {
                var reached = new HashMap<Run<K>, Integer>();
                var width = cells[row].length;
                var column = 0;
                while (column < width) {
                    var key = keyAt.apply(row, column);
                    var end = runEnd(keyAt, row, column, width, key);
                    if (key != null) {
                        var run = new Run<>(column, end, key);
                        var above = open.get(run);
                        if (above == null) {
                            found.add(new Rectangle(row, column, row, end));
                            keys.add(key);
                            reached.put(run, found.size() - 1);
                        } else {
                            found.get(above).bottom = row;
                            reached.put(run, above);
                        }
                    }
                    column = end + 1;
                }
                open = reached;
            }
            var results = new ArrayList<R>(found.size());
            for (var at = 0; at < found.size(); at++) {
                results.add(result.apply(keys.get(at), found.get(at)));
            }
            return results;
        }

        /** The last column of the run of cells of a row that take the key the run starts with. */
        private static <K> int runEnd(BiFunction<Integer, Integer, @Nullable K> keyAt, int row, int column, int width,
                                      @Nullable K key) {
            var end = column;
            while (end + 1 < width && Objects.equals(keyAt.apply(row, end + 1), key)) {
                end++;
            }
            return end;
        }


        /** The regions of the roles, each taking the type most of the values of its role in its line have. */
        TableRegions regions(RawTableGroups groups, List<RawTableSignaturePiece> signature) {
            var lineTypes = lineTypes();
            var regions = rectangles(cells, (row, column) -> {
                var place = cells[row][column];
                return place == null ? null : new RoleKey(place.role(), lineTypes.get(lineKey(place, row, column)));
            }, (key, rectangle) -> new RawTableRegion(key.role(), key.type(), rectangle.top, rectangle.left,
                    rectangle.bottom, rectangle.right));
            var own = new HashMap<TableRegions.Cell, RawTableValueType>();
            for (var row = 0; row < cells.length; row++) {
                for (var column = 0; column < cells[row].length; column++) {
                    var type = types[row][column];
                    var place = cells[row][column];
                    if (type != null && place != null && type != lineTypes.get(lineKey(place, row, column))) {
                        own.put(new TableRegions.Cell(row, column), type);
                    }
                }
            }
            return new TableRegions(regions, groups, signature, own);
        }

        /** The type most of the values of each role in each line have, ties going to the type met first. */
        private Map<LineKey, RawTableValueType> lineTypes() {
            var counts = new HashMap<LineKey, Map<RawTableValueType, Integer>>();
            for (var row = 0; row < cells.length; row++) {
                for (var column = 0; column < cells[row].length; column++) {
                    var type = types[row][column];
                    var place = cells[row][column];
                    if (type != null && place != null) {
                        counts.computeIfAbsent(lineKey(place, row, column),
                                key -> new EnumMap<>(RawTableValueType.class)).merge(type, 1, Integer::sum);
                    }
                }
            }
            var lineTypes = new HashMap<LineKey, RawTableValueType>();
            counts.forEach((line, byType) -> lineTypes.put(line, byType.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .orElseThrow()
                    .getKey()));
            return lineTypes;
        }

        private LineKey lineKey(Place place, int row, int column) {
            return new LineKey(place.role(), transposed ? row : column);
        }
    }

    /** A role in a line of the table: a column, or a row of a table written the other way round. */
    private record LineKey(RawTableRole role, int line) {
    }

    /** The key of a region of a role: the role, and the type of its values. */
    private record RoleKey(RawTableRole role, @Nullable RawTableValueType type) {
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
     * Gives every cell of the merged region a cell covers the same place. A region of empty cells does not widen the
     * table, so it may be merged past the edge of the table: the cells beyond the edge are not the table's.
     */
    private static void cover(Place[][] cells, IGridRegion table, ICell cell, Place place) {
        var region = cell.getAbsoluteRegion();
        var top = Math.max(region.getTop(), table.getTop());
        var bottom = Math.min(region.getBottom(), table.getBottom());
        var left = Math.max(region.getLeft(), table.getLeft());
        var right = Math.min(region.getRight(), table.getRight());
        for (var row = top; row <= bottom; row++) {
            for (var column = left; column <= right; column++) {
                cells[row - table.getTop()][column - table.getLeft()] = place;
            }
        }
    }

    /** What a layout tells a place of the body is. */
    @FunctionalInterface
    interface PlaceRoles {

        /**
         * What a place is.
         *
         * @param cell   a cell of the sheet the place takes, holding a merged region or merged with none
         * @param column the column of the place in the body as the compiler reads it
         * @param row    the row of the place in the body as the compiler reads it
         * @return what the place is
         */
        Place at(ICell cell, int column, int row);
    }
}
