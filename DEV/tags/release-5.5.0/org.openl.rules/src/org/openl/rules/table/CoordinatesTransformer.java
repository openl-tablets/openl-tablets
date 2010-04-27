package org.openl.rules.table;

/**
 * Class that represents transformer of coordinates inside logical table.
 * 
 * Coordinates of logical table will be transformed into coordinates of source
 * table.
 * 
 * @author PUdalau
 */
public interface CoordinatesTransformer {
    public static class Point {
        private int column;
        private int row;

        public Point(int column, int row) {
            this.column = column;
            this.row = row;
        }

        public int getColumn() {
            return column;
        }

        public int getRow() {
            return row;
        }
    }

    /**
     * @param column The column of logical table.
     * @param row The row of logical table.
     * @return Coordinates inside the source table.
     */
    Point calculateCoordinates(int column, int row);

    /**
     * @return The height of logical table.
     */
    int getHeight();

    /**
     * @return The width of logical table.
     */
    int getWidth();
}
