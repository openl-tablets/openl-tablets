package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGrid;
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

    // Read the initial cell -> move its merged region -> write the cell to the destination
    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        var rrFrom = grid.getRegionStartingAt(colFrom, rowFrom);

        var cell = grid.getCell(colFrom, rowFrom);

        var value = cell.getObjectValue();
        var formula = cell.getFormula();
        var style = cell.getStyle();
        String comment = null;
        String commentAuthor = null;
        if (cell.getComment() != null) {
            comment = cell.getComment().getText();
            commentAuthor = cell.getComment().getAuthor();
        }
        var metaInfo = metaInfoWriter.getMetaInfo(rowFrom, colFrom);

        if (rrFrom != null) {
            grid.removeMergedRegion(rrFrom);
            grid.addMergedRegion(new GridRegion(rrFrom.getTop() + getRow() - rowFrom,
                    rrFrom.getLeft() + getCol() - colFrom,
                    rrFrom.getBottom() + getRow() - rowFrom,
                    rrFrom.getRight() + getCol() - colFrom));
        }

        grid.setCellFormula(getCol(), getRow(), formula);
        grid.setCellValue(getCol(), getRow(), value);
        grid.setCellStyle(getCol(), getRow(), style);
        grid.setCellComment(getCol(), getRow(), comment, commentAuthor);
        metaInfoWriter.setMetaInfo(getRow(), getCol(), metaInfo);

        var newCell = grid.getCell(getCol(), getRow());
        if (cell.getType() == IGrid.CELL_TYPE_STRING && newCell.getType() == IGrid.CELL_TYPE_FORMULA) {
            grid.setCellStringValue(getCol(), getRow(), cell.getObjectValue().toString());
        }
    }
}
