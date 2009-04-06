/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table;


/**
 * @author snshor
 *
 */
public interface IUndoableGridAction extends IUndoableAction
{
	void doAction(IWritableGrid grid, IUndoGrid undo);
	void undoAction(IWritableGrid grid, IUndoGrid undo);
	

}
