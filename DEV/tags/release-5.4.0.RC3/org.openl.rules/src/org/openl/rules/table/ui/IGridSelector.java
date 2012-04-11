/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui;

/**
 * @author snshor
 *
 */
public interface IGridSelector {
    static public class Coord {
        public int col, row;
    }

    boolean selectCoords(int col, int row);
}
