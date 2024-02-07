package org.openl.rules.table.actions;

import org.openl.rules.table.IGridTable;

/**
 * @author Andrei Astrouski
 */
public interface IUndoableGridTableAction extends IUndoableAction {

    void doAction(IGridTable grid);

    void undoAction(IGridTable grid);

}
