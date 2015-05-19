package org.openl.rules.ui;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.filters.AGridFilter;

public class TableValueFilter extends AGridFilter {

    public interface Model {
        Object getValue(int col, int row);
    }

    private Model model;

    private int startX, startY;

    public TableValueFilter(IGridTable t, Model m) {
        model = m;
        startX = t.getGridColumn(0, 0);
        startY = t.getGridRow(0, 0);
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        Object v = model.getValue(cell.getColumn() - startX, cell.getRow() - startY);

        if (v != null) {
            cell.setObjectValue(v);
            cell.setFormattedValue(String.valueOf(v));
        }
        return cell;
    }

}
