/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public abstract class AGridTable implements IGridTable {

    protected ILogicalTable columnsInternal(int from, int to) {
        return new GridTableColumns(this, from, to);
    }

    public int findColumnStart(int gridOffset) throws TableException {
        if (gridOffset < getLogicalWidth()) {
            return gridOffset;
        }
        throw new TableException("gridOffset is higher than table's width");
    }

    public int findRowStart(int gridOffset) throws TableException {
        if (gridOffset < getLogicalHeight()) {
            return gridOffset;
        }
        throw new TableException("gridOffset is higher than table's height");
    }
    
    public IGridTable getGridTable() {
        return this;
    }

    public ILogicalTable getLogicalColumn(int column) {
        return columns(column, column);
    }

    public int getLogicalColumnGridWidth(int column) {
        return 1;
    }

    public int getLogicalHeight() {
        return getGridHeight();
    }

    protected ILogicalTable getLogicalRegionInternal(int column, int row, int width, int height) {
        return new GridTableRegion(this, column, row, width, height);
    }

    public ILogicalTable getLogicalRow(int row) {
        return rows(row, row);
    }

    public int getLogicalRowGridHeight(int row) {
        return 1;
    }

    public int getLogicalWidth() {
        return getGridWidth();
    }    

    public IGridRegion getRegion() {
        int left = getGridColumn(0, 0);
        int top = getGridRow(0, 0);

        int right = -1;
        int bottom = -1;

        if (isNormalOrientation()) {
            right = getGridColumn(getGridWidth() - 1, 0);
            bottom = getGridRow(0, getGridHeight() - 1);
        } else {
            right = getGridColumn(0, getGridHeight() - 1);
            bottom = getGridRow(getGridWidth() - 1, 0);
        }

        return new GridRegion(top, left, bottom, right);
    }

    public String getUri() {
        int w = getGridWidth();
        int h = getGridHeight();
        return getGrid().getRangeUri(getGridColumn(0, 0), getGridRow(0, 0), getGridColumn(w - 1, h - 1),
                getGridRow(w - 1, h - 1));
    }

    public String getUri(int col, int row) {
        int colStart = getGridColumn(col, row);
        int rowStart = getGridRow(col, row);
        return getGrid().getRangeUri(colStart, rowStart, colStart, rowStart);
    }

    public boolean isPartOfTheMergedRegion(int column, int row) {
        return getGrid().isPartOfTheMergedRegion(getGridColumn(column, row), getGridRow(column, row));
    }

    protected ILogicalTable rowsInternal(int from, int to) {
        return new GridTableRows(this, from, to);
    }

    public ILogicalTable transpose() {
        return new TransposedGridTable(this);
    }

    public ICell getCell(int column, int row) {
    	return new GridTableCell(column, row, this);
    }

    @Override
    public String toString() {
        StringBuffer tableVizualization = new StringBuffer();
        tableVizualization.append(super.toString() + (isNormalOrientation() ? "N" : "T")
                +  getRegion().toString() +"\n");
        for (int i = 0; i < getLogicalHeight(); i++) {
            int length = 0;
            for (int j = 0; j < getLogicalWidth(); j++) {
                String strValue = getCell(j, i).getStringValue();
                if (strValue == null) {
                    strValue = "EMPTY";
                }
                length += strValue.length();
                tableVizualization.append(strValue);                
                tableVizualization.append("|");
            }
            tableVizualization.append("\n");
            for(int k = 0; k <= length; k++) {
                tableVizualization.append("-");
            }   
            tableVizualization.append("\n");
        }
        
        return  tableVizualization.toString();
    }
    
    public ILogicalTable columns(int from) {
        return columns(from, getLogicalWidth() - 1);
    }

    public ILogicalTable columns(int from, int to) {
        if (getLogicalWidth() == to - from + 1) {
            return this;
        }

        return columnsInternal(from, to);
    }    

    public ILogicalTable getLogicalRegion(int column, int row, int width, int height) {
        if (column == 0 && width == getLogicalWidth()) {
            return rows(row, row + height - 1);
        }

        if (row == 0 && height == getLogicalHeight()) {
            return columns(column, column + width - 1);
        }

        return getLogicalRegionInternal(column, row, width, height);
    }
    
    public ILogicalTable rows(int from) {
        return rows(from, getLogicalHeight() - 1);
    }

    public ILogicalTable rows(int from, int to) {
        if (getLogicalHeight() == to - from + 1) {
            return this;
        }

        return rowsInternal(from, to);
    }

}
