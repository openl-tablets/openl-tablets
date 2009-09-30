/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui;

import java.util.HashMap;

import org.openl.rules.table.CellKey;
import org.openl.rules.table.ErrorCell;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.GridDelegator;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;

/**
 * @author snshor
 *
 */
public class FilteredGrid extends GridDelegator {

    IGridFilter[] formatFilters;

    HashMap<CellKey, ICell> formattedCells = new HashMap<CellKey, ICell>();

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
        ICell result = null;
        if (isEmpty(column, row)) {
            super.getCell(column, row);
        }
        try {
            result = getFormattedCell(column, row);
        } catch (RuntimeException e) {
            result = getErrorCell(column, row);
        }
        return result;
    }

    public synchronized ErrorCell getErrorCell(int column, int row) {
        CellKey ckey = new CellKey(column, row);
        ErrorCell errCell = new ErrorCell(delegate.getCell(column, row));
        formattedCells.put(ckey, errCell);
        return null;
    }

    public synchronized FormattedCell getFormattedCell(int col, int row) {
        CellKey ckey = new CellKey(col, row);
        FormattedCell fcell = (FormattedCell)formattedCells.get(ckey);
        if (fcell == null) {
            fcell = new FormattedCell(delegate.getCell(col, row));
            formatCell(fcell, col, row);
            formattedCells.put(ckey, fcell);
        }
        return fcell;
    }

}
