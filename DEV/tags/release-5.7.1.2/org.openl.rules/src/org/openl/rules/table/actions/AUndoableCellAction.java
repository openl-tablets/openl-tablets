/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

import org.openl.rules.table.IUndoGrid;
import org.openl.rules.table.IWritableGrid;

/**
 * @author snshor
 *
 */
public abstract class AUndoableCellAction implements IUndoableGridAction {
    int col, row;

    int undoID;

    public AUndoableCellAction(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public void doAction(IWritableGrid wgrid, IUndoGrid undo) {
        save(wgrid, undo);
        doDirectChange(wgrid);
    }

    /**
     * @param grid
     */
    public abstract void doDirectChange(IWritableGrid wgrid);

    public void restore(IWritableGrid wgrid, IUndoGrid undo) {
        undo.restoreCell(undoID, wgrid, col, row);
    }

    public void save(IWritableGrid wgrid, IUndoGrid undo) {
        undoID = undo.saveCell(wgrid, col, row);

    }

    public void undoAction(IWritableGrid wgrid, IUndoGrid undo) {
        restore(wgrid, undo);
    }

}
