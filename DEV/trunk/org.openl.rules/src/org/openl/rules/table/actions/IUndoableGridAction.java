/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

import org.openl.rules.table.IWritableGrid;

/**
 * @author snshor
 *
 */
public interface IUndoableGridAction extends IUndoableAction {
    void doAction(IWritableGrid grid);

    void undoAction(IWritableGrid grid);

}
