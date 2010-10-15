/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

import org.openl.rules.table.ICell;
import org.openl.rules.table.IWritableGrid;
import org.openl.util.formatters.IFormatter;

/**
 * @author snshor
 *
 */
public class UndoableSetValueAction extends AUndoableCellAction {

    private Object prevValue;
    private String newValue;
    private IFormatter format;

    public UndoableSetValueAction(int col, int row, String value, IFormatter format) {
        super(col, row);
        this.newValue = value;
        this.format = format;
    }

    public void doAction(IWritableGrid wgrid) {
        ICell cell = wgrid.getCell(col, row);
        prevValue = cell.getObjectValue();

        Object result = newValue;
        if (format != null) {
            result = format.parse(newValue);
        }
        //cell.setObjectValue(result);
        wgrid.setCellValue(col, row, result);
    }

    public void undoAction(IWritableGrid wgrid) {
        //ICell cell = wgrid.getCell(col, row);
        //cell.setObjectValue(prevValue);
        wgrid.setCellValue(col, row, prevValue);
    }

}
