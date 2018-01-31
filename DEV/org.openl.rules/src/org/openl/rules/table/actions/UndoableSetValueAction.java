package org.openl.rules.table.actions;

import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.util.StringUtils;

/**
 * @author snshor
 *
 */
public class UndoableSetValueAction extends AUndoableCellAction {

    private Object newValue;

    public UndoableSetValueAction(int col, int row, Object value) {
        super(col, row);
        this.newValue = value;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        ICell cell = grid.getCell(getCol(), getRow());
        setPrevValue(cell.getObjectValue());
        setPrevFormula(cell.getFormula());
        setPrevMetaInfo(cell.getMetaInfo());

        grid.setCellValue(getCol(), getRow(), newValue);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        if (StringUtils.isNotBlank(getPrevFormula())) {
            grid.setCellFormula(getCol(), getRow(), getPrevFormula());
        } else {
            grid.setCellValue(getCol(), getRow(), getPrevValue());
        }
        grid.getCell(getCol(), getRow()).setMetaInfo(getPrevMetaInfo());
    }

}
