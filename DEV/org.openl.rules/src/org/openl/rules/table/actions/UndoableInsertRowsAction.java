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
    private final boolean oneAfterAnother;

    /**
     * @param otherTables the tables beside the one edited, shared by every insert of the edit
     */
    public UndoableInsertRowsAction(int nRows, int beforeRow, MetaInfoWriter metaInfoWriter, OtherTables otherTables) {
        this(nRows, beforeRow, metaInfoWriter, otherTables, false);
    }

    private UndoableInsertRowsAction(int nRows,
                                     int beforeRow,
                                     MetaInfoWriter metaInfoWriter,
                                     OtherTables otherTables,
                                     boolean oneAfterAnother) {
        super(metaInfoWriter, nRows, otherTables);
        this.beforeRow = beforeRow;
        this.oneAfterAnother = oneAfterAnother;
    }

    /**
     * Rows laid down one after another under a row of the table, each taking the look of the row above it, as that
     * many single-row inserts would lay them. The rows under them move down once for the whole block.
     *
     * @param nRows       how many rows are laid down
     * @param afterRow    the row the first new row goes under, counted from the first row of the table
     * @param otherTables the tables beside the one edited, shared by every insert of the edit
     * @see GridTool#insertRowsOneAfterAnother
     */
    public static UndoableInsertRowsAction oneAfterAnother(int nRows,
                                                           int afterRow,
                                                           MetaInfoWriter metaInfoWriter,
                                                           OtherTables otherTables) {
        return new UndoableInsertRowsAction(nRows, afterRow, metaInfoWriter, otherTables, true);
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
        return oneAfterAnother
                ? GridTool.insertRowsOneAfterAnother(lines, beforeRow, fullTableRegion, table.getGrid(), metaInfoWriter)
                : GridTool.insertRows(lines, beforeRow, fullTableRegion, table.getGrid(), metaInfoWriter);
    }
}
