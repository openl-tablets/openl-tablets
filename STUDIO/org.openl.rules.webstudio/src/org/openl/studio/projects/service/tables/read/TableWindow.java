package org.openl.studio.projects.service.tables.read;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

/**
 * The slice of a table a read answers: the row it opens on, and how many rows it holds.
 *
 * <p>A window never cuts a merged cell in two, at either edge. A merged cell is one cell of the table
 * wherever the window's edge happens to fall, and a window that ended inside one would answer it twice —
 * clamped to its own last row, and rooted in the next window at a place that holds nothing of its own. A
 * screen reading a table window by window would then hold two groups where the workbook holds one and act on
 * each of them apart: a row taken away from the first leaves the rules of the second standing.
 *
 * <p>So the window runs past the count asked for to the end of a merge it would have cut, and opens on the
 * first row of a merge the row asked for stands inside. Windows tile the table exactly, and a reader asking
 * for the next one from the end of the one it has never lands inside a merge at all.
 *
 * @param startRow the table's own row the window opens on
 * @param rows     how many rows it holds, or {@link #EVERY_ROW} for the rest of the table
 */
record TableWindow(int startRow, int rows) {

    /** Asked for instead of a count, holds the table to its end. */
    static final int EVERY_ROW = -1;

    /**
     * The window a read of {@code maxRows} rows from {@code startRow} answers with.
     *
     * @param table    the table being read
     * @param startRow the first row asked for; {@code null} opens at the top
     * @param maxRows  how many rows were asked for; {@code null} reads to the end
     */
    static TableWindow of(IGridTable table, @Nullable Integer startRow, @Nullable Integer maxRows) {
        var from = openedOn(table, startRow == null ? 0 : startRow);
        return new TableWindow(from, maxRows == null ? EVERY_ROW : wholeRows(table, from, maxRows));
    }

    /** Whether the window holds fewer rows than the table has. */
    boolean partial(int height) {
        return startRow > 0 || (rows != EVERY_ROW && rows < height);
    }

    /** The first row of the merged cell the given row stands inside, or that row itself. */
    private static int openedOn(IGridTable table, int startRow) {
        var region = table.getRegion();
        var first = region.getTop() + startRow;
        // Past the last row there is no window at all, and nothing to open it on.
        if (startRow <= 0 || first > region.getBottom()) {
            return Math.max(startRow, 0);
        }
        for (var grown = true; grown; ) {
            grown = false;
            for (var merged : mergesOf(table)) {
                if (merged.getTop() < first && merged.getBottom() >= first) {
                    first = Math.max(merged.getTop(), region.getTop());
                    grown = true;
                }
            }
        }
        return first - region.getTop();
    }

    /** The rows asked for, and then as many more as it takes to reach the end of a merge they would cut. */
    private static int wholeRows(IGridTable table, int startRow, int maxRows) {
        var region = table.getRegion();
        var last = Math.min(region.getTop() + startRow + maxRows - 1, region.getBottom());
        for (var grown = true; grown; ) {
            grown = false;
            for (var merged : mergesOf(table)) {
                if (merged.getTop() <= last && merged.getBottom() > last) {
                    last = Math.min(merged.getBottom(), region.getBottom());
                    grown = true;
                }
            }
        }
        return last - region.getTop() - startRow + 1;
    }

    /** The merged cells of the table itself — the grid holds the merges of every table on the sheet. */
    private static Iterable<IGridRegion> mergesOf(IGridTable table) {
        var region = table.getRegion();
        var grid = table.getGrid();
        var merges = new java.util.ArrayList<IGridRegion>();
        for (var i = 0; i < grid.getNumberOfMergedRegions(); i++) {
            var merged = grid.getMergedRegion(i);
            if (IGridRegion.Tool.contains(region, merged.getLeft(), merged.getTop())) {
                merges.add(merged);
            }
        }
        return merges;
    }
}
