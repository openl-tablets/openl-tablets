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

        int width = table.getWidth();
        int height = table.getHeight();

        start = new GridPosition(table.getGridColumn(0, 0), table.getGridRow(0, 0), table.getGrid());
        end = new GridPosition(table.getGridColumn(width - 1, height - 1),
            table.getGridRow(width - 1, height - 1),
            table.getGrid());
    }

    @Override
    public IPosition getEnd() {
        return end;
    }

    @Override
    public IPosition getStart() {
        return start;
    }

    @Override
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
