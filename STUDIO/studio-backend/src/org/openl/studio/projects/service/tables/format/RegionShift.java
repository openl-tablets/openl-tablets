package org.openl.studio.projects.service.tables.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableValueType;

/**
 * Moves the regions of a table along the lines an edit inserts and removes, so a saved table is formatted with no
 * compile.
 *
 * <p>The regions start as the layout of the last compile of the table. Each line inserted inside a region stretches
 * it; a line inserted between two regions takes the roles of a neighbouring line: the one before it when that line
 * holds values, as the rows of a rule or the steps of a Spreadsheet do, and the one after it otherwise, so a rule
 * inserted under the titles is a rule. A line removed takes its cells out of every region, and a region left with
 * none is dropped. The lines that start a group move with the lines, and a line left with no values before it starts
 * none. A group whose first lines are removed starts at the first line left of it. A line inserted next to or
 * inside cells the table returns among others, as the cells a {@code SpreadsheetResult} marks, is none of them: it
 * takes their role alone, until a compile reads the marks again.
 *
 * <p>Once the edit is done, the regions are fitted to the table as it stands: the header and the properties are laid
 * out again from the grid, a table grown at its end takes the roles of its last line for the lines it gained, and a
 * table that lost lines at its end loses their regions. The pieces of the header are read again from its text.
 *
 * <p>The regions are named in table coordinates, as {@link TableRegions} names them.
 *
 * @author Yury Molchan
 */
@Slf4j
public final class RegionShift {

    /** The roles of the cells a line holds when it is a line of values, which a line inserted next to it repeats. */
    private static final Set<RawTableRole> VALUES = EnumSet.of(RawTableRole.VALUE, RawTableRole.OUTPUT);

    /**
     * The roles of the cells a value of a type can be written into with no change of how the compiler reads the table:
     * the values it takes, gives and returns.
     */
    private static final Set<RawTableRole> DATA = EnumSet.of(RawTableRole.VALUE, RawTableRole.OUTPUT,
            RawTableRole.RESULT);

    /** The roles of a line that stands alone, which a line inserted next to it never repeats. */
    private static final Set<RawTableRole> ALONE = EnumSet.of(RawTableRole.SECTION, RawTableRole.RESULT);

    /** The regions the shift started from, as the last compile laid the table out. */
    private final TableRegions start;
    private List<RawTableRegion> regions;
    private TreeSet<Integer> groupRows;
    private TreeSet<Integer> groupColumns;
    private Map<TableRegions.Cell, RawTableValueType> types;
    /** The rows the edits inserted, and the rows next to where rows were inserted or removed. */
    private TreeSet<Integer> touchedRows = new TreeSet<>();
    /** The columns the edits inserted, and the columns next to where columns were inserted or removed. */
    private TreeSet<Integer> touchedColumns = new TreeSet<>();
    /**
     * Whether an edit wrote a cell other than a value of a type, inserted or removed a column, or a line holding more
     * than such values: an edit after which the compiler may read the table in other roles.
     */
    private boolean structureEdited;
    /**
     * The lowest row holding a cell other than a value or a key that an edit wrote, or next to which it inserted or
     * removed rows, as the table stands, or -1 for none: a title, a declaration, code. It moves with the rows inserted
     * and removed above it.
     */
    private int structureEditedRow = -1;
    /** The region the last cell written stood in: the cells an edit writes are often next to each other. */
    private @Nullable RawTableRegion lastRegion;
    /** Whether the body moved, as the header and the properties came to take another number of rows. */
    private boolean bodyMoved;
    private int height;
    private int width;

    private RegionShift(TableRegions regions, int height, int width) {
        this.start = regions;
        this.regions = new ArrayList<>(regions.regions());
        this.groupRows = new TreeSet<>(regions.groups().rows());
        this.groupColumns = new TreeSet<>(regions.groups().columns());
        this.types = new HashMap<>(regions.types());
        this.height = height;
        this.width = width;
    }

    /**
     * Starts from the regions of a table of the given size.
     *
     * @param regions the regions of the table, as its last compile laid it out
     * @param height  how many rows the table had when it was laid out
     * @param width   how many columns the table had when it was laid out
     * @return the shift, which the edits move
     */
    public static RegionShift of(TableRegions regions, int height, int width) {
        return new RegionShift(regions, height, width);
    }

    /**
     * Starts from the regions of a table as its last compile laid it out. Asked before the table is written to.
     *
     * @param table the table, as the last compile read it
     * @return the shift, or {@code null} for a table of a kind OpenL Studio does not format, or one whose workbook was
     *         written to since the compile read it ({@link TableRegionLayout#compiledRegionsOf})
     */
    public static @Nullable RegionShift ofCompiled(IOpenLTable table) {
        var regions = TableRegionLayout.compiledRegionsOf(table);
        if (regions == null) {
            return null;
        }
        var grid = table.getGridTable();
        return new RegionShift(regions, grid.getHeight(), grid.getWidth());
    }

    /**
     * Starts from the regions of a table written for the first time that no builder laid out: the regions its text
     * tells, see {@link TableRegionLayout#writtenRegionsOf}. Asked once the table is written. A layout that fails is
     * logged, and the table is saved unformatted.
     *
     * @param written the table as it stands on its sheet once written, header included
     * @return the shift, or {@code null} for a table of a kind OpenL Studio does not format, one whose header cannot be
     *         read, and one whose layout failed
     */
    public static @Nullable RegionShift ofWritten(IGridTable written) {
        try {
            var regions = TableRegionLayout.writtenRegionsOf(written);
            return regions == null ? null : new RegionShift(regions, written.getHeight(), written.getWidth());
        } catch (RuntimeException e) {
            log.warn("The table at {} is saved unformatted.", written.getUri(), e);
            return null;
        }
    }

    /**
     * The regions the shift started from, before any edit moved them.
     *
     * @return the regions of the table as its last compile laid it out
     */
    public TableRegions start() {
        return start;
    }

    /**
     * The rows the edits inserted or restyled, and the rows on either side of where rows were inserted or removed, in
     * the table as it stands: the rows whose look an edit can have changed when the table was formatted before it.
     *
     * @return the rows, in table coordinates, in their order
     */
    public NavigableSet<Integer> touchedRows() {
        return Collections.unmodifiableNavigableSet(touchedRows.headSet(height, false));
    }

    /**
     * The columns the edits inserted, and the columns on either side of where columns were inserted or removed, as
     * {@link #touchedRows()} names the rows.
     *
     * @return the columns, in table coordinates, in their order
     */
    public NavigableSet<Integer> touchedColumns() {
        return Collections.unmodifiableNavigableSet(touchedColumns.headSet(width, false));
    }

    /**
     * Whether fitting the regions moved the body, as the header and the properties came to take another number of
     * rows than they took. A body moved so may give every line of it other roles than it had.
     *
     * @return whether the body moved under the header
     */
    public boolean bodyMoved() {
        return bodyMoved;
    }

    /**
     * Whether an edit may have changed how the compiler reads the table: it wrote, merged or unmerged a cell other than
     * a value of a type, inserted or removed a column, or inserted or removed a row that holds more than such values,
     * such as a row of titles or the step a Spreadsheet returns.
     *
     * <p>A value of a type is a value the compiler converts, not code and not a name: the name of a step of a
     * Spreadsheet decides by its marks what the table returns, and the operation of a step of a TBasic table may return
     * or start a subroutine. An edit of values of a type alone keeps the roles of the last compile.
     *
     * @return whether the table may now take other roles than its last compile gave it
     */
    public boolean structureEdited() {
        return structureEdited;
    }

    /**
     * Whether an edit wrote a title, a declaration or code, or inserted or removed rows next to one, from the given
     * row down: a change of such a cell shows only in the rows it stands in. A value of no type, such as the name of a
     * step, and a key are left out: they change the roles of the kinds whose first rows tell the roles of the rest
     * only in those first rows.
     *
     * @param row a row of the table
     * @return whether such an edit touched that row or a row under it
     */
    public boolean structureEditedFrom(int row) {
        return structureEditedRow >= row;
    }

    /**
     * Tells the shift that a cell was written, or merged with others.
     *
     * @param row    the row of the cell, in table coordinates, as the table stands
     * @param column the column of the cell, in table coordinates, as the table stands
     */
    public void cellWritten(int row, int column) {
        var region = lastRegion != null && lastRegion.holds(row, column) ? lastRegion : regionAt(row, column);
        lastRegion = region;
        if (holdsTypedValue(region, row, column)) {
            return;
        }
        structureEdited = true;
        if (region == null || !DATA.contains(region.role()) && region.role() != RawTableRole.KEY) {
            structureEditedRow = Math.max(structureEditedRow, row);
        }
    }

    /** Moves the lowest structural row along rows inserted, or removed when the count is negative, at a row. */
    private void editedRowMoved(int at, int count) {
        if (structureEditedRow >= at) {
            structureEditedRow = Math.max(at, structureEditedRow + count);
        }
    }

    /** Whether a cell holds a value of a type, which a write changes with no change of how the table is read. */
    private boolean holdsTypedValue(@Nullable RawTableRegion region, int row, int column) {
        if (region == null || !DATA.contains(region.role())) {
            return false;
        }
        var type = types.isEmpty() ? region.type()
                : types.getOrDefault(new TableRegions.Cell(row, column), region.type());
        return type != null && type != RawTableValueType.CODE;
    }

    /**
     * Tells that rows were inserted next to or removed from lines: the lowest of them holding more than values of a
     * type and keys may change the roles.
     */
    private void linesEdited(int from, int to) {
        for (var row = to; row >= from; row--) {
            if (!holdsValuesAlone(row)) {
                structureEdited = true;
                structureEditedRow = Math.max(structureEditedRow, row);
                return;
            }
        }
    }

    /** The region holding a cell, or {@code null} for a cell no region covers. */
    private @Nullable RawTableRegion regionAt(int row, int column) {
        for (var region : regions) {
            if (region.holds(row, column)) {
                return region;
            }
        }
        return null;
    }

    /** Whether each cell of a row of the table holds a value of a type or a key, or the row is past its end. */
    private boolean holdsValuesAlone(int row) {
        return row < 0 || row >= height || IntStream.range(0, width).allMatch(column -> {
            var region = regionAt(row, column);
            return region != null && region.role() == RawTableRole.KEY || holdsTypedValue(region, row, column);
        });
    }

    /**
     * Inserts rows.
     *
     * @param at    the row the first inserted row takes, the rows from it on moving down
     * @param count how many rows are inserted
     */
    public void rowsInserted(int at, int count) {
        if (count > 0) {
            // The row the inserted rows push down, or the last row for rows appended at the end.
            var next = at < height ? at : at - 1;
            linesEdited(next, next);
            editedRowMoved(at, count);
        }
        insertRows(at, count);
    }

    /** Inserts rows, or columns while the regions stand turned. */
    private void insertRows(int at, int count) {
        if (count <= 0) {
            return;
        }
        lastRegion = null;
        var source = templateOf(at);
        var covered = new boolean[width];
        var moved = new ArrayList<RawTableRegion>(regions.size() + 1);
        for (var region : regions) {
            if (region.bottom() < at) {
                moved.add(region);
            } else if (region.top() >= at) {
                moved.add(rows(region, region.top() + count, region.bottom() + count));
            } else {
                stretch(moved, region, at, count);
                for (var column = region.left(); column <= region.right(); column++) {
                    covered[column] = true;
                }
            }
        }
        if (source >= 0) {
            repeatRow(moved, source, at, at + count - 1, covered);
        }
        regions = moved;
        groupRows = shifted(groupRows, at, count);
        touchedRows = shifted(touchedRows, at, count);
        for (var row = Math.max(at - 1, 0); row <= at + count; row++) {
            touchedRows.add(row);
        }
        types = shiftedTypes(at, count);
        height += count;
    }

    /**
     * Removes rows.
     *
     * @param at    the first row removed
     * @param count how many rows are removed
     */
    public void rowsRemoved(int at, int count) {
        if (count > 0) {
            linesEdited(at, at + count - 1);
            editedRowMoved(at, -count);
        }
        removeRows(at, count);
    }

    /** Removes rows, or columns while the regions stand turned. */
    private void removeRows(int at, int count) {
        if (count <= 0) {
            return;
        }
        lastRegion = null;
        var end = at + count;
        var kept = new ArrayList<RawTableRegion>(regions.size());
        for (var region : regions) {
            var top = region.top() < at ? region.top() : Math.max(region.top() - count, at);
            var bottom = bottomLeft(region.bottom(), at, end);
            if (top <= bottom) {
                kept.add(rows(region, top, bottom));
            }
        }
        regions = kept;
        groupRows = groupsLeft(at, count);
        touchedRows = removed(touchedRows, at, count);
        touchedRows.add(Math.max(at - 1, 0));
        touchedRows.add(at);
        var moved = new HashMap<TableRegions.Cell, RawTableValueType>();
        types.forEach((cell, type) -> {
            if (cell.row() < at) {
                moved.put(cell, type);
            } else if (cell.row() >= end) {
                moved.put(new TableRegions.Cell(cell.row() - count, cell.column()), type);
            }
        });
        types = moved;
        height -= count;
    }

    /**
     * Stretches a region over the rows inserted inside it. The cells the table returns among others are cut around
     * them: an inserted row is none of them, and takes their role alone.
     */
    private static void stretch(List<RawTableRegion> into, RawTableRegion region, int at, int count) {
        if (!region.returned()) {
            into.add(rows(region, region.top(), region.bottom() + count));
            return;
        }
        into.add(rows(region, region.top(), at - 1));
        into.add(new RawTableRegion(region.role(), region.type(), at, region.left(), at + count - 1, region.right()));
        into.add(rows(region, at + count, region.bottom() + count));
    }

    /**
     * Gives the inserted rows, where no region stretched over them, the regions of the row they repeat.
     *
     * @param moved   the regions as the insert leaves them, which the repeated ones join
     * @param source  the row the inserted rows repeat, in the table before the insert
     * @param top     the first inserted row
     * @param bottom  the last inserted row
     * @param covered the columns a region stretched over the inserted rows
     */
    private void repeatRow(List<RawTableRegion> moved, int source, int top, int bottom, boolean[] covered) {
        for (var region : regions) {
            if (region.top() <= source && source <= region.bottom()) {
                addUncovered(moved, region, top, bottom, covered);
            }
        }
    }

    /**
     * The bottom row a region keeps once rows are removed: as it was above them, the row before them when it ended
     * among them, or moved up by as many below them.
     */
    private static int bottomLeft(int bottom, int at, int end) {
        if (bottom < at) {
            return bottom;
        }
        return bottom < end ? at - 1 : bottom - (end - at);
    }

    /**
     * Marks rows whose cells an edit restyled, so a table formatted before has them formatted again.
     *
     * @param at    the first row restyled
     * @param count how many rows are restyled
     */
    public void rowsRestyled(int at, int count) {
        for (var row = at; row < at + count; row++) {
            touchedRows.add(row);
        }
    }

    /**
     * Inserts columns.
     *
     * @param at    the column the first inserted column takes, the columns from it on moving right
     * @param count how many columns are inserted
     */
    public void columnsInserted(int at, int count) {
        structureEdited = true;
        transpose();
        insertRows(at, count);
        transpose();
    }

    /**
     * Removes columns.
     *
     * @param at    the first column removed
     * @param count how many columns are removed
     */
    public void columnsRemoved(int at, int count) {
        structureEdited = true;
        transpose();
        removeRows(at, count);
        transpose();
    }

    /**
     * The regions fitted to the table as it stands on its grid.
     *
     * <p>The header and the properties are laid out again from the grid, and the body moves by as many rows as they
     * gained or lost. The lines the table gained at its end take the roles of its last line, and the lines it lost
     * there take their regions along.
     *
     * @param grid the table as it stands on its grid, header included
     * @return the regions of the table
     */
    public TableRegions fittedTo(IGridTable grid) {
        return fittedTo(TableRegionLayout.headOf(grid), grid.getHeight(), grid.getWidth(), headerText(grid));
    }

    /**
     * The regions fitted to a table of the given head and size, as {@link #fittedTo(IGridTable)} fits them to a grid.
     *
     * @param head    the header and the properties of the table
     * @param rows    how many rows the table has
     * @param columns how many columns the table has
     * @param header  the text of the header
     * @return the regions of the table
     */
    public TableRegions fittedTo(TableRegionLayout.Head head, int rows, int columns, String header) {
        var oldHead = headHeight();
        bodyMoved |= head.height() != oldHead;
        if (head.height() > oldHead) {
            editedRowMoved(oldHead, head.height() - oldHead);
            insertRows(oldHead, head.height() - oldHead);
        } else {
            editedRowMoved(head.height(), head.height() - oldHead);
            removeRows(head.height(), oldHead - head.height());
        }
        fitRows(rows);
        dropLoneGroups();
        transpose();
        fitRows(columns);
        dropLoneGroups();
        transpose();
        var body = new ArrayList<RawTableRegion>(head.regions());
        for (var region : regions) {
            if (region.bottom() >= head.height()) {
                body.add(rows(region, Math.max(region.top(), head.height()), region.bottom()));
            }
        }
        var groups = new RawTableGroups(List.copyOf(groupRows.tailSet(head.height(), false)),
                List.copyOf(groupColumns));
        return new TableRegions(body, groups, SignatureSplitter.split(header), types);
    }

    /** How many rows the header and the properties take. */
    private int headHeight() {
        var head = 0;
        for (var region : regions) {
            if (region.role() == RawTableRole.SIGNATURE || region.role() == RawTableRole.PROPERTIES) {
                head = Math.max(head, region.bottom() + 1);
            }
        }
        return head;
    }

    /**
     * Drops the lines that start a group with no values before them, as a rule left first under the titles when the
     * rules above it are removed: a group sets lines of values apart.
     */
    private void dropLoneGroups() {
        groupRows.removeIf(row -> row < 1 || !holdsValues(row - 1));
    }

    /** Grows or shrinks the regions at the end of the table to the given number of rows. */
    private void fitRows(int rows) {
        if (rows > height) {
            insertRows(height, rows - height);
        } else if (rows < height) {
            removeRows(rows, height - rows);
        }
    }

    private static String headerText(IGridTable grid) {
        return Objects.requireNonNullElse(grid.getCell(0, 0).getStringValue(), "");
    }

    /**
     * The row whose roles the rows inserted at a place repeat: the row before when it holds values and stands not
     * alone, the row after otherwise, or the row before at the end of the table.
     *
     * @return the row, or {@code -1} for a table with no row
     */
    private int templateOf(int at) {
        var before = at - 1;
        if (before >= 0 && holdsValues(before) || at >= height) {
            return before;
        }
        return at;
    }

    /**
     * Whether a row holds a value and no cell that stands alone, such as a section, the result or a cell returned among
     * others.
     */
    private boolean holdsValues(int row) {
        var values = false;
        for (var region : regions) {
            if (region.top() <= row && row <= region.bottom()) {
                if (ALONE.contains(region.role()) || region.returned()) {
                    return false;
                }
                values |= VALUES.contains(region.role());
            }
        }
        return values;
    }

    /**
     * Adds the parts of a region's columns no other region covers yet on the inserted rows, as regions of its role. An
     * inserted row is none of the cells the table returns among others.
     */
    private static void addUncovered(List<RawTableRegion> into, RawTableRegion source, int top, int bottom,
                                     boolean[] covered) {
        var column = source.left();
        while (column <= source.right()) {
            if (covered[column]) {
                column++;
            } else {
                var end = column;
                while (end + 1 <= source.right() && !covered[end + 1]) {
                    end++;
                }
                into.add(new RawTableRegion(source.role(), source.type(), top, column, bottom, end));
                for (var at = column; at <= end; at++) {
                    covered[at] = true;
                }
                column = end + 1;
            }
        }
    }

    private static RawTableRegion rows(RawTableRegion region, int top, int bottom) {
        return region.withBounds(top, region.left(), bottom, region.right());
    }

    private static TreeSet<Integer> shifted(TreeSet<Integer> lines, int at, int count) {
        var moved = new TreeSet<Integer>();
        lines.forEach(line -> moved.add(line >= at ? line + count : line));
        return moved;
    }

    /**
     * The lines that start a group once lines are removed. Those after the removed lines move back by as many.
     *
     * <p>A group that started among the removed lines and goes on after them starts at the first line left of it, so
     * the line setting it apart from the group before stays. A group removed to the end of the table starts nowhere.
     */
    private TreeSet<Integer> groupsLeft(int at, int count) {
        var left = removed(groupRows, at, count);
        var end = at + count;
        if (end < height && !groupRows.subSet(at, end).isEmpty()) {
            left.add(at);
        }
        return left;
    }

    /** The lines left once lines are removed: those after the removed ones move back by as many. */
    private static TreeSet<Integer> removed(TreeSet<Integer> lines, int at, int count) {
        var left = new TreeSet<Integer>();
        lines.forEach(line -> {
            if (line < at) {
                left.add(line);
            } else if (line >= at + count) {
                left.add(line - count);
            }
        });
        return left;
    }

    private Map<TableRegions.Cell, RawTableValueType> shiftedTypes(int at, int count) {
        var moved = new HashMap<TableRegions.Cell, RawTableValueType>();
        types.forEach((cell, type) -> moved.put(cell.row() >= at
                ? new TableRegions.Cell(cell.row() + count, cell.column())
                : cell, type));
        return moved;
    }

    /** Swaps the rows and the columns, so a change of the columns is made as a change of the rows. */
    private void transpose() {
        var swapped = new ArrayList<RawTableRegion>(regions.size());
        regions.forEach(region -> swapped.add(region.withBounds(region.left(), region.top(), region.right(),
                region.bottom())));
        regions = swapped;
        lastRegion = null;
        var rows = groupRows;
        groupRows = groupColumns;
        groupColumns = rows;
        var touched = touchedRows;
        touchedRows = touchedColumns;
        touchedColumns = touched;
        var turnedTypes = new HashMap<TableRegions.Cell, RawTableValueType>();
        types.forEach((cell, type) -> turnedTypes.put(new TableRegions.Cell(cell.column(), cell.row()), type));
        types = turnedTypes;
        var rowCount = height;
        height = width;
        width = rowCount;
    }
}
