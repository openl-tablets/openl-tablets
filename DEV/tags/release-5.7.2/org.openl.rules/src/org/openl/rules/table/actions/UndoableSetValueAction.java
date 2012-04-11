/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table.actions;

import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.xls.formatters.AXlsFormatter;

/**
 * @author snshor
 *
 */
public class UndoableSetValueAction extends AUndoableCellAction {

    private String value;
    private AXlsFormatter format;

    /**
     * @param col
     * @param row
     */
    public UndoableSetValueAction(int col, int row, String value, AXlsFormatter format) {
        super(col, row);
        this.value = value;
        this.format = format;
    }

    @Override
    public void doDirectChange(IWritableGrid wgrid) {
        Object result = value;
        if (format != null) {
            result = format.parse(value);
        }
        wgrid.setCellValue(col, row, result);
    }

}
