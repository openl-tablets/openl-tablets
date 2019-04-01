package org.openl.rules.table;

/**
 * Class that represents transformer of coordinates inside logical table.
 *
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
}
