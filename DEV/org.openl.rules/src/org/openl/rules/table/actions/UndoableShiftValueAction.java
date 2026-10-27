package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;

/**
 * Shift cell with merged region.
 *
 * @author PUdalau
 */
public class UndoableShiftValueAction extends AUndoableCellAction {

    private final int colFrom;
    private final int rowFrom;

    public UndoableShiftValueAction(int colFrom, int rowFrom, int colTo, int rowTo, MetaInfoWriter metaInfoWriter) {
        super(colTo, rowTo, metaInfoWriter);
        this.colFrom = colFrom;
        this.rowFrom = rowFrom;
    }

    /**
     * Moves the cell with its merged region to the destination.
     *
     * <p>The value moves as it is stored, so the moved cell saves as it did before.
     */
    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        var rrFrom = grid.getRegionStartingAt(colFrom, rowFrom);

        var cell = grid.getCell(colFrom, rowFrom);

        var style = cell.getStyle();
        String comment = null;
        String commentAuthor = null;
        if (cell.getComment() != null) {
            comment = cell.getComment().getText();
            commentAuthor = cell.getComment().getAuthor();
        }
        var metaInfo = metaInfoWriter.getMetaInfo(rowFrom, colFrom);

        // The value moves before the merge: a merge empties the cells it covers, and they can include this one.
        grid.copyCellValue(colFrom, rowFrom, getCol(), getRow());
        if (rrFrom != null) {
            grid.moveMergedRegion(rrFrom, new GridRegion(rrFrom.getTop() + getRow() - rowFrom,
                    rrFrom.getLeft() + getCol() - colFrom,
                    rrFrom.getBottom() + getRow() - rowFrom,
                    rrFrom.getRight() + getCol() - colFrom));
        }

        if (style != null) {
            grid.setCellStyle(getCol(), getRow(), style);
        }
        grid.setCellComment(getCol(), getRow(), comment, commentAuthor);
        metaInfoWriter.setMetaInfo(getRow(), getCol(), metaInfo);
    }
}
