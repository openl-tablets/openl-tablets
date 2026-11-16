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
    private final int keptFrom;
    private final int keptCount;

    /**
     * @param otherTables the tables beside the one edited, shared by every insert of the edit
     */
    public UndoableInsertColumnsAction(int nCols, int beforeCol, MetaInfoWriter metaInfoWriter,
                                       OtherTables otherTables) {
        this(nCols, beforeCol, 0, 0, metaInfoWriter, otherTables);
    }

    /**
     * Inserts the columns in every row of the table but a block of rows, such as its properties, whose cells stay
     * where they are.
     *
     * @param keptFrom    the first row of the block, counted from the first row of the table
     * @param keptCount   how many rows the block has; 0 for none
     * @param otherTables the tables beside the one edited, shared by every insert of the edit
     */
    public UndoableInsertColumnsAction(int nCols,
                                       int beforeCol,
                                       int keptFrom,
                                       int keptCount,
                                       MetaInfoWriter metaInfoWriter,
                                       OtherTables otherTables) {
        super(metaInfoWriter, nCols, otherTables);
        this.beforeCol = beforeCol;
        this.keptFrom = keptFrom;
        this.keptCount = keptCount;
    }

    /**
     * Whether the table has room for columns laid down on its right: the columns, a blank column after them and a
     * blank row above and below cross none of the other tables.
     */
    public static boolean canInsertColumns(IGridTable table, int nCols, OtherTables otherTables) {
        IGridRegion region = getOriginalRegion(table);
        var newRegion = new GridRegion(region.getTop() - 1,
                region.getRight() + 1,
                region.getBottom() + 1,
                region.getRight() + 1 + nCols);
        return otherTables.isFree(newRegion);
    }

    @Override
    protected boolean canPerformAction(IGridTable table) {
        return UndoableInsertColumnsAction.canInsertColumns(table, lines, otherTables);
    }

    @Override
    protected IUndoableGridTableAction performAction(IGridRegion fullTableRegion, IGridTable table) {
        return new UndoableCompositeAction(GridTool.outsideRows(fullTableRegion, keptFrom, keptCount)
                .stream()
                .map(part -> GridTool.insertColumns(lines, beforeCol, part, table.getGrid(), metaInfoWriter))
                .toList());
    }

    @Override
    protected GridRegionAction getGridRegionAction(IGridRegion gridRegion) {
        return new GridRegionAction(gridRegion, COLUMNS, INSERT, ActionType.EXPAND, lines);
    }

}
