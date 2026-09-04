/**
 * Created Feb 17, 2007
 */
package org.openl.rules.table.actions;

import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.openl.rules.table.IGridTable;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class UndoableCompositeAction implements IUndoableGridTableAction {

    final List<IUndoableGridTableAction> actions;

    public UndoableCompositeAction(IUndoableGridTableAction... gridActions) {
        this.actions = Arrays.asList(gridActions);
    }

    @Override
    public void doAction(IGridTable table) {
        for (IUndoableGridTableAction action : actions) {
            action.doAction(table);
        }
    }

    @Override
    public void undoAction(IGridTable table) {
        for (var iter = actions.listIterator(actions.size()); iter.hasPrevious(); ) {
            var action = iter.previous();
            action.undoAction(table);
        }
    }

}
