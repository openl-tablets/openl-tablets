package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridTool;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.actions.GridRegionAction.ActionType;

/**
 * @author Andrei Astrouski
 */
public class UndoableRemoveColumnsAction extends UndoableRemoveAction {

    final int nCols;
    final int startCol;
    private final MetaInfoWriter metaInfoWriter;

    public UndoableRemoveColumnsAction(int nCols, int startCol, MetaInfoWriter metaInfoWriter) {
        this.nCols = nCols;
        this.startCol = startCol;
        this.metaInfoWriter = metaInfoWriter;
    }

    @Override
    protected boolean canPerformAction(IGridRegion gridRegion) {
        return !(startCol < 0 || startCol >= IGridRegion.Tool.width(gridRegion));
    }

    @Override
    protected int getNumberToRemove(IGridTable table) {
        return nCols;
    }

    @Override
    protected IUndoableGridTableAction performAction(int numberToRemove,
                                                     IGridRegion fullTableRegion,
                                                     IGridTable table) {
        return GridTool.removeColumns(numberToRemove, startCol, fullTableRegion, table.getGrid(), metaInfoWriter);
    }

    @Override
    protected GridRegionAction getGridRegionAction(IGridRegion gridRegion, int numberToRemove) {
        return new GridRegionAction(gridRegion, COLUMNS, REMOVE, ActionType.EXPAND, numberToRemove);
    }

}
