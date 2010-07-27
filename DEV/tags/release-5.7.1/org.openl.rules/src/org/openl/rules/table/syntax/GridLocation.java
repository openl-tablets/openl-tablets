/*
 * Created on Oct 7, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.syntax;

import org.openl.rules.table.IGridTable;
import org.openl.util.text.ILocation;
import org.openl.util.text.IPosition;

/**
 * @author snshor
 * 
 */
public class GridLocation implements ILocation {

    private GridPosition start;
    private GridPosition end;

    public GridLocation(IGridTable table) {

        int width = table.getGridWidth();
        int height = table.getGridHeight();

        start = new GridPosition(table.getGridColumn(0, 0), table.getGridRow(0, 0), table.getGrid());
        end = new GridPosition(table.getGridColumn(width - 1, height - 1),
            table.getGridRow(width - 1, height - 1),
            table.getGrid());
    }

    public GridLocation(IGridTable table, int x1, int y1) {
        start = new GridPosition(table.getGridColumn(x1, y1), table.getGridRow(x1, y1), table.getGrid());
        end = null;
    }

    public GridLocation(IGridTable table, int x1, int y1, int x2, int y2) {
        start = new GridPosition(table.getGridColumn(x1, y1), table.getGridRow(x1, y1), table.getGrid());
        end = new GridPosition(table.getGridColumn(x2, y2), table.getGridRow(x2, y2), table.getGrid());
    }

    public IPosition getEnd() {
        return end;
    }

    public IPosition getStart() {
        return start;
    }

    public boolean isTextLocation() {
        return false;
    }

    @Override
    public String toString() {

        if (end == null) {
            return XlsURLConstants.CELL + "=" + start;
        }

        return XlsURLConstants.RANGE + "=" + start + ":" + end;
    }

}
