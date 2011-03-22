/**
 * Created May 2, 2007
 */
package org.openl.rules.search;

import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class DataTableSearchInfo implements ITableSearchInfo {

    private TableSyntaxNode tsn;

    private ITable table;

    public DataTableSearchInfo(TableSyntaxNode tsn) {
        this.tsn = tsn;
        DataOpenField df = (DataOpenField) tsn.getMember();
        table = df.getTable();
    }

    public String getColumnDisplay(int col) {
        return table.getColumnDisplay(col);
    }

    public String getColumnName(int col) {
        return table.getColumnName(col);
    }

    public IOpenClass getColumnType(int col) {
        return table.getColumnType(col);
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tsn;
    }

    public IGridTable getHeaderDisplayTable() {
        return table.getHeaderTable();
    }

    public int getNumberOfColumns() {
        return table.getNumberOfColumns();
    }

    public int getNumberOfRows() {
        return table.getNumberOfRows();
    }

    public IGridTable getRowTable(int row) {
        return table.getRowTable(row);
    }

    public Object getTableValue(int col, int row) {
        return table.getValue(col, row);
    }

}
