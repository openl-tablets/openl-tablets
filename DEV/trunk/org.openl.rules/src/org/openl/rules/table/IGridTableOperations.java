package org.openl.rules.table;

public interface IGridTableOperations {
    IGridTable columns(int from);

    IGridTable columns(int from, int to);

    /**
     * Calculates # of the column starting exactly at gridOffset. Throws
     * TableException if gridOffset does not match any column's start
     *
     * @param gridOffset
     * @return
     */
    int findColumnStart(int gridOffset) throws TableException;

    /**
     * Calculates # of the row starting exactly at gridOffset. Throws
     * TableException if gridOffset does not match any row's start
     *
     * @param gridOffset
     * @return
     */
    int findRowStart(int gridOffset) throws TableException;    

    int getColumnGridWidth(int column);    

    int getRowGridHeight(int row);

    IGridTable rows(int from);

    IGridTable rows(int from, int to);
    
    IGridTable getRegion(int column, int row, int width, int height);
    
    IGridTable getRow(int row);
    
    IGridTable getColumn(int column);

    IGridTable transpose();
}
