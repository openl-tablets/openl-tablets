package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ui.IGridSelector;

public interface IGridFilter {

    /**
     * Changes styles, formated value, object value and other display related attributes of the cell.
     *
     * @param cell The cell to change formats in.
     * @return The cell which was input parameter.
     */
    FormattedCell filterFormat(FormattedCell cell);

    IGridSelector getGridSelector();
}