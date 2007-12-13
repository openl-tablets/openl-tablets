package org.openl.rules.ui;

import org.openl.rules.table.IGridTable;


/**
 * @author Stanislav Shor
 *
 */
public interface ICellSelector {
    public boolean select(IGridTable table, int col, int row);
}
