/**
 * Created Feb 17, 2007
 */
package org.openl.rules.table;

import java.util.Iterator;
import java.util.List;

/**
 * @author snshor
 *
 */
public class UndoableCompositeAction implements IUndoableGridAction {

    List<IUndoableGridAction> actions;

    public UndoableCompositeAction(List<IUndoableGridAction> actions) {
        this.actions = actions;
    }

    public void doAction(IWritableGrid grid, IUndoGrid undo) {
        for (Iterator<IUndoableGridAction> iter = actions.iterator(); iter.hasNext();) {
            IUndoableGridAction action = iter.next();
            action.doAction(grid, undo);
        }
    }

    public void undoAction(IWritableGrid grid, IUndoGrid undo) {
        for (Iterator<IUndoableGridAction> iter = actions.iterator(); iter.hasNext();) {
            IUndoableGridAction action = iter.next();
            action.undoAction(grid, undo);
        }

    }

}
