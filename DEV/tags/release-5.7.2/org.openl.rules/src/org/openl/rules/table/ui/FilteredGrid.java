/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.GridDelegator;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.ui.filters.IGridFilter;

/**
 * @author snshor
 *
 */
public class FilteredGrid extends GridDelegator {

    private IGridFilter[] formatFilters;

    public FilteredGrid(IGrid delegate, IGridFilter[] formatFilters) {
        super(delegate);
        this.formatFilters = formatFilters.clone();
    }

    private void formatCell(FormattedCell fcell, int col, int row) {
        if (formatFilters != null) {
            for (int i = 0; i < formatFilters.length; i++) {
                if (formatFilters[i].getGridSelector() == null
                        || formatFilters[i].getGridSelector().selectCoords(col, row)) {
                    try {
                        // Side effect of method call is setting object value of the cell
                        formatFilters[i].filterFormat(fcell);
                    }
                    catch (IllegalArgumentException e){
                        //Ignore if failed to format
                    }
                }
            }
        }
    }

    @Override
    public ICell getCell(int column, int row) {
        if (isEmpty(column, row)) {
            super.getCell(column, row);
        }

        return getFormattedCell(column, row);
    }

    public synchronized FormattedCell getFormattedCell(int col, int row) {
        FormattedCell cellToFormat = new FormattedCell(delegate.getCell(col, row));

        formatCell(cellToFormat, col, row);

        return cellToFormat;
    }

}
