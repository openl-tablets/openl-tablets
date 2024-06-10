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

    final int nRows;
    final int startRow;
    private final MetaInfoWriter metaInfoWriter;

    public UndoableRemoveRowsAction(int nRows, int startRow, MetaInfoWriter metaInfoWriter) {
        this.nRows = nRows;
        this.startRow = startRow;
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
        return nRows;
    }

    @Override
    protected IUndoableGridTableAction performAction(int numberToRemove,
                                                     IGridRegion fullTableRegion,
                                                     IGridTable table) {
        return GridTool.removeRows(numberToRemove, startRow, fullTableRegion, table.getGrid(), metaInfoWriter);
    }

}
