/**
 * Created Feb 17, 2007
 */
package org.openl.rules.table;


/**
 * @author snshor
 *
 */
public interface IUndoGrid
{
	int saveCell(IWritableGrid fromGrid, int col, int row);
	void restoreCell(int cellID, IWritableGrid toGrid, int col, int row);
}
