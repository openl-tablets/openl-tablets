package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.formatters.AXlsFormatter;

public class TableValueFilter extends AGridFilter {

    static public interface Model {
        public Object getValue(int col, int row);
    }

    private Model model;

    private int startX, startY;

 //   private IGrid grid;

    public TableValueFilter(IGridTable t, Model m) {
        model = m;
        startX = t.getGridColumn(0, 0);
        startY = t.getGridRow(0, 0);
//       grid = t.getGrid();
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        Object v = getCellValue(cell.getColumn(), cell.getRow());

        if (v != null) {
            cell.setObjectValue(v);
            cell.setFormattedValue(String.valueOf(v));
        }
        return cell;
    }

    public Object getCellValue(int column, int row) {
        Object v = model.getValue(column - startX, row - startY);

        return v;
    }

    public AXlsFormatter getFormatter() {
        // TODO Auto-generated method stub
        return null;
    }

}
