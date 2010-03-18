/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui;

import java.util.HashMap;

import org.openl.rules.table.CellKey;
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

    private HashMap<CellKey, FormattedCell> formattedCells = new HashMap<CellKey, FormattedCell>();

    /**
     * @param delegate
     */
    public FilteredGrid(IGrid delegate, IGridFilter[] formatFilters) {
        super(delegate);
        this.formatFilters = formatFilters;
    }

    /**
     * @param fcell
     * @param col
     * @param row
     */
    private void formatCell(FormattedCell fcell, int col, int row) {
        if (formatFilters != null) {
            for (int i = 0; i < formatFilters.length; i++) {
                if (formatFilters[i].getGridSelector() == null
                        || formatFilters[i].getGridSelector().selectCoords(col, row)) {
                    fcell = formatFilters[i].filterFormat(fcell);
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
        CellKey ckey = new CellKey(col, row);
        FormattedCell fcell = formattedCells.get(ckey);
        if (fcell == null) {
            fcell = new FormattedCell(delegate.getCell(col, row));
            formatCell(fcell, col, row);
            formattedCells.put(ckey, fcell);
        }
        return fcell;
    }

}
