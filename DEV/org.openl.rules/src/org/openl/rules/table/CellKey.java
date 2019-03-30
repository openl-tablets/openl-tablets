/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public class CellKey {

    private int col;
    private int row;

    private CellKey(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getColumn() {
        return col;
    }

    public int getRow() {
        return row;
    }

    @Override
    public int hashCode() {
        return col * 773 + row;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CellKey k = (CellKey) obj;
        return col == k.col && row == k.row;
    }

    @Override
    public String toString() {
        return String.format("row: %d, col: %d", row, col);
    }

    private static final CellKey ZERO_ZERO = new CellKey(0, 0);
    private static final CellKey ZERO_ONE = new CellKey(0, 1);
    private static final CellKey ONE_ZERO = new CellKey(1, 0);
    private static final CellKey ONE_ONE = new CellKey(1, 1);

    public static final class CellKeyFactory {
        private CellKeyFactory() {
        }

        public static CellKey getCellKey(int col, int row) {
            if (col == 0 && row == 0) {
                return ZERO_ZERO;
            } else if (col == 0 && row == 1) {
                return ZERO_ONE;
            } else if (col == 1 && row == 0) {
                return ONE_ZERO;
            } else if (col == 1 && row == 1) {
                return ONE_ONE;
            } else {
                return new CellKey(col, row);
            }
        }
    }

}
