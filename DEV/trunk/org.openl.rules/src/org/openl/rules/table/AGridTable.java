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

    protected IGridTable columnsInternal(int from, int to) {
        return new GridTableColumns(this, from, to);
    }

    public int findColumnStart(int gridOffset) throws TableException {
        if (gridOffset < getGridWidth()) {
            return gridOffset;
        }
        throw new TableException("gridOffset is higher than table's width");
    }

    public int findRowStart(int gridOffset) throws TableException {
        if (gridOffset < getGridHeight()) {
            return gridOffset;
        }
        throw new TableException("gridOffset is higher than table's height");
    }
    
    public IGridTable getGridTable() {
        return this;
    }

    public IGridTable getColumn(int column) {
        return columns(column, column);
    }

    public int getColumnGridWidth(int column) {
        return 1;
    }

    protected IGridTable getRegionInternal(int column, int row, int width, int height) {
        return new GridTableRegion(this, column, row, width, height);
    }

    public IGridTable getRow(int row) {
        return rows(row, row);
    }

    public int getRowGridHeight(int row) {
        return 1;
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

    protected IGridTable rowsInternal(int from, int to) {
        return new GridTableRows(this, from, to);
    }

    public IGridTable transpose() {
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
        for (int i = 0; i < getGridHeight(); i++) {
            int length = 0;
            for (int j = 0; j < getGridWidth(); j++) {
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
    
    public IGridTable columns(int from) {
        return columns(from, getGridWidth() - 1);
    }

    public IGridTable columns(int from, int to) {
        if (getGridWidth() == to - from + 1) {
            return this;
        }

        return columnsInternal(from, to);
    }    

    public IGridTable getRegion(int column, int row, int width, int height) {
        if (column == 0 && width == getGridWidth()) {
            return rows(row, row + height - 1);
        }

        if (row == 0 && height == getGridHeight()) {
            return columns(column, column + width - 1);
        }

        return getRegionInternal(column, row, width, height);
    }
    
    public IGridTable rows(int from) {
        return rows(from, getGridHeight() - 1);
    }

    public IGridTable rows(int from, int to) {
        if (getGridHeight() == to - from + 1) {
            return this;
        }

        return rowsInternal(from, to);
    }

}
