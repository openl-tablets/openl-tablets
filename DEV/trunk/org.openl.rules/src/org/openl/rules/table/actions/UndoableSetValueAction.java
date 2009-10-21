/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.IGridFilter;

/**
 * @author snshor
 *
 */
public class UndoableSetValueAction extends AUndoableCellAction {

    String value;
    IGridFilter filter;

    /**
     * @param col
     * @param row
     */
    public UndoableSetValueAction(int col, int row, String value, IGridFilter filter) {
        super(col, row);
        this.value = value;
        this.filter = filter;
    }

    @Override
    public void doDirectChange(IWritableGrid wgrid) {
        Object result = value;
        if (filter != null) {
            result = filter.parse(value);
        }
        wgrid.setCellValue(col, row, result);
    }

}
