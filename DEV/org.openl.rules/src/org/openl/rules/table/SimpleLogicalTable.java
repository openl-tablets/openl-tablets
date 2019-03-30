package org.openl.rules.table;

/**
 * Adapts {@link IGridTable}, that doesn`t contain merged cells in the top row and left column to {@link ILogicalTable} 
 * interface.<br>
 * <b>Be careful, as this implementation doesn`t provide that following operations<br></b>
 * {@link ITable#getColumn(int)}<br>
 * {@link ITable#getColumns(int)}<br>
 * {@link ITable#getColumns(int, int)}<br>
 * {@link ITable#getRow(int)}<br>
 * {@link ITable#getRows(int)}<br>
 * {@link ITable#getRows(int, int)}<br>
 * {@link ITable#getSubtable(int, int, int, int)}<br> will return right ILogicalTable. That is because all of them 
 * are based on {@link SimpleLogicalTable#getSubtable(int, int, int, int)} method implementation.
 *  
 * @author Andrei Astrouski
 * @author DLiauchuk
 */
public class SimpleLogicalTable extends ALogicalTable {

    public SimpleLogicalTable(IGridTable table) {
        super(table);
    }

    @Override
    public int getWidth() {
        return getSource().getWidth();
    }

    @Override
    public int getHeight() {
        return getSource().getHeight();
    }

    @Override
    public int findColumnStart(int gridOffset) {
        return gridOffset;
    }

    @Override
    public int findRowStart(int gridOffset) {
        return gridOffset;
    }

    @Override
    public int getColumnWidth(int column) {
        return 1;
    }

    @Override
    public int getRowHeight(int row) {
        return 1;
    }
    
    /**
     * This method consider that this table doesn`t have merged regions inside itself.
     * In general it is not true. Current Openl parsing implementation, correctly handles
     * such cases. If you need the trully ILogicalTable, with correctly calculated logical columns and
     * rows, you may get the source table, by calling {@link ILogicalTable#getSource()} extract the region you need
     * by calling {@link ITable#getSubtable(int, int, int, int)} and create {@link ILogicalTable} by calling
     * {@link LogicalTableHelper#logicalTable(IGridTable)}. This is optimised implementation because of
     * time overhead when checking for each table is it really logical or not.<br>
     * For test see {@link SimpleLogicalTableTest}. 
     * 
     * @param column from which we want to take the subtable, including this.
     * @param row from which we want to take the subtable, including this.
     * @param width of the needed table. 
     * @param height of the needed table.
     * @return {@link SimpleLogicalTable} 
     */
    @Override
    public ILogicalTable getSubtable(int column, int row, int width, int height) {
        if (width == 0 || height == 0) {
            return null;
        }
        // the right implementation for this method must be following:
        // return LogicalTableHelper.logicalTable(table.getSubtable(column, row, width, height));
        // but we loose lots of time on this callings.
        return new SimpleLogicalTable(getSource().getSubtable(column, row, width, height));
    }

    @Override
    public ICell getCell(int column, int row) {
        return super.getSource().getCell(column, row);
    }
}
