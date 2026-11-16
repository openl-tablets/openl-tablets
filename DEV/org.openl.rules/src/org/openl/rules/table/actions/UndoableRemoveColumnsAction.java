package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegionUtils;
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
    private final int keptFrom;
    private final int keptCount;
    private final MetaInfoWriter metaInfoWriter;

    public UndoableRemoveColumnsAction(int nCols, int startCol, MetaInfoWriter metaInfoWriter) {
        this(nCols, startCol, 0, 0, metaInfoWriter);
    }

    /**
     * Removes the columns from every row of the table but a block of rows, such as its properties, whose cells stay
     * where they are.
     *
     * @param keptFrom  the first row of the block, counted from the first row of the table
     * @param keptCount how many rows the block has; 0 for none
     */
    public UndoableRemoveColumnsAction(int nCols,
                                       int startCol,
                                       int keptFrom,
                                       int keptCount,
                                       MetaInfoWriter metaInfoWriter) {
        this.nCols = nCols;
        this.startCol = startCol;
        this.keptFrom = keptFrom;
        this.keptCount = keptCount;
        this.metaInfoWriter = metaInfoWriter;
    }

    @Override
    protected boolean canPerformAction(IGridRegion gridRegion) {
        return !(startCol < 0 || startCol >= GridRegionUtils.width(gridRegion));
    }

    @Override
    protected int getNumberToRemove(IGridTable table) {
        return nCols;
    }

    @Override
    protected IUndoableGridTableAction performAction(int numberToRemove,
                                                     IGridRegion fullTableRegion,
                                                     IGridTable table) {
        return new UndoableCompositeAction(GridTool.outsideRows(fullTableRegion, keptFrom, keptCount)
                .stream()
                .map(part -> GridTool.removeColumns(numberToRemove, startCol, part, table.getGrid(), metaInfoWriter))
                .toList());
    }

    @Override
    protected GridRegionAction getGridRegionAction(IGridRegion gridRegion, int numberToRemove) {
        return new GridRegionAction(gridRegion, COLUMNS, REMOVE, ActionType.EXPAND, numberToRemove);
    }

}
