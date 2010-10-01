package org.openl.rules.table;

/**
 * Logical Table may consist of logical columns and rows (created as a result of
 * merged cells).
 * 
 * @author snshor
 */
public interface ILogicalTable extends ITable<ILogicalTable> {

    IGridTable getSource();

    int getColumnWidth(int column);

    int getRowHeight(int row);

    /**
     * Calculates # of the column starting exactly at gridOffset.
     * Throws TableException if gridOffset does not match any column's start.
     *
     * @param gridOffset
     * @return
     */
    int findColumnStart(int gridOffset) throws TableException;

    /**
     * Calculates # of the row starting exactly at gridOffset.
     * Throws TableException if gridOffset does not match any row's start.
     *
     * @param gridOffset
     * @return
     */
    int findRowStart(int gridOffset) throws TableException;

}
