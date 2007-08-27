/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table;


/**
 * @author snshor
 *
 */
public class UndoableSetValueAction extends AUndoableCellAction
{

	 String value;

	/**
	 * @param col
	 * @param row
	 */
	public UndoableSetValueAction(int col, int row, String value)
	{
		super(col, row);
		this.value = value;
	}

	
	public void doDirectChange(IWritableGrid wgrid)
	{
		wgrid.setCellStringValue(col, row, value);
	}


}
