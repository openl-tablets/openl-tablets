/*
 * Created on Sep 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class GridSplitter {

    private final List<IGridTable> tables = new ArrayList<>();
    private final RegionsPool pool = new RegionsPool(null);

    private final IGrid grid;

    boolean cellIsUsed(int col, int row) {
        return pool.getRegionContaining(col, row) != null;
    }

    boolean containsCell(int column, int row) {
        return containsCell(grid, column, row);
    }

    /**
     * Tells whether a cell is one OpenL reads as part of a table.
     *
     * <p>A cell carries content when it holds a value of its own, or when a merge spanning it starts from a cell
     * that holds one. Every other cell is blank, and a whole line of blank cells is where a table ends.
     *
     * @param grid   the grid the cell belongs to
     * @param column absolute column of the cell
     * @param row    absolute row of the cell
     */
    public static boolean containsCell(IGrid grid, int column, int row) {
        if (!grid.isEmpty(column, row)) {
            return true;// not empty cell
        }
        var region = grid.getRegionContaining(column, row);
        return region != null && !grid.isEmpty(region.getLeft(), region.getTop());// part of not empty merged cell
    }

    boolean containsRow(int scol, int ecol, int row) {
        for (var col = scol; col < ecol; col++) {
            if (containsCell(col, row)) {
                return true;
            }
        }
        return false;
    }

    void defineTableBoundaries(int col, int row, int endX) {
        int y, x;
        x = endX;
        while (containsCell(x, row)) {
            ++x;
        }
        for (y = row; containsRow(col, x, y); ++y) {
            var newX = expandLeft(y, col);
            if (newX < col) {
                defineTableBoundaries(newX, row, x);
                return;
            }

            var newEndX = expandRight(y, x);
            if (newEndX > x) {
                defineTableBoundaries(col, row, newEndX);
                return;
            }

        }

        var table = new GridTable(row, col, y - 1, x - 1, grid);
        tables.add(table);
        pool.add(table.getRegion());
    }

    private int expandLeft(int y, int stX) {
        for (var x = stX; ; --x) {
            if (x <= 0) {
                return 0;
            }
            if (containsCell(x - 1, y)) {
                continue;
            }
            return x;
        }

    }

    private int expandRight(int y, int x) {
        while (containsCell(x, y)) {
            ++x;
        }
        return x;
    }

    public IGridTable[] split() {

        var nrows = grid.getMaxRowIndex() + 1;

        for (var row = grid.getMinRowIndex(); row < nrows; row++) {

            var ncells = grid.getMaxColumnIndex(row) + 1;
            if (ncells == 0) {
                continue;
            }

            for (var col = grid.getMinColumnIndex(row); col < ncells; col++) {

                // check if this cell was used
                if (cellIsUsed(col, row)) {
                    continue;
                }

                // skip empty cell
                if (grid.isEmpty(col, row)) {
                    continue;
                }

                defineTableBoundaries(col, row, col);
            }
        }

        return tables.toArray(IGridTable.EMPTY_GRID);

    }

}
