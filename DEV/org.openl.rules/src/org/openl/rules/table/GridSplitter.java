/*
 * Created on Sep 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

import java.util.ArrayList;
import java.util.List;

/**
 * @author snshor
 *
 */
public class GridSplitter {

    private List<IGridTable> tables = new ArrayList<>();
    private RegionsPool pool = new RegionsPool(null);

    private IGrid grid;

    public GridSplitter(IGrid grid) {
        this.grid = grid;
    }

    boolean cellIsUsed(int col, int row) {
        return pool.getRegionContaining(col, row) != null;
    }

    boolean containsCell(int column, int row) {
        if (!grid.isEmpty(column, row)) {
            return true;// not empty cell
        }
        if (grid.isPartOfTheMergedRegion(column, row)) {
            IGridRegion region = grid.getRegionContaining(column, row);
            if (!grid.isEmpty(region.getLeft(), region.getTop())) {
                return true;// part of not empty merged cell
            }
        }
        return false;
    }

    boolean containsRow(int scol, int ecol, int row) {
        for (int col = scol; col < ecol; col++) {
            if (containsCell(col, row)) {
                return true;
            }
        }
        return false;
    }

    void defineTableBoundaries(int col, int row, int endX) {
        int y, x;
        int stX = col;
        x = endX;
        while (containsCell(x, row)) {
            ++x;
        }
        for (y = row; containsRow(col, x, y); ++y) {
            int newX = expandLeft(y, stX);
            if (newX < stX) {
                defineTableBoundaries(newX, row, x);
                return;
            }

            int newEndX = expandRight(y, x);
            if (newEndX > x) {
                defineTableBoundaries(stX, row, newEndX);
                return;
            }

        }

        IGridTable table = new GridTable(row, stX, y - 1, x - 1, grid);
        tables.add(table);
        pool.add(table.getRegion());
    }

    private int expandLeft(int y, int stX) {
        for (int x = stX;; --x) {
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

        int nrows = grid.getMaxRowIndex() + 1;

        for (int row = grid.getMinRowIndex(); row < nrows; row++) {

            int ncells = grid.getMaxColumnIndex(row) + 1;
            if (ncells == 0) {
                continue;
            }

            for (int col = grid.getMinColumnIndex(row); col < ncells; col++) {

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

        return tables.toArray(new IGridTable[0]);

    }

}
