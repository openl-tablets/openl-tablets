package org.openl.rules.table;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

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

        while (resultTable instanceof AGridTableDecorator decorator) {
            resultTable = decorator.getOriginalGridTable();
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

    /**
     * Whether a table is assembled from parts written apart from one another, such as on several sheets.
     *
     * <p>Such a table stands on no sheet of its own, so nothing can be written into it.
     *
     * @param table the grid the table stands on, or {@code null} for a table that has none
     * @return {@code true} if the table is assembled from parts, {@code false} otherwise
     */
    public static boolean isAssembledFromParts(@Nullable IGridTable table) {
        return table != null && table.getGrid() instanceof CompositeGrid;
    }
}
