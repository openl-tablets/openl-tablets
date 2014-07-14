package org.openl.rules.table;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrei Ostrovski, Yury Molchan
 */
public class GridTableUtils {

    private GridTableUtils() {
    }

    /**
     * Extracts original table.
     *
     * @param table Table.
     * @return Original table if table is decorator and current table otherwise.
     */
    public static IGridTable getOriginalTable(IGridTable table) {
        IGridTable resultTable = table;

        while (resultTable instanceof AGridTableDecorator) {
            resultTable = ((AGridTableDecorator) resultTable).getOriginalGridTable();
        }

        return resultTable;
    }

    /**
     * Returns all regions of a table.
     *
     * @param table the table with regions.
     * @return a the regions of the table.
     */
    public static List<IGridRegion> getGridRegions(IGridTable table) {
        int height = table.getHeight();
        int width = table.getWidth();
        // create a matrix of processed cells
        // true - has processed
        // false - has not processed yet
        boolean[][] mask = new boolean[height][width];
        List<IGridRegion> regions = new ArrayList<IGridRegion>();

        // Go through all possible cells
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                // If a cell is marked as processed skip it
                if (mask[row][column]) {
                    continue;
                }
                // store a region of the cell
                ICell cell = table.getCell(column, row);
                regions.add(cell.getAbsoluteRegion());
                // mark this cell as processed, include merged cells (mark the whole region)
                for (int h = 0; h < cell.getHeight(); h++) {
                    for (int w = 0; w < cell.getWidth(); w++) {
                        mask[row + h][column + w] = true;
                    }
                }
            }
        }
        return regions;
    }
}
