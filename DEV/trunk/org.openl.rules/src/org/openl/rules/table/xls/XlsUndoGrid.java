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

/**
 * @author snshor
 *
 */
public class XlsUndoGrid implements IUndoGrid {

    private static final int CELLS_IN_A_ROW = 250;
    private Workbook wb;
    private Sheet sheet;

    private XlsSheetGridModel grid;

    private int cnt;

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

	public Cell getCellToRestore(int id) {
        int col = getColumn(id);
        int row = getRow(id);
        return PoiHelper.getPoiXlsCell(col, row, grid.getSheetSource().getSheet());
    }

    public void restoreCell(int cellID, IWritableGrid toGrid, int col, int row) {
        ((XlsSheetGridModel) toGrid).copyCell(getCellToRestore(cellID), col, row, getCellMetaInfoToRestore(cellID));
    }

    public CellMetaInfo getCellMetaInfoToRestore(int id) {
        int col = getColumn(id);
        int row = getRow(id);
        return grid.getCell(col, row).getMetaInfo();
    }

	public synchronized int saveCell(Cell cell, CellMetaInfo meta) {
        ++cnt;
        int colTo = getColumn(cnt);
        int rowTo = getRow(cnt);
        grid.copyCell(cell, colTo, rowTo, meta);
        return cnt;
    }

    public int saveCell(IWritableGrid fromGrid, int col, int row) {
        return saveCell(PoiHelper.getPoiXlsCell(col, row, ((XlsSheetGridModel) fromGrid).getSheetSource().getSheet()),
                fromGrid.getCell(col, row).getMetaInfo());
    }

}
