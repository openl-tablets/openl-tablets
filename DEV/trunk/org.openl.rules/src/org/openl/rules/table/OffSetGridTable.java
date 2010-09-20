/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 *
 * TODO: Write javadocs!!!!
 * 
 */
public class OffSetGridTable extends AGridTableDelegator {
    
    private int[] rowOffset;

    private int[] columnOffset;

    public OffSetGridTable(IGridTable gridTable, int width, int height) {
        super(gridTable);
        calculateRowOffsets(height);
        calculateColumnOffsets(width);
    }

    public OffSetGridTable(IGridTable gridTable, int[] columnOffset, int[] rowOffset) {
        super(gridTable);
        
        if (columnOffset == null) {
            int width = OffSetGridTableHelper.calcLogicalColumns(getGridTable());
            calculateColumnOffsets(width);
        }    
        else this.columnOffset = columnOffset;
        
        if (rowOffset == null) {
            int height = OffSetGridTableHelper.calcLogicalRows(getGridTable());
            calculateRowOffsets(height);
        }    
        else this.rowOffset = rowOffset;
    }

    private void calculateRowOffsets(int height) {
        rowOffset = new int[height + 1];
        int cellHeight = 0, offset = 0;
        int i = 0;
        for (; i < rowOffset.length - 1; offset += cellHeight, ++i) {
            rowOffset[i] = offset;
            cellHeight = getGridTable().getCell(0, offset).getHeight();
        }
        rowOffset[i] = offset;

    }
    
    @Override
    public IGridTable getGridTable() {        
        return getOriginalGridTable();
    }
    private void calculateColumnOffsets(int width) {
        columnOffset = new int[width+1];
        int cellWidth = 0;
        int offset = 0;
        int i = 0;
        for (; i < columnOffset.length - 1; offset += cellWidth, ++i) {
            columnOffset[i] = offset;
            cellWidth = getGridTable().getCell(offset, 0).getWidth();
        }
        columnOffset[i] = offset;
    }

    @Override
    protected IGridTable columnsInternal(int from, int to) {
        return OffSetGridTableHelper.offSetTable(getGridTable().columns(columnOffset[from], columnOffset[to + 1] - 1));
    }

    public int findColumnStart(int gridOffset) throws TableException {
        for (int i = 0; i < columnOffset.length - 1; i++) {
            if (columnOffset[i] == gridOffset) {
                return i;
            }
            if (columnOffset[i] > gridOffset) {
                throw new TableException("gridOffset does not match column start");
            }
        }
        throw new TableException("gridOffset is higher than table's width");
    }

    public int findRowStart(int gridOffset) throws TableException {
        for (int i = 0; i < rowOffset.length - 1; i++) {
            if (rowOffset[i] == gridOffset) {
                return i;
            }
            if (rowOffset[i] > gridOffset) {
                throw new TableException("gridOffset does not match row start");
            }
        }
        throw new TableException("gridOffset is higher than table's height");
    }

    public IGridTable getColumn(int column) {
        return getRegion(column, 0, 1, getGridHeight());
    }

    public int getColumnGridWidth(int column) {

        return columnOffset[column + 1] - columnOffset[column];
    }

   @Override
    public IGridTable getRegionInternal(int column, int row, int width, int height) {

        int startRow = rowOffset[row];
        int endRow = rowOffset[row + height];
        int startColumn = columnOffset[column];
        int endColumn = columnOffset[column + width];

        return OffSetGridTableHelper.offSetTable(getGridTable().getRegion(startColumn, startRow, endColumn - startColumn,
                endRow - startRow));
    }

    public IGridTable getRow(int row) {
        return getRegion(0, row, getGridWidth(), 1);
    }

    public int getRowGridHeight(int row) {
        return rowOffset[row + 1] - rowOffset[row];
    }

     @Override
    protected IGridTable rowsInternal(int from, int to) {
        return OffSetGridTableHelper.offSetTable(getGridTable().rows(rowOffset[from], rowOffset[to + 1] - 1));
    }

    public IGridTable transpose() {
        return OffSetGridTableHelper.offSetTable(new TransposedGridTable(getGridTable()));
    }

    public int[] getRowOffset() {
        return rowOffset;
    }
    
    public int[] getColumnOffset() {
        return columnOffset;
    }
    
    @Deprecated
    public IGridTable getLogicalCell(int column, int row) {
        return getColumn(column).getRow(row);
    }
    
    @Override
    public String toString() {
        StringBuffer tableVisualization = new StringBuffer();     
        tableVisualization.append(super.toString()).append("\n");
        
        for (int i = 0; i < getGridHeight(); i++) {
            int length = 0;
            for (int j = 0; j < getGridWidth(); j++) {
                String stringValue = getGridTable().getCell(j, i).getStringValue();
                if (stringValue == null) {
                    stringValue = "EMPTY";
                }
                length += stringValue.length();
                tableVisualization.append(stringValue);                
                tableVisualization.append("|");
            }
            tableVisualization.append("\n");
            for(int k = 0; k <= length; k++) {
                tableVisualization.append("-");
            }   
            tableVisualization.append("\n");
        }
        
        return  tableVisualization.toString();
    }

    public int getGridColumn(int column, int row) {
        // TODO Auto-generated method stub
        // implement using offSet operations
        return 0;
    }

    public int getGridHeight() {
        return rowOffset.length - 1;
    }

    public int getGridRow(int column, int row) {
        // TODO Auto-generated method stub
        // implement using offSet operations
        return 0;
    }

    public int getGridWidth() {
        return columnOffset.length - 1;
    }

    public boolean isNormalOrientation() {        
        return getGridTable().isNormalOrientation();
    }

}
