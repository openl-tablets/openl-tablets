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

    public CellKey(int col, int row) {
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
        return col * 37 + row;
    }

    @Override
    public boolean equals(Object obj) {
        CellKey k = (CellKey) obj;
        return col == k.col && row == k.row;
    }

    @Override
    public String toString() {
        return String.format("row: %d, col: %d", row, col);
    }

}
