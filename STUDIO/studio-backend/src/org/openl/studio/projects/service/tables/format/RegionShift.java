package org.openl.studio.projects.service.tables.format;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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
 * none.
 *
 * <p>Once the edit is done, the regions are fitted to the table as it stands: the header and the properties are laid
 * out again from the grid, a table grown at its end takes the roles of its last line for the lines it gained, and a
 * table that lost lines at its end loses their regions. The pieces of the header are read again from its text.
 *
 * <p>The regions are named in table coordinates, as {@link TableRegions} names them.
 *
 * @author Yury Molchan
 */
public final class RegionShift {

    /** The roles of the cells a line holds when it is a line of values, which a line inserted next to it repeats. */
    private static final Set<RawTableRole> VALUES = EnumSet.of(RawTableRole.VALUE, RawTableRole.OUTPUT);

    /** The roles of a line that stands alone, which a line inserted next to it never repeats. */
    private static final Set<RawTableRole> ALONE = EnumSet.of(RawTableRole.SECTION, RawTableRole.RESULT);

    private List<RawTableRegion> regions;
    private TreeSet<Integer> groupRows;
    private TreeSet<Integer> groupColumns;
    private Map<TableRegions.Cell, RawTableValueType> types;
    private int height;
    private int width;

    private RegionShift(TableRegions regions, int height, int width) {
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
     * Inserts rows.
     *
     * @param at    the row the first inserted row takes, the rows from it on moving down
     * @param count how many rows are inserted
     */
    public void rowsInserted(int at, int count) {
        if (count <= 0) {
            return;
        }
        var source = templateOf(at);
        var covered = new boolean[width];
        var moved = new ArrayList<RawTableRegion>(regions.size() + 1);
        for (var region : regions) {
            if (region.bottom() < at) {
                moved.add(region);
            } else if (region.top() >= at) {
                moved.add(rows(region, region.top() + count, region.bottom() + count));
            } else {
                moved.add(rows(region, region.top(), region.bottom() + count));
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
        if (count <= 0) {
            return;
        }
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
        var groups = new TreeSet<Integer>();
        groupRows.forEach(row -> {
            if (row < at) {
                groups.add(row);
            } else if (row >= end) {
                groups.add(row - count);
            }
        });
        groupRows = groups;
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
     * Inserts columns.
     *
     * @param at    the column the first inserted column takes, the columns from it on moving right
     * @param count how many columns are inserted
     */
    public void columnsInserted(int at, int count) {
        transpose();
        rowsInserted(at, count);
        transpose();
    }

    /**
     * Removes columns.
     *
     * @param at    the first column removed
     * @param count how many columns are removed
     */
    public void columnsRemoved(int at, int count) {
        transpose();
        rowsRemoved(at, count);
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
        if (head.height() > oldHead) {
            rowsInserted(oldHead, head.height() - oldHead);
        } else {
            rowsRemoved(head.height(), oldHead - head.height());
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
            rowsInserted(height, rows - height);
        } else if (rows < height) {
            rowsRemoved(rows, height - rows);
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

    /** Whether a row holds a value and no cell that stands alone, such as a section or the result. */
    private boolean holdsValues(int row) {
        var values = false;
        for (var region : regions) {
            if (region.top() <= row && row <= region.bottom()) {
                if (ALONE.contains(region.role())) {
                    return false;
                }
                values |= VALUES.contains(region.role());
            }
        }
        return values;
    }

    /** Adds the parts of a region's columns no other region covers yet on the inserted rows, as regions of its role. */
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
        return new RawTableRegion(region.role(), region.type(), top, region.left(), bottom, region.right());
    }

    private static TreeSet<Integer> shifted(TreeSet<Integer> lines, int at, int count) {
        var moved = new TreeSet<Integer>();
        lines.forEach(line -> moved.add(line >= at ? line + count : line));
        return moved;
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
        var turned = new ArrayList<RawTableRegion>(regions.size());
        regions.forEach(region -> turned.add(new RawTableRegion(region.role(), region.type(), region.left(),
                region.top(), region.right(), region.bottom())));
        regions = turned;
        var rows = groupRows;
        groupRows = groupColumns;
        groupColumns = rows;
        var turnedTypes = new HashMap<TableRegions.Cell, RawTableValueType>();
        types.forEach((cell, type) -> turnedTypes.put(new TableRegions.Cell(cell.column(), cell.row()), type));
        types = turnedTypes;
        var rowCount = height;
        height = width;
        width = rowCount;
    }
}
