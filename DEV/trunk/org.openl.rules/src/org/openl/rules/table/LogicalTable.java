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
public class LogicalTable extends ALogicalTable {
    
    private IGridTable gridTable;
    
    private int[] rowOffset;

    private int[] columnOffset;

    public LogicalTable(IGridTable gridTable, int width, int height) {
        this.gridTable = gridTable;
        calculateRowOffsets(height);
        calculateColumnOffsets(width);
    }

    public LogicalTable(IGridTable gridTable, int[] columnOffset, int[] rowOffset) {
        this.gridTable = gridTable;
        
        if (columnOffset == null) {
            int width = LogicalTableHelper.calcLogicalColumns(gridTable);
            calculateColumnOffsets(width);
        }    
        else this.columnOffset = columnOffset;
        
        if (rowOffset == null) {
            int height = LogicalTableHelper.calcLogicalRows(gridTable);
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
            cellHeight = gridTable.getCell(0, offset).getHeight();
        }
        rowOffset[i] = offset;

    }
    
    private void calculateColumnOffsets(int width) {
        columnOffset = new int[width+1];
        int cellWidth = 0;
        int offset = 0;
        int i = 0;
        for (; i < columnOffset.length - 1; offset += cellWidth, ++i) {
            columnOffset[i] = offset;
            cellWidth = gridTable.getCell(offset, 0).getWidth();
        }
        columnOffset[i] = offset;
    }

    @Override
    protected ILogicalTable columnsInternal(int from, int to) {
        return LogicalTableHelper.logicalTable(gridTable.columns(columnOffset[from], columnOffset[to + 1] - 1));
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

    public IGridTable getGridTable() {
        return gridTable;
    }

    public ILogicalTable getLogicalColumn(int column) {
        return getLogicalRegion(column, 0, 1, getLogicalHeight());
    }

    public int getLogicalColumnGridWidth(int column) {

        return columnOffset[column + 1] - columnOffset[column];
    }

    public int getLogicalHeight() {
        return rowOffset.length - 1;
    }

   @Override
    public ILogicalTable getLogicalRegionInternal(int column, int row, int width, int height) {

        int startRow = rowOffset[row];
        int endRow = rowOffset[row + height];
        int startColumn = columnOffset[column];
        int endColumn = columnOffset[column + width];

        return LogicalTableHelper.logicalTable(gridTable.getLogicalRegion(startColumn, startRow, endColumn - startColumn,
                endRow - startRow));
    }

    public ILogicalTable getLogicalRow(int row) {
        return getLogicalRegion(0, row, getLogicalWidth(), 1);
    }

    public int getLogicalRowGridHeight(int row) {
        return rowOffset[row + 1] - rowOffset[row];
    }

    public int getLogicalWidth() {
        return columnOffset.length - 1;
    }

     @Override
    protected ILogicalTable rowsInternal(int from, int to) {
        return LogicalTableHelper.logicalTable(gridTable.rows(rowOffset[from], rowOffset[to + 1] - 1));
    }

    public ILogicalTable transpose() {
        return LogicalTableHelper.logicalTable(new TransposedGridTable(gridTable));
    }

    public int[] getRowOffset() {
        return rowOffset;
    }
    
    public int[] getColumnOffset() {
        return columnOffset;
    }
    
    public ILogicalTable getLogicalCell(int column, int row) {
        return getLogicalColumn(column).getLogicalRow(row);
    }
    
    @Override
    public String toString() {
        StringBuffer tableVisualization = new StringBuffer();     
        tableVisualization.append(super.toString()).append("\n");
        
        for (int i = 0; i < getLogicalHeight(); i++) {
            int length = 0;
            for (int j = 0; j < getLogicalWidth(); j++) {
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

}
