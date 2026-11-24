package org.openl.rules.table;

/**
 * Class that represents transformer of coordinates inside logical table.
 * <p>
 * Coordinates of logical table will be transformed into coordinates of source table.
 *
 * @author PUdalau
 */
public interface CoordinatesTransformer {

    /**
     * @return The height of logical table.
     */
    int getHeight();

    /**
     * @return The width of logical table.
     */
    int getWidth();

    /**
     * @param col The column of logical table.
     * @param row The row of logical table.
     * @return Coordinates inside the source table.
     */
    int getColumn(int col, int row);

    /**
     * @param col The column of logical table.
     * @param row The row of logical table.
     * @return Coordinates inside the source table.
     */
    int getRow(int col, int row);

    /**
     * Whether the cell of logical table holds nothing.
     *
     * <p>Such a cell still stands at the coordinates of a cell of the source table, so it is located on the sheet as
     * that cell, but it gives no value.
     *
     * @param col The column of logical table.
     * @param row The row of logical table.
     */
    default boolean isEmpty(int col, int row) {
        return false;
    }

    /**
     * How many columns of logical table the cell spans.
     *
     * @param col The column of logical table.
     * @param row The row of logical table.
     * @return the number of columns, or {@code 0} when the cell spans as many columns as its cell of the source table
     */
    default int getCellWidth(int col, int row) {
        return 0;
    }

    /**
     * How many rows of logical table the cell spans.
     *
     * @param col The column of logical table.
     * @param row The row of logical table.
     * @return the number of rows, or {@code 0} when the cell spans as many rows as its cell of the source table
     */
    default int getCellHeight(int col, int row) {
        return 0;
    }
}
