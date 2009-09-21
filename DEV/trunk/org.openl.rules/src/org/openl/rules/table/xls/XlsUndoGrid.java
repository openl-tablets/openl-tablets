/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.xls;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.IUndoGrid;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.ICellStyle;

/**
 * @author snshor
 *
 */
public class XlsUndoGrid implements IUndoGrid {

    static final int CELLS_IN_A_ROW = 250;
    Workbook wb;
    Sheet sheet;

    XlsSheetGridModel grid;

    int cnt;

	public XlsUndoGrid(XlsSheetGridModel originalGrid) {
		Workbook originalWorkbook = originalGrid.getSheetSource().getWorkbookSource().getWorkbook();
		
		if (originalWorkbook instanceof XSSFWorkbook) {
			wb = new XSSFWorkbook();
		} else {
			wb = new HSSFWorkbook();
		}
		wb.createSheet();
		sheet = wb.getSheetAt(0);
		grid = new XlsSheetGridModel(sheet);
	}

    private int getColumn(int cid) {
        return cid / CELLS_IN_A_ROW;
    }

    private int getRow(int cid) {
        return cid % CELLS_IN_A_ROW;
    }

	public Cell restoreCell(int id) {
        int col = getColumn(id);
        int row = getRow(id);
        return grid.getXlsCell(col, row);
    }

    public void restoreCell(int cellID, IWritableGrid toGrid, int col, int row) {
        ((XlsSheetGridModel) toGrid).copyFrom(restoreCell(cellID), col, row, restoreMeta(cellID));
        toGrid.setCellStyle(col, row, restoreModifiedStyle(cellID));
    }

    public CellMetaInfo restoreMeta(int id) {
        int col = getColumn(id);
        int row = getRow(id);
        return grid.getCellMetaInfo(col, row);
    }

    private ICellStyle restoreModifiedStyle(int id) {
        int col = getColumn(id);
        int row = getRow(id);
        return grid.getModifiedStyle(col, row);
    }

	public synchronized int saveCell(Cell cell, CellMetaInfo meta, ICellStyle modifiedStyle) {
        ++cnt;
        int colTo = getColumn(cnt);
        int rowTo = getRow(cnt);
        grid.copyFrom(cell, colTo, rowTo, meta);
        grid.setCellStyle(colTo, rowTo, modifiedStyle);
        return cnt;
    }

    public int saveCell(IWritableGrid fromGrid, int col, int row) {
        return saveCell(((XlsSheetGridModel) fromGrid).getXlsCell(col, row), fromGrid.getCellMetaInfo(col, row),
                ((XlsSheetGridModel) fromGrid).getModifiedStyle(col, row));
    }

}
