/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.util.formatters.IFormatter;

/**
 * @author snshor
 *
 */
public class UndoableSetValueAction extends AUndoableCellAction {

    private String newValue;
    private IFormatter format;

    public UndoableSetValueAction(int col, int row, String value, IFormatter format) {
        super(col, row);
        this.newValue = value;
        this.format = format;
    }

    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        ICell cell = grid.getCell(getCol(), getRow());
        setPrevValue(cell.getObjectValue());
        setPrevFormula(cell.getFormula());

        Object result = newValue;
        if (format != null) {
            result = format.parse(newValue);
        }

        grid.setCellValue(getCol(), getRow(), result);
    }

    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        if (StringUtils.isNotBlank(getPrevFormula())) {
            grid.setCellFormula(getCol(), getRow(), getPrevFormula());
        } else {
            grid.setCellValue(getCol(), getRow(), getPrevValue());
        }
    }

}
