/**
 * Created Feb 17, 2007
 */
package org.openl.rules.table.actions;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.openl.rules.table.IWritableGrid;

/**
 * @author snshor
 * 
 */
public class UndoableCompositeAction implements IUndoableGridAction {

    List<IUndoableGridAction> actions;

    public UndoableCompositeAction(List<IUndoableGridAction> actions) {
        this.actions = actions;
    }

    public UndoableCompositeAction(IUndoableGridAction... gridActions) {
        this.actions = Arrays.asList(gridActions);
    }

    public void doAction(IWritableGrid grid) {
        for (Iterator<IUndoableGridAction> iter = actions.iterator(); iter.hasNext();) {
            IUndoableGridAction action = iter.next();
            action.doAction(grid);
        }
    }

    public void undoAction(IWritableGrid grid) {
        for (ListIterator<IUndoableGridAction> iter = actions.listIterator(actions.size()); iter.hasPrevious();) {
            IUndoableGridAction action = iter.previous();
            action.undoAction(grid);
        }
    }
}
