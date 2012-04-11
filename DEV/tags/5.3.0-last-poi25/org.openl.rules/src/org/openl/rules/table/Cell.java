package org.openl.rules.table;

import org.openl.rules.table.ui.ICellStyle;

public class Cell implements ICell {
	
	private int col;
	private int row;
	private IGridTable gridTable;
	
	
	public Cell(int col, int row, IGridTable gridTable) {
		this.col = col;
		this.row = row;
		this.gridTable = gridTable;
	}

	public int getCellHeight() {
		return gridTable.isNormalOrientation() ? gridTable.getGrid().getCellHeight(gridTable.getGridColumn(col, row), gridTable.getGridRow(col, row))
                : gridTable.getGrid().getCellWidth(gridTable.getGridColumn(col, row), gridTable.getGridRow(col, row));
	}

	public ICellInfo getCellInfo() {
		return gridTable.isNormalOrientation() ? gridTable.getGrid().getCellInfo(gridTable.getGridColumn(col, row), gridTable.getGridRow(col, row))
                : gridTable.getGrid().getCellInfo(gridTable.getGridColumn(col, row), gridTable.getGridRow(col, row));		
	}

	public ICellStyle getCellStyle() {
		return gridTable.getGrid().getCellStyle(gridTable.getGridColumn(col, row), gridTable.getGridRow(col, row));
	}

	public int getCellWidth() {
		return gridTable.isNormalOrientation() ? gridTable.getGrid().getCellWidth(gridTable.getGridColumn(col, row), gridTable.getGridRow(col, row))
                : gridTable.getGrid().getCellHeight(gridTable.getGridColumn(col, row), gridTable.getGridRow(col, row));
	}
	
	public Object getObjectValue() {
		return gridTable.getGrid().getObjectCellValue(gridTable.getGridColumn(col, row), gridTable.getGridRow(col, row));
	}

	public String getStringValue() {
		return gridTable.getGrid().getStringCellValue(gridTable.getGridColumn(col, row), gridTable.getGridRow(col, row));
	}

}
