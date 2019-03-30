package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridTool;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.actions.GridRegionAction.ActionType;

/**
 * @author Andrei Astrouski
 */
public class UndoableRemoveRowsAction extends UndoableRemoveAction {

    private int nRows;
    private int startRow;
    private int col;
    private MetaInfoWriter metaInfoWriter;

    public UndoableRemoveRowsAction(int nRows, int startRow, int col, MetaInfoWriter metaInfoWriter) {
        this.nRows = nRows;
        this.startRow = startRow;
        this.col = col;
        this.metaInfoWriter = metaInfoWriter;
    }

    @Override
    protected boolean canPerformAction(IGridRegion gridRegion) {
        return !(startRow < 0 || startRow >= IGridRegion.Tool.height(gridRegion));
    }

    @Override
    protected GridRegionAction getGridRegionAction(IGridRegion gridRegion, int numberToRemove) {
        return new GridRegionAction(gridRegion, ROWS, REMOVE, ActionType.EXPAND, numberToRemove);
    }

    @Override
    protected int getNumberToRemove(IGridTable table) {
        int cellHeight = getOriginalTable(table).getCell(col, startRow).getHeight();
        int numberToRemove = nRows;
        if (cellHeight > 1) { // merged cell
            numberToRemove += cellHeight - 1;
        }
        return numberToRemove;
    }

    @Override
    protected IUndoableGridTableAction performAction(int numberToRemove,
            IGridRegion fullTableRegion,
            IGridTable table) {
        return GridTool.removeRows(numberToRemove, startRow, fullTableRegion, table.getGrid(), metaInfoWriter);
    }

}
