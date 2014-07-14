package org.openl.rules.table;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrei Ostrovski
 */
public class GridTableUtils {

    private GridTableUtils() { }

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
     * @param table the table with regions.
     * @return a the regions of the table.
     */
    public static List<IGridRegion> getGridRegions(IGridTable table) {
        int height = table.getHeight();
        int width = table.getWidth();

        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        ICell cell = null;
        for (int row = 0; row < height; row += cell.getHeight()) {
            for (int column = 0; column < width; column += cell.getWidth()) {
                cell = table.getCell(column, row);
                regions.add(cell.getAbsoluteRegion());
            }
        }
        return regions;
    }
}
