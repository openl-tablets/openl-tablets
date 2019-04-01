package org.openl.rules.table;

/**
 * Logical Table consists of logical columns and rows (created as a result of merged cells). Each merged region is taken
 * as one cell.<br>
 * To make {@link ILogicalTable} from your source {@link IGridTable} use
 * {@link LogicalTableHelper#logicalTable(IGridTable)}.
 *
 *
 * @author snshor
 */
public interface ILogicalTable extends ITable<ILogicalTable> {

    /**
     *
     * @return underlying {@link IGridTable}
     */
    IGridTable getSource();

    /**
     *
     * @param column index of the column
     * @return width of the column by its index.
     */
    int getColumnWidth(int column);

    /**
     *
     * @param row index of the row
     * @return height of the row by its index.
     */
    int getRowHeight(int row);

    /**
     * Calculates # of the column starting exactly at gridOffset. Throws TableException if gridOffset does not match any
     * column's start.
     *
     * @param gridOffset
     * @return
     */
    int findColumnStart(int gridOffset);

    /**
     * Calculates # of the row starting exactly at gridOffset. Throws TableException if gridOffset does not match any
     * row's start.
     *
     * @param gridOffset
     * @return
     */
    int findRowStart(int gridOffset);

}
