package org.openl.rules.table;

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

}
