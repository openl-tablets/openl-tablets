/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.rules.ui;

import java.io.IOException;
import java.util.Vector;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IUndoGrid;
import org.openl.rules.table.IUndoableAction;
import org.openl.rules.table.IUndoableGridAction;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.UndoableActions;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.XlsUndoGrid;

/**
 * @author snshor
 *
 */
public class TableEditorModel
{
	
	static final boolean COLUMNS = true, ROWS = false, INSERT = true, REMOVE = false;
	
	IGridTable table;
	
	GridRegion region;
	GridTable[] othertables;

	UndoableActions actions = new UndoableActions();
	
	XlsUndoGrid undoGrid =  new XlsUndoGrid(); 
	
	public TableEditorModel(IGridTable table)
	{
		this.table = table;
		this.region = new GridRegion(table.getRegion());
		othertables = new GridSplitter(table.getGrid()).split();
		removeThisTable(othertables);
	}
	
	
	
	
	/**
	 * @param othertables
	 * 
	 */
	private void removeThisTable(GridTable[] othertables)
	{
		Vector v = new Vector();
		for (int i = 0; i < othertables.length; i++)
		{
			if (!IGridRegion.Tool.intersects(othertables[i], region))
			{
				v.add(othertables[i]);
			}	
		}
		
		this.othertables = (GridTable[])v.toArray(new GridTable[0]);
	}
	
	




	IWritableGrid wgrid()
	{
		return (IWritableGrid)table.getGrid();
	}
	
	public void insertRows(int nRows, int beforeRow)
	{
		IUndoableGridAction ua = IWritableGrid.Tool.insertRows(nRows, beforeRow, region, wgrid());
		RegionAction ra = new RegionAction(ua, ROWS, INSERT, nRows);
		ra.doSome(region, wgrid(), undoGrid);
		actions.addNewAction(ra);
	}

	public void insertColumns(int nCols, int beforeCol)
	{
		IUndoableGridAction ua = IWritableGrid.Tool.insertColumns(nCols, beforeCol, region, wgrid());
		RegionAction ra = new RegionAction(ua, COLUMNS, INSERT, nCols);
		ra.doSome(region, wgrid(), undoGrid);
		actions.addNewAction(ra);
	}

	public void removeRows(int nRows, int beforeRow)
	{
		IUndoableGridAction ua = IWritableGrid.Tool.removeRows(nRows, beforeRow, region);
		RegionAction ra = new RegionAction(ua, ROWS, REMOVE, nRows);
		ra.doSome(region, wgrid(), undoGrid);
		actions.addNewAction(ra);
	}

	
	public void removeColumns(int nCols, int beforeCol)
	{
		IUndoableGridAction ua = IWritableGrid.Tool.removeColumns(nCols, beforeCol, region);
		RegionAction ra = new RegionAction(ua, COLUMNS, REMOVE, nCols);
		ra.doSome(region, wgrid(), undoGrid);
		actions.addNewAction(ra);
	}

	public void setCellValue(int col, int row, String value)
	{
		IUndoableGridAction ua = IWritableGrid.Tool.setStringValue(col, row, region, value);
		RegionAction ra = new RegionAction(ua, ROWS, REMOVE, 0);
		ra.doSome(region, wgrid(), undoGrid);
		actions.addNewAction(ra);
	}

	
	
	
	static class RegionAction  implements IUndoableAction
	{
		IUndoableGridAction gridAction;
		
		boolean isInsert;
		boolean isColumns;
		int nRowsOrColumns;
		
		
		public RegionAction(IUndoableGridAction action, boolean isColumns, boolean isInsert, int nRowsOrColumns)
		{
			this.gridAction = action;
			this.isColumns = isColumns;
			this.isInsert = isInsert;
			this.nRowsOrColumns = nRowsOrColumns;
		}

		public void doSome(GridRegion r, IWritableGrid wgrid, IUndoGrid undoGrid)
		{
			gridAction.doAction(wgrid, undoGrid);
			updateRegion(isInsert, isColumns, nRowsOrColumns, r);
		}
		
		/**
		 * @param isInsert2
		 * @param isColumns2
		 * @param rowsOrColumns
		 */
		void updateRegion(boolean isInsert, boolean isColumns, int rowsOrColumns, GridRegion r)
		{
			int inc = isInsert ?  rowsOrColumns : -rowsOrColumns;
			if (isColumns)
				r.setRight(r.getRight() + inc);
			else
				r.setBottom(r.getBottom() + inc);
		}

		public void undoSome(GridRegion r, IWritableGrid wgrid, IUndoGrid undoGrid)
		{
			updateRegion(!isInsert, isColumns, nRowsOrColumns, r);
			gridAction.undoAction(wgrid, undoGrid);
		}
	}
	
	
	public boolean hasUndo()
	{
		return actions.hasUndo();
	}
	
	public boolean hasRedo()
	{
		return actions.hasRedo();
	}
	
	
	public void undo()
	{
		IUndoableAction ua = actions.undo();
		((RegionAction)ua).undoSome(region, wgrid(), undoGrid);
	}
	
	public void redo()
	{
		IUndoableAction ua = actions.redo();
		((RegionAction)ua).doSome(region, wgrid(), undoGrid);
	}
	
	public void cancel()
	{
		while(actions.hasUndo())
			undo();
	}
	
	public void save() throws IOException
	{
		XlsSheetGridModel xlsgrid =  (XlsSheetGridModel)table.getGrid();
		xlsgrid.getSheetSource().getWorkbookSource().save();
	}
	
	public void saveAs(String fname) throws IOException
	{
		XlsSheetGridModel xlsgrid =  (XlsSheetGridModel)table.getGrid();
		xlsgrid.getSheetSource().getWorkbookSource().saveAs(fname);
	}


	/**
	 * @return
	 */
	public IGridTable getUpdatedTable()
	{
		if (canAddRows(1) && canAddCols(1) )
			return new GridTable(region.getTop(), region.getLeft(), region.getBottom(), region.getRight(), table.getGrid());
		
		return new GridTable(region.getTop()-3, region.getLeft()-3, region.getBottom()+3, region.getRight()+3, table.getGrid());
	}
	

	public boolean canAddRows(int nRows)
	{
		GridRegion testRegion = new GridRegion(region.getBottom()+1, region.getLeft()-1, region.getBottom()+2, region.getRight()+1);
		for (int i = 0; i < othertables.length; i++)
		{
			if (IGridRegion.Tool.intersects(testRegion, othertables[i]))
				return false;
		}
		return true;
	}

	public boolean canRemoveRows(int nRows)
	{
		return IGridRegion.Tool.height(region) > nRows;
	}
	
	public boolean canAddCols(int nRows)
	{
		GridRegion testRegion = new GridRegion(region.getTop()-1, region.getRight()+1, region.getBottom()+1, region.getRight()+2);
		for (int i = 0; i < othertables.length; i++)
		{
			if (IGridRegion.Tool.intersects(testRegion, othertables[i]))
				return false;
		}
		return true;
	}

	public boolean canRemoveCols(int nCols)
	{
		return IGridRegion.Tool.width(region) > nCols;
	}
	
	
}
