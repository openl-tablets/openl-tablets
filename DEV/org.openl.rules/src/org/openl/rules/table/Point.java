package org.openl.rules.table;

/**
 * Handles two coordinates: column number and row number.
 *
 */
public abstract class Point {

    private static final byte MAX_COLUMN = 10;
    private static final byte MAX_ROW = 100;
    private static final Point[][] POINTS = new Point[MAX_COLUMN][MAX_ROW];

    private Point() {
        // Disable inheritance
    }

    static {
        for (byte i = 0; i < MAX_COLUMN; i++) {
            for (byte j = 0; j < MAX_ROW; j++) {
                final byte column = i;
                final byte row = j;
                POINTS[i][j] = new Point() {
                    @Override
                    public int getColumn() {
                        return column;
                    }

                    @Override
                    public int getRow() {
                        return row;
                    }
                };

            }
        }
    }

    public static Point get(int column, int row) {
        if (column < MAX_COLUMN && row < MAX_ROW) {
            return POINTS[column][row]; // Cached
        }
        if (((column & 0xFFFFFF00) | (row & 0xFF000000)) == 0) {
            return new CompressedPoint(column, row); // Compressed
        }
        return new BigPoint(column, row);
    }

    public abstract int getColumn();

    public abstract int getRow();

    private static final class CompressedPoint extends Point {
        private final int compressed;

        CompressedPoint(int column, int row) {
            this.compressed = row | (column << 24);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return compressed == ((CompressedPoint) o).compressed;
        }

        @Override
        public int hashCode() {
            return compressed;
        }

        @Override
        public int getColumn() {
            return (compressed & 0xFF000000) >> 24;
        }

        @Override
        public int getRow() {
            return compressed & 0x00FFFFFF;
        }
    }

    private static final class BigPoint extends Point {
        final int column, row;

        private BigPoint(int column, int row) {
            this.column = column;
            this.row = row;
        }

        @Override
        public int getColumn() {
            return column;
        }

        @Override
        public int getRow() {
            return row;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BigPoint bigPoint = (BigPoint) o;

            return column == bigPoint.column && row == bigPoint.row;
        }

        @Override
        public int hashCode() {
            return 31 * column + row;
        }
    }
}
