/*
 * Created on Sep 22, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.poi;

/**
 * @author snshor
 *
 */
public class SheetCell {
    int x, y;

    static public String toString(int x, int y) {
        StringBuffer buf = new StringBuffer();
        int div = 'Z' - 'A' + 1;

        int xx = x;
        while (xx >= div) {
            int dd = xx / div;
            buf.append((char) ('A' + dd - 1));
            xx -= dd * div;
        }

        buf.append((char) ('A' + xx));

        buf.append(y + 1);
        return buf.toString();
    }

    public SheetCell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SheetCell)) {
            return false;
        }

        SheetCell c = (SheetCell) obj;
        return x == c.x && y == c.y;
    }

    @Override
    public int hashCode() {
        return (x * 37 + y) * 31;
    }

    @Override
    public String toString() {
        return toString(x, y);
    }

}
