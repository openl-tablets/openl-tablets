/**
 * Created Feb 17, 2007
 */
package org.openl.rules.table.actions;

import java.util.ArrayList;

/**
 * @author snshor
 *
 */
public class UndoableActions {

    private ArrayList<IUndoableAction> undoableActions = new ArrayList<IUndoableAction>();

    private int currentUndoIndex = 0;
    private int undoSize = 0;

    public void addNewAction(IUndoableAction iu) {
        undoableActions.ensureCapacity(currentUndoIndex + 1);
        if (currentUndoIndex < undoableActions.size()) {
            undoableActions.set(currentUndoIndex, iu);
        } else {
            undoableActions.add(iu);
        }
        ++currentUndoIndex;
        undoSize = currentUndoIndex;
    }

    public boolean hasRedo() {
        return currentUndoIndex < undoSize;
    }

    public boolean hasUndo() {
        return currentUndoIndex > 0;
    }

    public IUndoableAction getRedoAction() {
        return undoableActions.get(currentUndoIndex++);
    }

    public IUndoableAction getUndoAction() {
        return undoableActions.get(--currentUndoIndex);
    }

}
