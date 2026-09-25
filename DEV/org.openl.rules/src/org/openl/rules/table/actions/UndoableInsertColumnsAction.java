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

    private final int beforeCol;

    public UndoableInsertColumnsAction(int nCols, int beforeCol, MetaInfoWriter metaInfoWriter) {
        super(metaInfoWriter, nCols);
        this.beforeCol = beforeCol;
    }

    public static boolean canInsertColumns(IGridTable table, int nCols) {
        IGridRegion region = getOriginalRegion(table);
        var newRegion = new GridRegion(region.getTop() - 1,
                region.getRight() + 1,
                region.getBottom() + 1,
                region.getRight() + 1 + nCols);
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
        return UndoableInsertColumnsAction.canInsertColumns(table, lines);
    }

    @Override
    protected IUndoableGridTableAction performAction(IGridRegion fullTableRegion, IGridTable table) {
        return GridTool.insertColumns(lines, beforeCol, fullTableRegion, table.getGrid(), metaInfoWriter);
    }

    @Override
    protected GridRegionAction getGridRegionAction(IGridRegion gridRegion) {
        return new GridRegionAction(gridRegion, COLUMNS, INSERT, ActionType.EXPAND, lines);
    }

}
