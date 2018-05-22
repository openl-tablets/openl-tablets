package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTool;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.actions.GridRegionAction.ActionType;

/**
 * @author Andrei Astrouski
 */
public class UndoableInsertColumnsAction extends UndoableInsertAction {

    private int nCols;
    private int beforeCol;
    private int row;

    public UndoableInsertColumnsAction(int nCols, int beforeCol, int row, MetaInfoWriter metaInfoWriter) {
        super(metaInfoWriter);
        this.nCols = nCols;
        this.beforeCol = beforeCol;
        this.row = row;
    }

    public static boolean canInsertColumns(IGridTable table, int nCols) {
        IGridRegion region = getOriginalRegion(table);
        GridRegion newRegion = new GridRegion(region.getTop() - 1, region.getRight() + 1,
                region.getBottom() + 1, region.getRight() + 1 + nCols);
        IGridTable[] allGridTables = table.getGrid().getTables();
        for (IGridTable allGridTable : allGridTables) {
            if (!table.getUri().equals(allGridTable.getUri())
                    && IGridRegion.Tool.intersects(newRegion, allGridTable.getRegion())) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    protected boolean canPerformAction(IGridTable table) {
        return UndoableInsertColumnsAction.canInsertColumns(table, getNumberToInsert(table));
    }
    
    @Override
    protected int getNumberToInsert(IGridTable table) {
        int cellWidth = getOriginalTable(table).getCell(beforeCol, row).getWidth();
        int colToInsert = nCols;
        if (cellWidth > 1) { // merged cell
            colToInsert += cellWidth - 1;
        }
        return colToInsert;
    }
    
    @Override
    protected IUndoableGridTableAction performAction(int numberToInsert, IGridRegion fullTableRegion, IGridTable table) {
        return GridTool.insertColumns(numberToInsert, beforeCol, fullTableRegion, table.getGrid(), metaInfoWriter);
    }
    
    @Override
    protected GridRegionAction getGridRegionAction(IGridRegion gridRegion, int numberToInsert) {
        return new GridRegionAction(gridRegion, COLUMNS, INSERT, ActionType.EXPAND, numberToInsert);
    }
    
}
