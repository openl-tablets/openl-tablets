/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

/**
 * @author snshor
 *
 */
public abstract class AUndoableCellAction implements IUndoableGridTableAction {

    int col, row;

    public AUndoableCellAction(int col, int row) {
        this.col = col;
        this.row = row;
    }

}
