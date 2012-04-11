/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public class UndoableClearAction extends AUndoableCellAction {

    GridRegion toRestore;

    /**
     * @param col
     * @param row
     */
    public UndoableClearAction(int col, int row) {
        super(col, row);
    }

    void clearRegion(IWritableGrid wgrid) {
        IGridRegion rrTo = wgrid.getRegionStartingAt(col, row);

        if (rrTo == null) {
            return;
        }

        toRestore = new GridRegion(rrTo);
        wgrid.removeMergedRegion(toRestore);
    }

    @Override
    public void doDirectChange(IWritableGrid wgrid) {
        wgrid.clearCell(col, row);
        clearRegion(wgrid);
    }

    @Override
    public void restore(IWritableGrid wgrid, IUndoGrid undo) {
        if (toRestore != null) {
            wgrid.addMergedRegion(toRestore);
        }
        super.restore(wgrid, undo);
    }

}
