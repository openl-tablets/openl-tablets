/*
 * Created on Sep 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author snshor
 *
 */
public class GridSplitter {

    List<GridTable> tables = new ArrayList<GridTable>();

    IGrid grid;

    public GridSplitter(IGrid grid) {
        this.grid = grid;
    }

    boolean cellIsUsed(int col, int row) {
        for (Iterator<GridTable> iter = tables.iterator(); iter.hasNext();) {
            GridTable table = iter.next();
            if (GridTool.contains(table, col, row)) {
                return true;
            }
        }
        return false;
    }

    boolean containsCell(int x, int y) {
        return !grid.isEmpty(x, y) || grid.isPartOfTheMergedRegion(x, y);
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
        for (x = endX; containsCell(x, row); ++x) {
            ;
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

        tables.add(new GridTable(row, stX, y - 1, x - 1, grid));

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
        for (; containsCell(x, y); ++x) {
            ;
        }
        return x;
    }

    public GridTable[] split() {

        int nrows = grid.getMaxRowIndex() + 1;

        for (int row = grid.getMinRowIndex(); row < nrows; row++) {

            int ncells = grid.getMaxColumnIndex(row) + 1;
            if (ncells == 0) continue;

            for (int col = grid.getMinColumnIndex(row); col < ncells; col++) {

                // skip empty cell
                if (grid.isEmpty(col, row)) {
                    continue;
                }

                // check if this cell was used

                if (cellIsUsed(col, row)) {
                    continue;
                }

                defineTableBoundaries(col, row, col);
            }
        }

        return tables.toArray(new GridTable[tables.size()]);

    }

}
