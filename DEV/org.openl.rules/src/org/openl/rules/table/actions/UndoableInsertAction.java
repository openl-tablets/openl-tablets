package org.openl.rules.table.actions;

import java.util.ArrayList;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

/**
 * Default behaviour for insert operations.
 *
 * @author DLiauchuk
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class UndoableInsertAction extends UndoableEditTableAction {

    protected final MetaInfoWriter metaInfoWriter;

    /**
     * How many rows or columns are laid down: what the caller asked for, and nothing beyond it.
     * <p>
     * A merge the new line lands beside grows over it — {@code GridTool} resizes it — and that is the whole of
     * what a merge changes here. The count itself is the caller's: a caller that means to lay a line down past a
     * merged block says so by the index it inserts at.
     */
    protected final int lines;

    @Override
    public void doAction(IGridTable table) {
        if (!canPerformAction(table)) {
            moveTable(table, metaInfoWriter);
        }
        IGridRegion fullTableRegion = getOriginalRegion(table);
        var actions = new ArrayList<IUndoableGridTableAction>();
        actions.add(performAction(fullTableRegion, table));
        actions.add(getGridRegionAction(fullTableRegion));
        if (isDecoratorTable(table)) {
            actions.add(getGridRegionAction(table.getRegion()));
        }
        new UndoableCompositeAction(actions).doAction(table);
    }

    /**
     * Checks if action can be performed without moving the table.
     *
     * @param table a table to apply the action.
     * @return true if action can be performed without moving the table.
     */
    protected abstract boolean canPerformAction(IGridTable table);

    /**
     * Perform action for inserting rows or columns.
     *
     * @param fullTableRegion a region of original table
     * @param table           a table to apply the action.
     * @return action for inserting rows or columns.
     */
    protected abstract IUndoableGridTableAction performAction(IGridRegion fullTableRegion, IGridTable table);

    protected abstract GridRegionAction getGridRegionAction(IGridRegion gridRegion);

}
