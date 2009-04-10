/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table;

/**
 * @author snshor
 * 
 */
public class UndoableCopyValueAction extends AUndoableCellAction
{

	int colFrom, rowFrom;


	GridRegion toRestore, toRemove;

	/**
	 * @param col
	 * @param row
	 */
	public UndoableCopyValueAction(int colFrom, int rowFrom, int colTo, int rowTo)
	{
		super(colTo, rowTo);
		this.colFrom = colFrom;
		this.rowFrom = rowFrom;
	}

	public void doDirectChange(IWritableGrid wgrid)
	{

		wgrid.copyCell(colFrom, rowFrom, col, row);
		moveRegion(wgrid);
	}

	void moveRegion(IWritableGrid wgrid)
	{
		IGridRegion rrFrom = wgrid.getRegionStartingAt(colFrom, rowFrom);
		IGridRegion rrTo = wgrid.getRegionStartingAt(col, row);

		if (rrFrom == null && rrTo == null)
			return;

		// if (rrFrom != null && rrTo != null &&
		// IGridRegion.Tool.width(rrTo) == IGridRegion.Tool.width(rrFrom)
		// && IGridRegion.Tool.height(rrTo) == IGridRegion.Tool.height(rrFrom)
		// )
		// return;

		if (rrTo != null)
		{
			toRestore = new GridRegion(rrTo);
			wgrid.removeMergedRegion(toRestore);
		}

		if (rrFrom != null)
		{
			GridRegion copyFrom = new GridRegion(rrFrom.getTop() + row - rowFrom,
					rrFrom.getLeft() + col - colFrom, rrFrom.getBottom() + row - rowFrom,
					rrFrom.getRight() + col - colFrom);
			wgrid.addMergedRegion(copyFrom);
			toRemove = copyFrom;
		}

	}

	public void restore(IWritableGrid wgrid, IUndoGrid undo)
	{

		if (toRemove != null)
			wgrid.removeMergedRegion(toRemove);
		if (toRestore != null)
			wgrid.addMergedRegion(toRestore);
		super.restore(wgrid, undo);
	}

}
