/**
 * Created Feb 17, 2007
 */
package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public interface IUndoGrid {
    void restoreCell(int cellID, IWritableGrid toGrid, int col, int row);

    int saveCell(IWritableGrid fromGrid, int col, int row);
}
