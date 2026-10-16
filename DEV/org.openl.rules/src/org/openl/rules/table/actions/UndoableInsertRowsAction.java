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

    /**
     * @param otherTables the tables beside the one edited, shared by every insert of the edit
     */
    public UndoableInsertRowsAction(int nRows, int beforeRow, MetaInfoWriter metaInfoWriter, OtherTables otherTables) {
        super(metaInfoWriter, nRows, otherTables);
        this.beforeRow = beforeRow;
    }

    /**
     * Whether the table has room for rows laid down under it: the rows, a blank row under them and a blank column on
     * either side cross none of the other tables.
     */
    public static boolean canInsertRows(IGridTable table, int nRows, OtherTables otherTables) {
        IGridRegion region = getOriginalRegion(table);
        var newRegion = new GridRegion(region.getBottom() + 1,
                region.getLeft() - 1,
                region.getBottom() + 1 + nRows,
                region.getRight() + 1);
        return otherTables.isFree(newRegion);
    }

    @Override
    protected boolean canPerformAction(IGridTable table) {
        return UndoableInsertRowsAction.canInsertRows(table, lines, otherTables);
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
