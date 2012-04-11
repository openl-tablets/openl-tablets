/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table;

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

        // FormattedCell fcell = wgrid.getFormattedCell(col, row);
        // if (fcell != null)
        // {
        // IGridFilter filter = fcell.getFilter();
        if (filter != null) {
            Object res = filter.parse(value);
            wgrid.setCellValue(col, row, res);
            return;
        }

        // }

        wgrid.setCellStringValue(col, row, value);
    }

}
