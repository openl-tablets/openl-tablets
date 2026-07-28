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
        var resultTable = table;

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
    public static List<IGridRegion> getGridRegions(ILogicalTable table) {
        var height = table.getHeight();
        var width = table.getWidth();
        var regions = new ArrayList<IGridRegion>();

        // Go through all possible cells
        for (var row = 0; row < height; row++) {
            for (var column = 0; column < width; column++) {
                var cell = table.getCell(column, row);
                regions.add(cell.getAbsoluteRegion());
            }
        }
        return regions;
    }

    public static boolean isSingleCellTable(ILogicalTable table) {
        return table.getHeight() == 1 && table.getWidth() == 1;
    }
}
