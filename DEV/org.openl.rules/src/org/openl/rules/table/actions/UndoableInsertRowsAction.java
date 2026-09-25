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
public class UndoableInsertRowsAction extends UndoableInsertAction {

    private final int beforeRow;

    public UndoableInsertRowsAction(int nRows, int beforeRow, MetaInfoWriter metaInfoWriter) {
        super(metaInfoWriter, nRows);
        this.beforeRow = beforeRow;
    }

    public static boolean canInsertRows(IGridTable table, int nRows) {
        IGridRegion region = getOriginalRegion(table);
        var newRegion = new GridRegion(region.getBottom() + 1,
                region.getLeft() - 1,
                region.getBottom() + 1 + nRows,
                region.getRight() + 1);
        var allGridTables = table.getGrid().getTables();
        for (IGridTable allGridTable : allGridTables) {
            if (!table.getUri().equals(allGridTable.getUri()) && IGridRegion.Tool.intersects(newRegion,
                    allGridTable.getRegion())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean canPerformAction(IGridTable table) {
        return UndoableInsertRowsAction.canInsertRows(table, lines);
    }

    @Override
    protected GridRegionAction getGridRegionAction(IGridRegion gridRegion) {
        return new GridRegionAction(gridRegion, ROWS, INSERT, ActionType.EXPAND, lines);
    }

    @Override
    protected IUndoableGridTableAction performAction(IGridRegion fullTableRegion, IGridTable table) {
        return GridTool.insertRows(lines, beforeRow, fullTableRegion, table.getGrid(), metaInfoWriter);
    }
}
