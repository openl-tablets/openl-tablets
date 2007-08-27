/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.xls;

import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.openl.rules.table.IUndoGrid;
import org.openl.rules.table.IWritableGrid;

/**
 * @author snshor
 *
 */
public class XlsUndoGrid implements IUndoGrid
{
	
	HSSFWorkbook wb;
	HSSFSheet sheet;
	XlsSheetGridModel grid;
	
	public XlsUndoGrid()
	{	
		wb = new HSSFWorkbook();
		wb.createSheet();
		sheet = wb.getSheetAt(0);
		grid = new XlsSheetGridModel(sheet);
	}

	
	static final int CELLS_IN_A_ROW = 250;
	
	int cnt;
	
	
	
	
	public synchronized int saveCell(HSSFCell cell, Map map)
	{
		++cnt;
		int colTo = getColumn(cnt);
		int rowTo = getRow(cnt);
		grid.copyFrom(cell, colTo, rowTo, map);
		return cnt;
	}

	public HSSFCell restoreCell(int id)
	{
		int col = getColumn(id);
		int row = getRow(id);
		return grid.getCell(col, row);
	}

	public Map restoreMap(int id)
	{
		int col = getColumn(id);
		int row = getRow(id);
		return grid.getCellMetaInfoMap(col, row);
	}

	
	private int getColumn(int cnt)
	{
		return cnt/CELLS_IN_A_ROW;
	}
	
	
	private int getRow(int cnt)
	{
		return cnt%CELLS_IN_A_ROW;
	}

	public int saveCell(IWritableGrid fromGrid, int col, int row)
	{
		return saveCell(((XlsSheetGridModel)fromGrid).getCell(col, row), fromGrid.getCellMetaInfoMap(col, row));
	}

	public void restoreCell(int cellID, IWritableGrid toGrid, int col, int row)
	{
		((XlsSheetGridModel)toGrid).copyFrom(restoreCell(cellID), col, row, restoreMap(cellID));
	}
	
	
}
